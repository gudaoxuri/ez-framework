package com.ecfront.ez.framework.service.jdbc.dialect

import com.ecfront.ez.framework.service.jdbc.dialect.DialectType.DialectType

object PostgresDialect extends Dialect {

  def paging(sql: String, pageNumber: Long, pageSize: Long): String = {
    sql + " LIMIT " + pageSize + " OFFSET " + (pageNumber - 1) * pageSize
  }

  def count(sql: String): String = {
    "SELECT count(1) FROM ( " + sql + " ) _ctmp"
  }

  def getTableInfo(tableName: String): String = {
    "SELECT * FROM pg_tables t WHERE t.tablename='" + tableName + "'"
  }

  def getDriver: String = "org.postgresql.Driver"

  def getDialectType: DialectType = DialectType.POSTGRE

  override def createTableIfNotExist(tableName: String, tableDesc: String, fields: List[FiledInfo], indexFields: List[String], uniqueFields: List[String], pkField: String): String = ???

  override def changeTableName(oriTableName: String, newTableName: String): String = ???
}
