package com.asto.ez.framework.function

import com.asto.ez.framework.storage.jdbc.DBHelper
import com.asto.ez.framework.{BasicSpec, EZGlobal}
import io.vertx.ext.jdbc.JDBCClient

abstract class JDBCBasicSpec extends BasicSpec {

  override def before2(): Any = {
    val jdbc = EZGlobal.ez.getJsonObject("jdbc")
    DBHelper.dbClient = JDBCClient.createShared(EZGlobal.vertx, jdbc)
  }

}

