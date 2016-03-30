package com.ecfront.ez.framework.service.redis

import com.ecfront.common.Resp
import com.ecfront.ez.framework.core.{EZContext, EZServiceAdapter}
import io.vertx.core.json.JsonObject

object ServiceAdapter extends EZServiceAdapter[JsonObject] {

  private val DEFAULT_REDIS_PORT: Integer = 6379

  var host: String = _
  var port: Int = _
  var db: Int = _
  var auth: String = _

  override def init(parameter: JsonObject): Resp[String] = {
    host = parameter.getString("host", "127.0.0.1")
    port = parameter.getInteger("port", DEFAULT_REDIS_PORT)
    db = parameter.getInteger("db", 0)
    auth = parameter.getString("auth", null)
    RedisProcessor.init(EZContext.vertx, host, port, db, auth)
  }

  override def destroy(parameter: JsonObject): Resp[String] = {
    Resp.success("")
  }

  override var serviceName: String = "redis"

}


