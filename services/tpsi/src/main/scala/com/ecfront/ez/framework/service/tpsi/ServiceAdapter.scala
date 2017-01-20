package com.ecfront.ez.framework.service.tpsi

import com.ecfront.common.{JsonHelper, Resp}
import com.ecfront.ez.framework.core.rpc.AutoBuildingProcessor
import com.ecfront.ez.framework.core.{EZ, EZServiceAdapter}
import com.fasterxml.jackson.databind.JsonNode

object ServiceAdapter extends EZServiceAdapter[JsonNode] {

  private[tpsi] var config: TPSIConfig = _

  val TPSI_ADD_FLAG = "/ez/tpsi/add/"


  override def init(parameter: JsonNode): Resp[String] = {
    config = JsonHelper.toObject[TPSIConfig](parameter)
    if (config.code == null || config.code.trim.isEmpty) {
      config.code = EZ.Info.module
    }
    Resp.success(s"[TPSI]start success, mock = ${config.isMock}")
  }

  override def initPost(): Unit = {
    AutoBuildingProcessor.autoBuilding("com.ecfront.ez.framework.service.tpsi")
    super.initPost()
  }

  override def destroy(parameter: JsonNode): Resp[String] = {
    Resp.success("")
  }

  override var serviceName: String = "tpsi"

}


