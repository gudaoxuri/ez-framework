package com.ecfront.ez.framework.service.jdbc

import com.ecfront.common.Resp
import com.ecfront.ez.framework.core.EZServiceAdapter
import com.fasterxml.jackson.databind.JsonNode

object ServiceAdapter extends EZServiceAdapter[JsonNode] {

  override def init(parameter: JsonNode): Resp[String] = {
    val processor=JDBCProcessor(
      parameter.path("url").asText(),
      parameter.path("userName").asText(),
      parameter.path("password").asText()
    )
    if(parameter.has("initialSize")){
      processor.setInitialSize(parameter.path("initialSize").asInt())
    }
    if(parameter.has("maxActive")){
      processor.setMaxActive(parameter.path("maxActive").asInt())
    }
    if(parameter.has("minIdle")){
      processor.setMinIdle(parameter.path("minIdle").asInt())
    }
    if(parameter.has("maxWait")){
      processor.setMaxWait(parameter.path("maxWait").asInt())
    }
    JDBCProcessor.initDS(processor)
  }

  override def destroy(parameter: JsonNode): Resp[String] = {
    JDBCProcessor.close()
    Resp.success("")
  }

  override var serviceName: String = "jdbc"

}


