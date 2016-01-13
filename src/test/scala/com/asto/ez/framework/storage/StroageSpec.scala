package com.asto.ez.framework.storage

import com.asto.ez.framework.storage.jdbc.DBProcessor
import com.asto.ez.framework.storage.mongo.MongoProcessor
import com.asto.ez.framework.{BasicSpec, EZGlobal}
import io.vertx.ext.jdbc.JDBCClient
import io.vertx.ext.mongo.MongoClient

abstract class StroageSpec extends BasicSpec {

  override def before2(): Any = {
    val jdbc = EZGlobal.ez_storage.getJsonObject("jdbc")
    DBProcessor.dbClient = JDBCClient.createShared(EZGlobal.vertx, jdbc)
    val mongo = EZGlobal.ez_storage.getJsonObject("mongo")
    MongoProcessor.mongoClient = MongoClient.createShared(EZGlobal.vertx, mongo)
  }

}

