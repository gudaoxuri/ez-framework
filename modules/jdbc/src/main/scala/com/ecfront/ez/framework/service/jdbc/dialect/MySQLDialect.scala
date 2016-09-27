package com.ecfront.ez.framework.service.jdbc.dialect

import com.ecfront.ez.framework.service.jdbc.dialect.DialectType.DialectType
import sun.reflect.generics.reflectiveObjects.NotImplementedException

object MySQLDialect extends Dialect {

  def paging(sql: String, pageNumber: Long, pageSize: Long): String = {
    sql + " LIMIT " + (pageNumber - 1) * pageSize + ", " + pageSize
  }

  def count(sql: String): String = {
    "SELECT count(1) FROM ( " + sql + " ) _ctmp"
  }

  def getTableInfo(tableName: String): String = {
    //TODO
    throw new NotImplementedException
  }

  def getDriver: String = "com.mysql.jdbc.Driver"

  def getDialectType: DialectType = DialectType.MYSQL

}
