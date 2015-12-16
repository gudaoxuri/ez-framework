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

  def process[E](category: String, obj: E, successFun: => (E, Map[String, Any]) => Future[Resp[Void]], errorFun: => (Resp[E], Map[String, Any]) => Future[Resp[Void]] = null) = {
    val beforeP = Promise[Resp[E]]()
    val context = collection.mutable.Map[String, Any]()
    doProcess[E](obj, context, interceptor_container(category).asInstanceOf[List[EZInterceptor[E]]], isBefore = true, AsyncResp(beforeP))
    beforeP.future.onSuccess {
      case beforeResp =>
        if (beforeResp) {
          successFun(beforeResp.body, context.toMap).onSuccess {
            case successResp =>
              if (successResp) {
                val afterP = Promise[Resp[E]]()
                doProcess[E](obj, context, interceptor_container(category).asInstanceOf[List[EZInterceptor[E]]].reverse, isBefore = false, AsyncResp(afterP))
                afterP.future.onSuccess {
                  case afterResp =>
                    if (!afterResp) {
                      logger.warn(s"Execute interceptor error : ${afterResp.message}")
                    }
                }
              } else {
                logger.warn(s"Execute interceptor error : ${successResp.message}")
              }
          }
        } else {
          logger.warn(s"Execute interceptor error : ${beforeResp.message}")
          if (errorFun != null) {
            errorFun(beforeResp, context.toMap)
          }
        }
    }
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
