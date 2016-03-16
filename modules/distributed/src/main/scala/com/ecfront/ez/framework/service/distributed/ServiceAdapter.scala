package com.ecfront.ez.framework.service.distributed

import com.ecfront.common.Resp
import com.ecfront.ez.framework.core.EZServiceAdapter
import io.vertx.core.json.JsonObject

import scala.collection.mutable

object ServiceAdapter extends EZServiceAdapter[JsonObject] {

  override def init(parameter: JsonObject): Resp[String] = {
    val address = com.ecfront.ez.framework.service.redis.ServiceAdapter.host + ":" + com.ecfront.ez.framework.service.redis.ServiceAdapter.port
    DistributedProcessor.init(
      List(address),
      com.ecfront.ez.framework.service.redis.ServiceAdapter.db,
      com.ecfront.ez.framework.service.redis.ServiceAdapter.auth
    )
  }

  override def destroy(parameter: JsonObject): Resp[String] = {
    DistributedProcessor.close()
    Resp.success("")
  }

  override lazy val dependents: mutable.Set[String] = mutable.Set(
    com.ecfront.ez.framework.service.redis.ServiceAdapter.serviceName
  )

  override var serviceName: String = "distributed"

}


