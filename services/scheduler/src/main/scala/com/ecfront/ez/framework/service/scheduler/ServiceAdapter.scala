package com.ecfront.ez.framework.service.scheduler

import com.ecfront.common.Resp
import com.ecfront.ez.framework.core.{EZ, EZServiceAdapter}
import com.fasterxml.jackson.databind.JsonNode

import scala.collection.mutable

object ServiceAdapter extends EZServiceAdapter[JsonNode] {

  override def init(parameter: JsonNode): Resp[String] = {
    SchedulerProcessor.init(EZ.Info.module)
    Resp.success("")
  }

  override def destroy(parameter: JsonNode): Resp[String] = {
    SchedulerProcessor.shutdown()
    Resp.success("")
  }

  override lazy val dependents: mutable.Set[String] =
    mutable.Set(com.ecfront.ez.framework.service.jdbc.ServiceAdapter.serviceName)

  override var serviceName: String = "scheduler"

}


