package com.ecfront.ez.framework.service.message

import com.ecfront.common.Resp
import com.ecfront.ez.framework.core.EZServiceAdapter
import com.ecfront.ez.framework.core.rpc.AutoBuildingProcessor
import com.ecfront.ez.framework.service.message.entity.{EZ_Message, EZ_Message_Log}
import com.fasterxml.jackson.databind.JsonNode

import scala.collection.JavaConversions._
import scala.collection.mutable

object ServiceAdapter extends EZServiceAdapter[JsonNode] {

  override def init(parameter: JsonNode): Resp[String] = {
    if (parameter.has("customTables")) {
      parameter.get("customTables").fields().foreach {
        item =>
          item.getKey match {
            case "message" => EZ_Message.customTableName(item.getValue.asInstanceOf[String])
            case "message_log" => EZ_Message_Log.customTableName(item.getValue.asInstanceOf[String])
          }
      }
    }
    AutoBuildingProcessor.autoBuilding("com.ecfront.ez.framework.service.message")
    Resp.success("")
  }

  override def destroy(parameter: JsonNode): Resp[String] = {
    Resp.success("")
  }

  override lazy val dependents: mutable.Set[String] =
    mutable.Set(com.ecfront.ez.framework.service.jdbc.ServiceAdapter.serviceName)

  override var serviceName: String = "message"

}


