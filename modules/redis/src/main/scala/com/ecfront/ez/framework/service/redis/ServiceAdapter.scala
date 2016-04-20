package com.ecfront.ez.framework.service.redis

import com.ecfront.common.Resp
import com.ecfront.ez.framework.core.EZServiceAdapter
import io.vertx.core.json.JsonObject

object ServiceAdapter extends EZServiceAdapter[JsonObject] {

  private val DEFAULT_REDIS_PORT: Integer = 6379


  override def init(parameter: JsonObject): Resp[String] = {
    val host = parameter.getString("host", "127.0.0.1")
    val port = parameter.getInteger("port", DEFAULT_REDIS_PORT)
    var address = host + ":" + port
    if (parameter.containsKey("address")) {
      address = parameter.getString("address")
    }
    val db = parameter.getInteger("db", 0)
    val auth = parameter.getString("auth", null)
    RedisProcessor.init(List(address), db, auth)
  }

  override def destroy(parameter: JsonObject): Resp[String] = {
    RedisProcessor.close()
    Resp.success("")
  }

  override var serviceName: String = "redis"

}


