package com.ecfront.ez.framework.service.storage.jdbc

import com.ecfront.common.Resp
import com.ecfront.ez.framework.service.storage.foundation.Id
import com.typesafe.scalalogging.slf4j.LazyLogging
import io.vertx.core.json.JsonArray
import io.vertx.core.{AsyncResult, Handler}
import io.vertx.ext.sql.{ResultSet, UpdateResult}

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Promise}
import scala.util.Success

private[jdbc] object JDBCExecutor extends LazyLogging {

  def save[M](entityInfo: JDBCEntityInfo, valueInfo: Map[String, Any], clazz: Class[M]): Resp[M] = {
    val tableName = entityInfo.tableName
    val idFieldName = entityInfo.idFieldName
    val richValueInfo = collection.mutable.Map[String, Any]()
    richValueInfo ++= valueInfo
    if (entityInfo.idStrategy == Id.STRATEGY_SEQ && richValueInfo.contains(idFieldName) && richValueInfo(idFieldName) == 0) {
      richValueInfo -= idFieldName
    }
    if (entityInfo.uniqueFieldNames.nonEmpty) {
      val existQuery = entityInfo.uniqueFieldNames.filter(richValueInfo.contains).map {
        field =>
          field + "= ?" -> richValueInfo(field)
      }.toMap
      val existR = JDBCProcessor.exist(
        s"SELECT 1 FROM $tableName WHERE ${existQuery.keys.toList.mkString(" OR ")} ",
        existQuery.values.toList.filter(_ != null)
      )
      if (existR) {
        if (existR.body) {
          Resp.badRequest(entityInfo.uniqueFieldNames.map {
            field =>
              if (entityInfo.fieldLabel.contains(field)) {
                entityInfo.fieldLabel(field)
              } else {
                field
              }
          }.mkString("[", ",", "]") + " must be unique")
        } else {
          doSave(tableName, idFieldName, richValueInfo, clazz)
        }
      } else {
        existR
      }
    } else {
      doSave(tableName, idFieldName, richValueInfo, clazz)
    }
  }

  private def doSave[M](tableName: String, idFieldName: String, richValueInfos: mutable.Map[String, Any], clazz: Class[M]): Resp[M] = {
    val idValue = if (richValueInfos.contains(idFieldName)) richValueInfos(idFieldName) else null
    if (idValue != null) {
      val sql =
        s"""
           |INSERT INTO $tableName
           | (${richValueInfos.keys.mkString(",")})
           | SELECT ${(for (i <- 0 until richValueInfos.size) yield "?").mkString(",")}
           | FROM DUAL WHERE NOT EXISTS ( SELECT 1 FROM $tableName WHERE $idFieldName = ? )
       """.stripMargin
      JDBCProcessor.update(sql, richValueInfos.values.toList ++ List(idValue))
      JDBCProcessor.get(
        s"SELECT * FROM $tableName WHERE $idFieldName  = ? ",
        List(idValue),
        clazz
      )
    } else {
      val p = Promise[Resp[M]]()
      val sql =
        s"""
           |INSERT INTO $tableName
           | (${richValueInfos.keys.mkString(",")})
           | VALUES ( ${(for (i <- 0 until richValueInfos.size) yield "?").mkString(",")} )
       """.stripMargin
      // 要获取保存后的id
      JDBCProcessor.Async.db.onComplete {
        case Success(conn) =>
          conn.updateWithParams(sql,
            new JsonArray(JDBCProcessor.Async.formatParameters(richValueInfos.values.toList)),
            new Handler[AsyncResult[UpdateResult]] {
              override def handle(event: AsyncResult[UpdateResult]): Unit = {
                if (event.succeeded()) {
                  conn.query("SELECT LAST_INSERT_ID()", new Handler[AsyncResult[ResultSet]] {
                    override def handle(event2: AsyncResult[ResultSet]): Unit = {
                      if (event2.succeeded()) {
                        val row = event2.result().getRows.get(0).getLong("LAST_INSERT_ID()")
                        conn.query(s"SELECT * FROM $tableName WHERE $idFieldName = $row ", new Handler[AsyncResult[ResultSet]] {
                          override def handle(event3: AsyncResult[ResultSet]): Unit = {
                            if (event3.succeeded()) {
                              val result = event3.result().getRows.get(0)
                              p.success(Resp.success(JDBCProcessor.Async.convertObject(result, clazz)))
                            } else {
                              logger.error(s"JDBC execute error ", event3.cause())
                              p.success(Resp.serverError(event3.cause().getMessage))
                            }
                            conn.close()
                          }
                        })
                      } else {
                        conn.close()
                        logger.error(s"JDBC execute error ", event2.cause())
                        p.success(Resp.serverError(event2.cause().getMessage))
                      }
                    }
                  })
                } else {
                  conn.close()
                  logger.error(s"JDBC execute error ", event.cause())
                  p.success(Resp.serverError(event.cause().getMessage))
                }
              }
            }
          )
      }
      Await.result(p.future, Duration.Inf)
    }
  }

  def update[M](entityInfo: JDBCEntityInfo, idValue: Any, valueInfo: Map[String, Any], clazz: Class[M]): Resp[M] = {
    val tableName = entityInfo.tableName
    val idFieldName = entityInfo.idFieldName
    val richValueInfo = collection.mutable.Map[String, Any]()
    richValueInfo ++= valueInfo
    if (entityInfo.uniqueFieldNames.nonEmpty) {
      val existQuery = entityInfo.uniqueFieldNames.filter(richValueInfo.contains).map {
        field =>
          field + "= ?" -> richValueInfo(field)
      }.toMap
      val existR = JDBCProcessor.exist(
        s"SELECT 1 FROM $tableName WHERE ${existQuery.keys.toList.mkString(" OR ") + s" AND $idFieldName != ? "} ",
        existQuery.values.toList.filter(_ != null) ++ List(idValue)
      )
      if (existR) {
        if (existR.body) {
          Resp.badRequest(entityInfo.uniqueFieldNames.map {
            field =>
              if (entityInfo.fieldLabel.contains(field)) {
                entityInfo.fieldLabel(field)
              } else {
                field
              }
          }.mkString("[", ",", "]") + " must be unique")
        } else {
          doUpdate(tableName, idFieldName, idValue, richValueInfo, clazz)
        }
      } else {
        existR
      }
    } else {
      doUpdate(tableName, idFieldName, idValue, richValueInfo, clazz)
    }
  }

  private def doUpdate[M](tableName: String, idFieldName: String, idValue: Any, richValueInfos: mutable.Map[String, Any], clazz: Class[M]): Resp[M] = {
    val sql =
      s"""
         |INSERT INTO $tableName
         | (${richValueInfos.keys.toList.mkString(",")})
         | VALUES ( ${(for (i <- 0 until richValueInfos.size) yield "?").mkString(",")} )
         | ON DUPLICATE KEY UPDATE
         | ${richValueInfos.keys.filterNot(_ == idFieldName).toList.map(key => s"$key = VALUES($key)").mkString(",")}
       """.stripMargin
    val updateR = JDBCProcessor.update(sql, richValueInfos.values.toList)
    if (updateR) {
      JDBCProcessor.get(
        s"SELECT * FROM $tableName WHERE $idFieldName  = ? ",
        List(idValue),
        clazz
      )
    } else {
      updateR
    }
  }

  def saveOrUpdate[M](entityInfo: JDBCEntityInfo, idValue: Any, valueInfos: Map[String, Any], clazz: Class[M]): Resp[M] = {
    val idFieldName = entityInfo.idFieldName
    if (!valueInfos.contains(idFieldName) || entityInfo.idStrategy == Id.STRATEGY_SEQ
      && valueInfos.contains(idFieldName) && valueInfos(idFieldName) == 0) {
      save(entityInfo, valueInfos, clazz)
    } else {
      update(entityInfo, valueInfos(idFieldName), valueInfos, clazz)
    }
  }

}
