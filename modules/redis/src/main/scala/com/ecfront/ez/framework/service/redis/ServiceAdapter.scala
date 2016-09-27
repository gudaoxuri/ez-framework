package com.ecfront.ez.framework.service.redis

import com.ecfront.common.Resp
import com.ecfront.ez.framework.core.EZServiceAdapter
import com.fasterxml.jackson.databind.JsonNode

object ServiceAdapter extends EZServiceAdapter[JsonNode] {

  override def init(parameter: JsonNode): Resp[String] = {
    val address = parameter.path("address").asText().split(";")
    val db = parameter.path("db").asInt(0)
    val auth = parameter.path("auth").asText("")
    RedisProcessor.init(address, db, auth)
  }

  override def destroy(parameter: JsonNode): Resp[String] = {
    RedisProcessor.close()
    Resp.success("")
  }

  override var serviceName: String = "redis"

}


