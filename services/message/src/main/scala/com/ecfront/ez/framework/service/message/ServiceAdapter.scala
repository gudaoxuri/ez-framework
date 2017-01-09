package com.ecfront.ez.framework.service.message

import com.ecfront.common.Resp
import com.ecfront.ez.framework.core.EZServiceAdapter
import com.ecfront.ez.framework.core.rpc.AutoBuildingProcessor
import com.fasterxml.jackson.databind.JsonNode

import scala.collection.mutable

object ServiceAdapter extends EZServiceAdapter[JsonNode] {

  override def init(parameter: JsonNode): Resp[String] = {
    Resp.success("")
  }

  override def initPost(): Unit = {
    AutoBuildingProcessor.autoBuilding("com.ecfront.ez.framework.service.message")
    super.initPost()
  }

  override def destroy(parameter: JsonNode): Resp[String] = {
    Resp.success("")
  }

  override lazy val dependents: mutable.Set[String] =
    mutable.Set(com.ecfront.ez.framework.service.jdbc.ServiceAdapter.serviceName)

  override var serviceName: String = "message"

}


