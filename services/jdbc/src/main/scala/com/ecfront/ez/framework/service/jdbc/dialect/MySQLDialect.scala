package com.ecfront.ez.framework.service.jdbc.dialect

import java.sql.SQLException

import com.ecfront.ez.framework.service.jdbc.dialect.DialectType.DialectType

object MySQLDialect extends Dialect {

  def paging(sql: String, pageNumber: Long, pageSize: Long): String = {
    sql + " LIMIT " + (pageNumber - 1) * pageSize + ", " + pageSize
  }

  def count(sql: String): String = {
    "SELECT count(1) FROM ( " + sql + " ) _ctmp"
  }

  def getTableInfo(tableName: String): String = ???

  override def createTableIfNotExist(tableName: String, tableDesc: String, fields: List[FiledInfo],
                                     indexFields: List[String], uniqueFields: List[String], pkField: String): String = {
    val ddl = new StringBuilder(s"CREATE TABLE IF NOT EXISTS `$tableName` ( ")
    fields.reverse.foreach {
      field =>
        val columnName = field.name.toLowerCase
        val desc = s"COMMENT '${field.desc}'"
        val len = field.len
        val scale = field.scale
        val columnExt = field.dType.toLowerCase match {
          case "seq" => s"INT NOT NULL AUTO_INCREMENT"
          case t if t == "int" || t == "integer" =>
            s"INT${if (len == 0) "" else "(" + len + ")"} NOT NULL ${if (pkField != null && pkField.toLowerCase == columnName) "AUTO_INCREMENT" else """DEFAULT "0""""}"
          case "long" => s"""BIGINT${if (len == 0) "" else "(" + len + ")"} NOT NULL DEFAULT "0""""
          case "short" => s"""SMALLINT${if (len == 0) "" else "(" + len + ")"} NOT NULL DEFAULT "0""""
          case "string" =>
            if (len == 0) {
              s"""TEXT NOT NULL"""
            } else {
              s"""VARCHAR${if (len == 0) "" else "(" + len + ")"} NOT NULL DEFAULT """""
            }
          case t if t == "bool" || t == "boolean" => s"""BOOLEAN NOT NULL DEFAULT "0""""
          case "float" => s"""FLOAT${if (len == 0) "" else "(" + len + "," + scale + ")"} NOT NULL DEFAULT "0""""
          case "double" => s"""DOUBLE${if (len == 0) "" else "(" + len + "," + scale + ")"} NOT NULL DEFAULT "0""""
          case "bigdecimal" => s"""DECIMAL${if (len == 0) "" else "(" + len + "," + scale + ")"} NOT NULL DEFAULT "0""""
          case "java.util.date" => s"""DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP"""
          case t =>
            throw new SQLException("Not support data type:" + t)
        }
        ddl.append("\r\n    `" + columnName + "` " + columnExt + " " + desc + ",")
    }
    if (pkField != null && pkField.trim.nonEmpty) {
      ddl.append(s"\r\n    PRIMARY KEY (`${pkField.toLowerCase}`),")
    }
    if (uniqueFields != null && uniqueFields.nonEmpty) {
      uniqueFields.foreach {
        uniqueField =>
          ddl.append(s"\r\n    UNIQUE KEY `uni_${uniqueField.toLowerCase}` (`${uniqueField.toLowerCase}`),")
      }
    }
    if (indexFields != null && indexFields.nonEmpty) {
      indexFields.foreach {
        indexField =>
          ddl.append(s"\r\n    KEY `idx_${indexField.toLowerCase}` (`${indexField.toLowerCase}`),")
      }
    }
    ddl.substring(0, ddl.length - 1) + s"\r\n) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='$tableDesc'"
  }

  override def changeTableName(oriTableName: String, newTableName: String): String = {
    s"RENAME TABLE $oriTableName TO $newTableName"
  }

  def getDriver: String = "com.mysql.jdbc.Driver"

  def getDialectType: DialectType = DialectType.MYSQL

}
