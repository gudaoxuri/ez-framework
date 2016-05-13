package com.ecfront.ez.framework.service.scheduler

import com.ecfront.common.Resp
import com.ecfront.ez.framework.core.{EZContext, EZServiceAdapter}
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
            case "scheduler" => EZ_Scheduler.customTableName(item.getValue.asInstanceOf[String])
            case "scheduler_Log" => EZ_Scheduler_Log.customTableName(item.getValue.asInstanceOf[String])
          }
      }
    }
    SchedulerProcessor.init(EZContext.module)
    Resp.success("")
  }

  override def destroy(parameter: JsonObject): Resp[String] = {
    SchedulerProcessor.shutdown()
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


