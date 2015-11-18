package com.asto.ez.framework.storage.jdbc.dialect

trait Dialect {

  def paging(sql: String, pageNumber: Long, pageSize: Long): String

  def count(sql: String): String

  def getTableInfo(tableName: String): String

  def createTableIfNotExist(tableName: String, tableDesc: String, fields: Map[String, String], fieldsDesc: Map[String, String], indexFields: List[String], uniqueFields: List[String], pkField: String): String

}
