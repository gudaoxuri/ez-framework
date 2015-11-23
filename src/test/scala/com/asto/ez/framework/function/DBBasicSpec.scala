package com.asto.ez.framework.function

import com.asto.ez.framework.helper.DBHelper
import com.typesafe.scalalogging.slf4j.LazyLogging
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.jdbc.JDBCClient
import org.scalatest.{BeforeAndAfter, FunSuite}

abstract class DBBasicSpec extends FunSuite with BeforeAndAfter with LazyLogging {

  before {
    DBHelper.dbClient = JDBCClient.createShared(Vertx.vertx(), new JsonObject()
      .put("url", "jdbc:mysql://192.168.4.99:3306/dop?characterEncoding=UTF-8")
      .put("driver_class", "com.mysql.jdbc.Driver")
      .put("user", "root")
      .put("password", "123456")
      .put("max_pool_size", 30))
  }

}

