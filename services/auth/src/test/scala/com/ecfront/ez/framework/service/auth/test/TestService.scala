package com.ecfront.ez.framework.service.auth.test

import com.ecfront.common.Resp
import com.ecfront.ez.framework.core.logger.Logging
import com.ecfront.ez.framework.core.rpc.{GET, RPC}

@RPC("/public/test/")
object TestService extends Logging {

  @GET("wait/")
  def waiting(parameter: Map[String, String]): Resp[String] = {
    logger.info(s"[${parameter("id")}]wait...")
    Thread.sleep(10000)
    Resp.success("")
  }

  @GET("immediately/")
  def immediately(parameter: Map[String, String]): Resp[String] = {
    logger.info("immediately...")
    Resp.success(System.nanoTime() + "")
  }

}