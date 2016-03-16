package com.ecfront.ez.framework.service.redis

import com.ecfront.common.Resp
import com.ecfront.ez.framework.core.{EZContext, EZServiceAdapter}
import io.vertx.core.json.JsonObject

object ServiceAdapter extends EZServiceAdapter[JsonObject] {

  var host: String = ""
  var port: Int = 0
  var db: Int = 0
  var auth: String = ""

  override def init(parameter: JsonObject): Resp[String] = {
    host = parameter.getString("host")
    port = parameter.getInteger("port")
    db = parameter.getInteger("db", 0)
    auth = parameter.getString("auth", null)
    RedisProcessor.init(EZContext.vertx, host, port, db, auth)
  }

  override def destroy(parameter: JsonObject): Resp[String] = {
    Resp.success("")
  }

  override var serviceName: String = "redis"

}


