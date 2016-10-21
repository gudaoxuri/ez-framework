package com.ecfront.ez.framework.service.tpsi

import java.util.concurrent.Executors

import com.ecfront.common.{JsonHelper, Resp}
import com.ecfront.ez.framework.core.EZServiceAdapter
import com.fasterxml.jackson.databind.JsonNode

object ServiceAdapter extends EZServiceAdapter[JsonNode] {

  val execute = Executors.newCachedThreadPool()

  override def init(parameter: JsonNode): Resp[String] = {
    val config = JsonHelper.toObject[TPSIServiceConfig](parameter)
    try {
      val service = runtimeMirror.reflectModule(
        runtimeMirror.staticModule(s"${config.servicePath}$$")
      ).instance.asInstanceOf[TPSIService]
    } catch {
      case e: Throwable =>
        logger.error("start services error.", e)
        Resp.serverError(e.getMessage)
    }
    Resp.success("")
  }

  override def destroy(parameter: JsonNode): Resp[String] = {
    Resp.success("")
  }

  override var serviceName: String = "tpsi"

}


