package com.ecfront.ez.framework.service.message

import com.ecfront.common.Resp
import com.ecfront.ez.framework.core.EZServiceAdapter
import com.ecfront.ez.framework.service.message.entity.{EZ_Message, EZ_Message_Log}
import com.ecfront.ez.framework.service.rpc.foundation.AutoBuildingProcessor
import com.ecfront.ez.framework.service.rpc.http.HTTP
import io.vertx.core.json.JsonObject

import scala.collection.JavaConversions._

object ServiceAdapter extends EZServiceAdapter[JsonObject] {

  var mongoStorage: Boolean = false

  override def init(parameter: JsonObject): Resp[String] = {
    mongoStorage = parameter.getString("storage", "mongo") == "mongo"
    if (parameter.containsKey("customTables")) {
      parameter.getJsonObject("customTables").foreach {
        item =>
          item.getKey match {
            case "message" => EZ_Message.customTableName(item.getValue.asInstanceOf[String])
            case "message_log" => EZ_Message_Log.customTableName(item.getValue.asInstanceOf[String])
          }
      }
    }
    AutoBuildingProcessor.autoBuilding[HTTP]("com.ecfront.ez.framework.service.message", classOf[HTTP])
    Resp.success("")
  }

  override def destroy(parameter: JsonObject): Resp[String] = {
    Resp.success("")
  }

  override def getDynamicDependents(parameter: JsonObject): Set[String] = {
    if (parameter.getString("storage") == "mongo") {
      Set(com.ecfront.ez.framework.service.storage.mongo.ServiceAdapter.serviceName)
    } else {
      Set(com.ecfront.ez.framework.service.storage.jdbc.ServiceAdapter.serviceName)
    }
  }

  override var serviceName: String = "scheduler"

}


