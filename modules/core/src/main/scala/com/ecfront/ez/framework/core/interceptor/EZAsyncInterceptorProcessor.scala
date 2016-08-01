package com.ecfront.ez.framework.core.interceptor

import com.ecfront.common.{AsyncResp, Resp}
import com.typesafe.scalalogging.slf4j.LazyLogging

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}

object EZAsyncInterceptorProcessor extends LazyLogging {

  private val interceptor_container = collection.mutable.Map[String, List[EZAsyncInterceptor[_]]]()

  /**
    * 注册拦截器栈
    *
    * @param category    拦截类型
    * @param interceptor 拦截器
    */
  def register(category: String, interceptor: EZAsyncInterceptor[_]): Unit = {
    if (interceptor_container.contains(category)) {
      interceptor_container += category -> (interceptor_container(category) ++ List(interceptor))
    } else {
      interceptor_container += category -> List(interceptor)
    }
  }

  /**
    * 注册拦截器栈
    *
    * @param category     拦截类型
    * @param interceptors 拦截器列表
    */
  def register(category: String, interceptors: List[EZAsyncInterceptor[_]]): Unit = {
    interceptor_container += category -> interceptors
  }

  /**
    * 拦截器栈处理方法
    *
    * @param category   拦截类型
    * @param parameter  初始入栈参数
    * @param executeFun 入栈成功后执行的方法
    * @tparam E 初始入栈参数的类型
    * @return 最终输出结果
    */
  def process[E](category: String, parameter: E, executeFun: => (E, Map[String, Any]) => Future[Resp[E]] = null): Future[Resp[(E, Map[String, Any])]] = {
    val finishP = Promise[Resp[(E, Map[String, Any])]]()
    val context = collection.mutable.Map[String, Any]()
    if (interceptor_container.contains(category)) {
      val beforeP = Promise[Resp[E]]()
      doProcess[E](parameter, context, interceptor_container(category).asInstanceOf[List[EZAsyncInterceptor[E]]], isBefore = true, AsyncResp(beforeP))
      beforeP.future.onSuccess {
        case beforeResp =>
          if (beforeResp) {
            if (executeFun != null) {
              executeFun(beforeResp.body, context.toMap).onSuccess {
                case executeResp =>
                  if (executeResp) {
                    val afterP = Promise[Resp[E]]()
                    doProcess[E](executeResp.body, context,
                      interceptor_container(category).asInstanceOf[List[EZAsyncInterceptor[E]]].reverse, isBefore = false, AsyncResp(afterP))
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
              doProcess[E](beforeResp.body, context,
                interceptor_container(category).asInstanceOf[List[EZAsyncInterceptor[E]]].reverse, isBefore = false, AsyncResp(afterP))
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
      if (executeFun != null) {
        executeFun(parameter, context.toMap).onSuccess {
          case executeResp =>
            if (executeResp) {
              finishP.success(Resp.success(executeResp.body,context.toMap))
            } else {
              logger.warn(s"Execute interceptor error : ${executeResp.message}")
              finishP.success(executeResp)
            }
        }
      } else {
        finishP.success(Resp.success((parameter, context.toMap)))
      }
    }
    finishP.future
  }

  private def doProcess[E](
                            obj: E, context: collection.mutable.Map[String, Any],
                            interceptors: List[EZAsyncInterceptor[E]], isBefore: Boolean, interP: AsyncResp[E]): Unit = {
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
