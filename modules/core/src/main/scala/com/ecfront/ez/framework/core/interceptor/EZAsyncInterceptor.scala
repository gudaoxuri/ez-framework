package com.ecfront.ez.framework.core.interceptor

import com.ecfront.common.{AsyncResp, Resp}
import com.typesafe.scalalogging.slf4j.LazyLogging

import scala.concurrent.{Future, Promise}

/**
  * EZ异步拦截器定义，用于拦截器栈处理
  *
  * @tparam E 拦截参数类型
  */
trait EZAsyncInterceptor[E] extends LazyLogging {

  // 拦截器类型
  val category: String

  // 拦截器名称
  lazy val name = this.getClass.getSimpleName

  // 入栈方法
  def before(obj: E, context: collection.mutable.Map[String, Any], p: AsyncResp[E]): Unit

  // 出栈方法
  def after(obj: E, context: collection.mutable.Map[String, Any], p: AsyncResp[E]): Unit

  private[core] def before(obj: E, context: collection.mutable.Map[String, Any]): Future[Resp[E]] = {
    val p = Promise[Resp[E]]()
    logger.trace(s"EZ Interceptor [$category - $name - before ] execute.")
    before(obj, context, AsyncResp(p))
    p.future
  }

  private[core] def after(obj: E, context: collection.mutable.Map[String, Any]): Future[Resp[E]] = {
    val p = Promise[Resp[E]]()
    logger.trace(s"EZ Interceptor [$category - $name - after ] execute.")
    after(obj, context, AsyncResp(p))
    p.future
  }

}
