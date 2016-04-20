package com.ecfront.ez.framework.service.distributed

import com.ecfront.common.Resp
import com.ecfront.ez.framework.core.EZServiceAdapter
import io.vertx.core.json.JsonObject

import scala.collection.mutable

object ServiceAdapter extends EZServiceAdapter[JsonObject] {

  override def init(parameter: JsonObject): Resp[String] = {
    Resp.success("")
  }

  override def destroy(parameter: JsonObject): Resp[String] = {
    Resp.success("")
  }

  override lazy val dependents: mutable.Set[String] = mutable.Set(
    com.ecfront.ez.framework.service.redis.ServiceAdapter.serviceName
  )

  override var serviceName: String = "distributed"

}


