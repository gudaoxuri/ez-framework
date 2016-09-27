package com.ecfront.ez.framework.service.email

import com.ecfront.common.Resp
import com.ecfront.ez.framework.core.EZServiceAdapter
import com.fasterxml.jackson.databind.JsonNode

object ServiceAdapter extends EZServiceAdapter[JsonNode] {

  override def init(parameter: JsonNode): Resp[String] = {
    EmailProcessor.init(
      parameter.path("host").asText(),
      parameter.path("port").asInt(),
      parameter.path("userName").asText(),
      parameter.path("password").asText(),
      parameter.path("protocol").asText(),
      parameter.path("poolSize").asInt(-1),
      parameter.path("defaultSender").asText(),
      parameter.path("defaultSendAddress").asText()
    )
  }

  override def destroy(parameter: JsonNode): Resp[String] = {
    Resp.success("")
  }

  override var serviceName: String = "email"

}


