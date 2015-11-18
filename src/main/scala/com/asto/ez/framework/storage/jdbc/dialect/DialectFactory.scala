package com.asto.ez.framework.storage.jdbc.dialect

import com.asto.ez.framework.storage.jdbc.dialect.DialectType.DialectType


object DialectFactory {

  /*var dialect:Dialect=_

  def parseDialect(driver: String): Dialect = {
    dialect=getDialectType(driver) match {
      case DialectType.ORACLE =>
         new OracleDialect
      case DialectType.H2 =>
         new H2Dialect
      case DialectType.MYSQL =>
         new MySQLDialect
      case DialectType.POSTGRE =>
         new PostgresDialect
      case _ =>
         throw new Exception(s"Not found driver type [$driver] ")
    }
  }

  def getDialectType(driver: String): DialectType = {
    driver match {
      case d if d.contains("OracleDriver") => DialectType.ORACLE
      case d if d.contains("h2") => DialectType.H2
      case d if d.contains("mysql")  => DialectType.MYSQL
      case d if d.contains("postgresql") =>  DialectType.POSTGRE
      case _ => null
    }
  }*/
}

object DialectType extends Enumeration {
  type DialectType = Value
  val ORACLE, MYSQL, POSTGRE, H2, SPARK_SQL = Value
}

