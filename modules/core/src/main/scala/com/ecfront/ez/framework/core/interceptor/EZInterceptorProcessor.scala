package com.ecfront.ez.framework.core.interceptor

import com.ecfront.common.Resp
import com.typesafe.scalalogging.slf4j.LazyLogging

/**
  * 拦截器栈处理类
  */
object EZInterceptorProcessor extends LazyLogging {

  private val interceptor_container = collection.mutable.Map[String, List[EZInterceptor[_]]]()

  /**
    * 注册拦截器栈
    *
    * @param category    拦截类型
    * @param interceptor 拦截器
    */
  def register(category: String, interceptor: EZInterceptor[_]): Unit = {
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
  def register(category: String, interceptors: List[EZInterceptor[_]]): Unit = {
    interceptor_container += category -> interceptors
  }

  /**
    * 拦截器栈处理方法
    *
    * @param category   拦截类型
    * @param parameter        初始入栈参数
    * @param executeFun 入栈成功后执行的方法
    * @tparam E 初始入栈参数的类型
    * @return 最终输出结果
    */
  def process[E](category: String, parameter: E, executeFun: => (E, Map[String, Any]) => Resp[E] = null): Resp[(E, Map[String, Any])] = {
    val context = collection.mutable.Map[String, Any]()
    if (interceptor_container.contains(category)) {
      val beforeR = doProcess[E](parameter, context, interceptor_container(category).asInstanceOf[List[EZInterceptor[E]]], isBefore = true)
      if (beforeR) {
        if (executeFun != null) {
          val execR = executeFun(beforeR.body, context.toMap)
          if (execR) {
            doProcess[E](execR.body, context, interceptor_container(category).asInstanceOf[List[EZInterceptor[E]]].reverse, isBefore = false)
          } else {
            logger.warn(s"EZ Interceptor [$category] execute error : ${execR.code} - ${execR.message} ")
            execR
          }
        } else {
          doProcess[E](beforeR.body, context, interceptor_container(category).asInstanceOf[List[EZInterceptor[E]]].reverse, isBefore = false)
        }
      } else {
        beforeR
      }
    } else {
      logger.warn(s"EZ Interceptor [$category] not found ")
      Resp.notFound(s"EZ Interceptor [$category] not found ")
    }
  }

  private def doProcess[E](obj: E, context: collection.mutable.Map[String, Any], interceptors: List[EZInterceptor[E]], isBefore: Boolean): Resp[E] = {
    val execR = if (isBefore) {
      interceptors.head.before(obj, context)
    } else {
      interceptors.head.after(obj, context)
    }
    if (execR) {
      if (interceptors.tail.nonEmpty) {
        doProcess(execR.body, context, interceptors.tail, isBefore)
      } else {
        execR
      }
    } else {
      logger.warn(s"EZ Interceptor [${interceptors.head.category} - ${interceptors.head.name} : $isBefore] error : ${execR.code} - ${execR.message} ")
      execR
    }
  }

}
