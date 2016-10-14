package com.ecfront.ez.framework.core.interceptor

import com.ecfront.common.Resp
import com.typesafe.scalalogging.slf4j.LazyLogging

/**
  * EZ拦截器定义，用于拦截器栈处理
  *
  * @tparam E 拦截参数类型
  */
trait EZInterceptor[E] extends Serializable with LazyLogging {

  // 拦截器类型
  val category: String

  // 拦截器名称
  lazy val name = this.getClass.getSimpleName

  // 入栈方法
  def before(obj: E, context: collection.mutable.Map[String, Any]): Resp[E]

  // 出栈方法
  def after(obj: E, context: collection.mutable.Map[String, Any]): Resp[E]


}
