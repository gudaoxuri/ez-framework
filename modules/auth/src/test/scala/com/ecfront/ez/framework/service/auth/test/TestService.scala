package com.ecfront.ez.framework.service.auth.test

import com.ecfront.common.Resp
import com.ecfront.ez.framework.service.auth.EZAuthContext
import com.ecfront.ez.framework.service.rpc.foundation.{GET, RPC}
import com.ecfront.ez.framework.service.rpc.http.HTTP
import com.typesafe.scalalogging.slf4j.LazyLogging

@RPC("/public/test/")
@HTTP
object TestService extends LazyLogging{

  @GET("wait/")
  def waiting(parameter: Map[String, String], context: EZAuthContext): Resp[String] = {
    logger.info("wait...")
    Thread.sleep(1000000)
    Resp.success("")
  }

  @GET("immediately/")
  def immediately(parameter: Map[String, String], context: EZAuthContext): Resp[String] = {
    logger.info("immediately...")
    Resp.success(System.nanoTime() + "")
  }

}