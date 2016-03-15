package com.ecfront.ez.framework.service.redis

import com.ecfront.common.Resp
import com.ecfront.ez.framework.core.{EZContext, EZServiceAdapter}
import io.vertx.core.json.JsonObject

object ServiceAdapter extends EZServiceAdapter[JsonObject] {

  override def init(parameter: JsonObject): Resp[String] = {
    RedisProcessor.init(
      EZContext.vertx, parameter.getString("host"),
      parameter.getInteger("port"),
      parameter.getInteger("db",0),
      parameter.getString("auth", null)
    )
  }

  override def destroy(parameter: JsonObject): Resp[String] = {
    Resp.success("")
  }

}


