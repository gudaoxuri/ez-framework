package com.ecfront.ez.framework.service.kafka

import com.ecfront.common.Resp
import com.ecfront.ez.framework.core.EZServiceAdapter
import io.vertx.core.json.JsonObject

object ServiceAdapter extends EZServiceAdapter[JsonObject] {

  override def init(parameter: JsonObject): Resp[String] = {
    KafkaProcessor.init(parameter.getString("brokerList"))
    Resp.success("")
  }

  override def destroy(parameter: JsonObject): Resp[String] = {
    KafkaProcessor.close()
    Resp.success("")
  }

  override var serviceName: String = "kafka"

}


