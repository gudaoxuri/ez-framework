package com.asto.ez.framework.interceptor

import com.ecfront.common.{AsyncResp, Resp}
import com.typesafe.scalalogging.slf4j.LazyLogging

import scala.concurrent.{Future, Promise}

trait EZInterceptor[E] extends LazyLogging {

  val category: String

  lazy val name = this.getClass.getSimpleName

  def before(obj: E, context: collection.mutable.Map[String, Any]): Future[Resp[E]] = {
    val p = Promise[Resp[E]]()
    logger.trace(s"Interceptor [$category - $name - before ] execute.")
    before(obj, context, AsyncResp(p))
    p.future
  }

  def after(obj: E, context: collection.mutable.Map[String, Any]): Future[Resp[E]] = {
    val p = Promise[Resp[E]]()
    logger.trace(s"Interceptor [$category - $name - after ] execute.")
    after(obj, context, AsyncResp(p))
    p.future
  }

  def before(obj: E, context: collection.mutable.Map[String, Any], p: AsyncResp[E])

  def after(obj: E, context: collection.mutable.Map[String, Any], p: AsyncResp[E])


}
