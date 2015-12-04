package com.asto.ez.framework.storage

import com.asto.ez.framework.storage.jdbc.DBProcessor
import com.asto.ez.framework.{BasicSpec, EZGlobal}
import io.vertx.ext.jdbc.JDBCClient

abstract class JDBCBasicSpec extends BasicSpec {

  override def before2(): Any = {
    val jdbc = EZGlobal.ez_storage.getJsonObject("jdbc")
    DBProcessor.dbClient = JDBCClient.createShared(EZGlobal.vertx, jdbc)
  }

}

