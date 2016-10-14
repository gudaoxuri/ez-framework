package com.ecfront.ez.framework.service.jdbc.dialect

import com.ecfront.ez.framework.service.jdbc.dialect.DialectType.DialectType


object H2Dialect extends Dialect {

  def paging(sql: String, pageNumber: Long, pageSize: Long): String = {
    sql + " LIMIT " + pageSize + " OFFSET " + (pageNumber - 1) * pageSize
  }

  def count(sql: String): String = {
    "SELECT count(1) FROM ( " + sql + " ) "
  }

  def getTableInfo(tableName: String): String = {
    "SELECT * FROM INFORMATION_SCHEMA.TABLES t WHERE t.table_name ='" + tableName + "'"
  }

  def getDriver: String = "org.h2.Driver"

  def getDialectType: DialectType = DialectType.H2

}
