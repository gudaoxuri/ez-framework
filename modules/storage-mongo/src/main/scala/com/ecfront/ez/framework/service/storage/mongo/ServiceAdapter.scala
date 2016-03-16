package com.ecfront.ez.framework.service.storage.mongo

import com.ecfront.common.Resp
import com.ecfront.ez.framework.core.{EZContext, EZServiceAdapter}
import io.vertx.core.json.JsonObject
import io.vertx.ext.mongo.MongoClient

object ServiceAdapter extends EZServiceAdapter[JsonObject] {

  override def init(parameter: JsonObject): Resp[String] = {
    MongoProcessor.mongoClient = MongoClient.createShared(EZContext.vertx, parameter)
    Resp.success("")
  }

  override def destroy(parameter: JsonObject): Resp[String] = {
    Resp.success("")
  }

  override var serviceName: String = "storage.mongo"


}


