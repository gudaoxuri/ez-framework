package com.ecfront.ez.framework.service.message

import com.ecfront.common.Resp
import com.ecfront.ez.framework.core.EZServiceAdapter
import com.ecfront.ez.framework.core.rpc.AutoBuildingProcessor
import com.ecfront.ez.framework.service.message.entity.{EZ_Message, EZ_Message_Log}
import io.vertx.core.json.JsonObject

import scala.collection.JavaConversions._
import scala.collection.mutable

object ServiceAdapter extends EZServiceAdapter[JsonObject] {

  override def init(parameter: JsonObject): Resp[String] = {
    if (parameter.containsKey("customTables")) {
      parameter.getJsonObject("customTables").foreach {
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

  override def destroy(parameter: JsonObject): Resp[String] = {
    Resp.success("")
  }

  override lazy val dependents: mutable.Set[String] =
    mutable.Set(com.ecfront.ez.framework.service.jdbc.ServiceAdapter.serviceName)

  override var serviceName: String = "message"

}


