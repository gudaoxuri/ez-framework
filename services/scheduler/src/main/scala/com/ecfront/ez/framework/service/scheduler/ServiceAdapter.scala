package com.ecfront.ez.framework.service.scheduler

import com.ecfront.common.Resp
import com.ecfront.ez.framework.core.{EZ, EZServiceAdapter}
import io.vertx.core.json.JsonObject

import scala.collection.JavaConversions._
import scala.collection.mutable

object ServiceAdapter extends EZServiceAdapter[JsonObject] {

  override def init(parameter: JsonObject): Resp[String] = {
    if (parameter.containsKey("customTables")) {
      parameter.getJsonObject("customTables").foreach {
        item =>
          item.getKey match {
            case "scheduler" => EZ_Scheduler.customTableName(item.getValue.asInstanceOf[String])
            case "scheduler_log" => EZ_Scheduler_Log.customTableName(item.getValue.asInstanceOf[String])
          }
      }
    }
    SchedulerProcessor.init(EZ.Info.module)
    Resp.success("")
  }

  override def destroy(parameter: JsonObject): Resp[String] = {
    SchedulerProcessor.shutdown()
    Resp.success("")
  }

  override lazy val dependents: mutable.Set[String] =
    mutable.Set(com.ecfront.ez.framework.service.jdbc.ServiceAdapter.serviceName)

  override var serviceName: String = "scheduler"

}


