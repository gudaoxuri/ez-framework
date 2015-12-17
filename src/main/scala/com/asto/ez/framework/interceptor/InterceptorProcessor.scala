package com.asto.ez.framework.interceptor

import com.ecfront.common.{AsyncResp, Resp}
import com.typesafe.scalalogging.slf4j.LazyLogging

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}

object InterceptorProcessor extends LazyLogging {

  private val interceptor_container = collection.mutable.Map[String, List[EZInterceptor[_]]]()

  def register[E](category: String, interceptors: List[EZInterceptor[E]]): Unit = {
    interceptor_container += category -> interceptors
  }

  def process[E](category: String, obj: E, executeFun: => (E, Map[String, Any]) => Future[Resp[E]] = null): Future[Resp[(E, Map[String, Any])]] = {
    val finishP = Promise[Resp[(E, Map[String, Any])]]()
    val context = collection.mutable.Map[String, Any]()
    if (interceptor_container.contains(category)) {
      val beforeP = Promise[Resp[E]]()
      doProcess[E](obj, context, interceptor_container(category).asInstanceOf[List[EZInterceptor[E]]], isBefore = true, AsyncResp(beforeP))
      beforeP.future.onSuccess {
        case beforeResp =>
          if (beforeResp) {
            if (executeFun != null) {
              executeFun(beforeResp.body, context.toMap).onSuccess {
                case executeResp =>
                  if (executeResp) {
                    val afterP = Promise[Resp[E]]()
                    doProcess[E](executeResp.body, context, interceptor_container(category).asInstanceOf[List[EZInterceptor[E]]].reverse, isBefore = false, AsyncResp(afterP))
                    afterP.future.onSuccess {
                      case afterResp =>
                        if (!afterResp) {
                          logger.warn(s"Execute after interceptor error : ${afterResp.message}")
                          finishP.success(afterResp)
                        } else {
                          finishP.success(Resp.success((afterResp.body, context.toMap)))
                        }
                    }
                  } else {
                    logger.warn(s"Execute interceptor error : ${executeResp.message}")
                    finishP.success(executeResp)
                  }
              }
            } else {
              val afterP = Promise[Resp[E]]()
              doProcess[E](beforeResp.body, context, interceptor_container(category).asInstanceOf[List[EZInterceptor[E]]].reverse, isBefore = false, AsyncResp(afterP))
              afterP.future.onSuccess {
                case afterResp =>
                  if (!afterResp) {
                    logger.warn(s"Execute after interceptor error : ${afterResp.message}")
                    finishP.success(afterResp)
                  } else {
                    finishP.success(Resp.success((afterResp.body, context.toMap)))
                  }
              }
            }
          } else {
            logger.warn(s"Execute before interceptor error : ${beforeResp.message}")
            finishP.success(beforeResp)
          }
      }
    } else {
      finishP.success(Resp.success((obj, context.toMap)))
    }
    finishP.future
  }

  private def doProcess[E](obj: E, context: collection.mutable.Map[String, Any], interceptors: List[EZInterceptor[E]], isBefore: Boolean, interP: AsyncResp[E]): Unit = {
    val execFuture = if (isBefore) {
      interceptors.head.before(obj, context)
    } else {
      interceptors.head.after(obj, context)
    }
    execFuture.onSuccess {
      case objResp =>
        if (objResp) {
          if (interceptors.tail.nonEmpty) {
            doProcess(objResp.body, context, interceptors.tail, isBefore, interP)
          } else {
            interP.success(objResp.body)
          }
        } else {
          logger.warn(s"Interceptor [${interceptors.head.category} - ${interceptors.head.name} : $isBefore] error : ${objResp.code} - ${objResp.message} ")
          interP.resp(objResp)
        }
    }
  }


}
