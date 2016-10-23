package com.ecfront.ez.framework.service.tpsi

import java.util.concurrent.Executors

import com.ecfront.common.{JsonHelper, Resp}
import com.ecfront.ez.framework.core.{EZ, EZServiceAdapter}
import com.fasterxml.jackson.databind.JsonNode

object ServiceAdapter extends EZServiceAdapter[JsonNode] {

  val execute = Executors.newCachedThreadPool()
  private[tpsi] var config:TPSIConfig = _

  override def init(parameter: JsonNode): Resp[String] = {
    config = JsonHelper.toObject[TPSIConfig](parameter)
    if(config.code==null||config.code.trim.isEmpty){
      config.code=EZ.Info.module
    }
    Resp.success("")
  }

  override def destroy(parameter: JsonNode): Resp[String] = {
    Resp.success("")
  }

  override var serviceName: String = "tpsi"

}


