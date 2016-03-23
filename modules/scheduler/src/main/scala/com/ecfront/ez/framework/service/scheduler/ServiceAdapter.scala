package com.ecfront.ez.framework.service.scheduler

import com.ecfront.common.Resp
import com.ecfront.ez.framework.core.{EZContext, EZServiceAdapter}
import io.vertx.core.json.JsonObject

object ServiceAdapter extends EZServiceAdapter[JsonObject] {

  var mongoStorage: Boolean = false

  override def init(parameter: JsonObject): Resp[String] = {
    mongoStorage = parameter.getString("storage", "mongo") == "mongo"
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


