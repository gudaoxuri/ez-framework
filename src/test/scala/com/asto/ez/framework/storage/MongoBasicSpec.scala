package com.asto.ez.framework.storage

import com.asto.ez.framework.storage.mongo.MongoProcessor
import com.asto.ez.framework.{BasicSpec, EZGlobal}
import io.vertx.ext.mongo.MongoClient

abstract class MongoBasicSpec extends BasicSpec {

  override def before2(): Any = {
    val mongo = EZGlobal.ez_storage.getJsonObject("mongo")
    MongoProcessor.mongoClient = MongoClient.createShared(EZGlobal.vertx, mongo)
  }
}

