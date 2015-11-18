package com.asto.ez.framework.storage.jdbc.dialect

import java.sql.SQLException
import java.util.Objects

import sun.reflect.generics.reflectiveObjects.NotImplementedException

object MySQLDialect extends Dialect{

  def paging(sql: String, pageNumber: Long, pageSize: Long): String = {
     sql + " LIMIT " + (pageNumber - 1) * pageSize + ", " + pageSize
  }

  def count(sql: String): String = {
     "SELECT COUNT(1) FROM ( " + sql + " ) _" + System.currentTimeMillis
  }

  def getTableInfo(tableName: String): String = {
    throw new NotImplementedException
  }

  //TODO
  def createTableIfNotExist(tableName: String, tableDesc: String, fields: Map[String, String], fieldsDesc: Map[String, String], indexFields: List[String], uniqueFields: List[String], pkField: String): String = {
    val sb: StringBuilder = new StringBuilder("CREATE TABLE IF NOT EXISTS " + tableName + " ( ")
    import scala.collection.JavaConversions._
    for (field <- fields.entrySet) {
      val t: String =field.getValue.toLowerCase match {
        case "int" => "INT"
        case "integer" => "INT"
        case "long" => "BIGINT"
        case "short" => "SMALLINT"
        case "string" => "VARCHAR(65535)"
        case "bool" => "BOOLEAN"
        case "boolean" => "BOOLEAN"
        case "float" => "FLOAT"
        case "double" => "DOUBLE"
        case "char" => "CHAR"
        case "date" => "TIMESTAMP"
        case "uuid" => "UUID"
        case "decimal" => "DECIMAL"
        case _ =>
          throw new SQLException(s"Not support type [${field.getValue}]" )
      }
      sb.append(field.getKey).append(" ").append(t).append(" ,")
    }
    if (pkField != null && !Objects.equals(pkField.trim, "")) {
       sb.append("primary key(").append(pkField.trim).append(") )").toString
    }
    else {
       sb.substring(0, sb.length - 1) + ")"
    }


  }

}
