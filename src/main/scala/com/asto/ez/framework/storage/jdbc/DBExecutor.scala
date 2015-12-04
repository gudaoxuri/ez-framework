package com.asto.ez.framework.storage.jdbc

import com.asto.ez.framework.EZContext
import com.asto.ez.framework.storage.Page
import com.asto.ez.framework.storage.jdbc.JDBCEntityContainer.JDBCEntityInfo
import com.ecfront.common.Resp

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}

object DBExecutor {

  def save(entityInfo: JDBCEntityInfo, context: EZContext, valueInfos: Map[String, Any]): Future[Resp[String]] = {
    val p = Promise[Resp[String]]()
    val tableName = entityInfo.tableName
    val idFieldName = entityInfo.idFieldName
    val clazz = entityInfo.clazz
    val richValueInfos = collection.mutable.Map[String, Any]()
    richValueInfos ++= valueInfos
    if (entityInfo.idStrategy == Id.STRATEGY_SEQ && richValueInfos.contains(idFieldName) && richValueInfos(idFieldName) == 0) {
      richValueInfos -= idFieldName
    }
    val idValue = if (richValueInfos.contains(idFieldName)) richValueInfos(idFieldName) else null
    if (idValue != null) {
      val sql =
        s"""
           |INSERT INTO $tableName
           | (${richValueInfos.keys.mkString(",")})
           | SELECT ${(for (i <- 0 until richValueInfos.size) yield "?").mkString(",")}
           | FROM DUAL WHERE NOT EXISTS ( SELECT 1 FROM $tableName WHERE $idFieldName = ? )
       """.stripMargin
      DBProcessor.update(sql, richValueInfos.values.toList ++ List(idValue)).onSuccess {
        case resp =>
          if (resp) {
            p.success(Resp.success(null))
          } else {
            p.success(resp)
          }
      }
    } else {
      val sql =
        s"""
           |INSERT INTO $tableName
           | (${richValueInfos.keys.mkString(",")})
           | VALUES ( ${(for (i <- 0 until richValueInfos.size) yield "?").mkString(",")} )
       """.stripMargin
      DBProcessor.update(sql, richValueInfos.values.toList).onSuccess {
        case resp =>
          if (resp) {
            p.success(Resp.success(null))
          } else {
            p.success(resp)
          }
      }
    }
    p.future
  }

  def update(entityInfo: JDBCEntityInfo, context: EZContext, idValue: Any, valueInfos: Map[String, Any]): Future[Resp[String]] = {
    val p = Promise[Resp[String]]()
    val idFieldName = entityInfo.idFieldName
    val clazz = entityInfo.clazz
    val richValueInfos = collection.mutable.Map[String, Any]()
    richValueInfos ++= valueInfos.filterNot(_._1 == entityInfo.idFieldName)
    //keys.toList.map ，toList 让map有序
    val newValues = richValueInfos.keys.toList.map(key => s"$key = ? ").mkString(",")
    val condition = s" $idFieldName = ? "
    update(entityInfo, context, newValues, condition, richValueInfos.values.toList ++ List(idValue)).onSuccess {
      case resp =>
        if (resp) {
          p.success(Resp.success(null))
        } else {
          p.success(resp)
        }
    }
    p.future
  }

  def saveOrUpdate(entityInfo: JDBCEntityInfo, context: EZContext, idValue: Any, valueInfos: Map[String, Any]): Future[Resp[String]] = {
    val p = Promise[Resp[String]]()
    val tableName = entityInfo.tableName
    val idFieldName = entityInfo.idFieldName
    val idValue = if (valueInfos.contains(idFieldName)) valueInfos(idFieldName) else null
    if (idValue != null) {
      val richValueInfos = collection.mutable.Map[String, Any]()
      richValueInfos ++= valueInfos
      if (entityInfo.idStrategy == Id.STRATEGY_SEQ && richValueInfos.contains(idFieldName) && richValueInfos(idFieldName) == 0) {
        richValueInfos -= idFieldName
      }
      val sql =
        s"""
           |INSERT INTO $tableName
           | (${richValueInfos.keys.toList.mkString(",")})
           | VALUES ( ${(for (i <- 0 until richValueInfos.size) yield "?").mkString(",")} )
           | ON DUPLICATE KEY UPDATE
           | ${richValueInfos.keys.filterNot(_ == idFieldName).toList.map(key => s"$key = VALUES($key)").mkString(",")}
       """.stripMargin
      DBProcessor.update(sql, richValueInfos.values.toList).onSuccess {
        case resp =>
          if (resp) {
            p.success(Resp.success(null))
          } else {
            p.success(resp)
          }
      }
    } else {
      save(entityInfo, context, valueInfos)
    }
    p.future
  }

  def update(entityInfo: JDBCEntityInfo, context: EZContext, newValues: String, condition: String, parameters: List[Any]): Future[Resp[Void]] = {
    val tableName = entityInfo.tableName
    DBProcessor.update(
      s"UPDATE $tableName Set $newValues WHERE $condition",
      parameters
    )
  }

  def delete(entityInfo: JDBCEntityInfo, context: EZContext, idValue: Any): Future[Resp[Void]] = {
    val tableName = entityInfo.tableName
    val idFieldName = entityInfo.idFieldName
    DBProcessor.update(
      s"DELETE FROM $tableName WHERE $idFieldName = ? ",
      List(idValue)
    )
  }

  def delete(entityInfo: JDBCEntityInfo, context: EZContext, condition: String, parameters: List[Any]): Future[Resp[Void]] = {
    val tableName = entityInfo.tableName
    DBProcessor.update(
      s"DELETE FROM $tableName WHERE $condition ",
      parameters
    )
  }

  def get[E](entityInfo: JDBCEntityInfo, context: EZContext, idValue: Any): Future[Resp[E]] = {
    val tableName = entityInfo.tableName
    val idFieldName = entityInfo.idFieldName
    val clazz = entityInfo.clazz.asInstanceOf[Class[E]]
    DBProcessor.get(
      s"SELECT * FROM $tableName WHERE $idFieldName  = ? ",
      List(idValue),
      clazz
    )
  }

  def get[E](entityInfo: JDBCEntityInfo, context: EZContext, condition: String, parameters: List[Any]): Future[Resp[E]] = {
    val tableName = entityInfo.tableName
    val clazz = entityInfo.clazz.asInstanceOf[Class[E]]
    DBProcessor.get(
      s"SELECT * FROM $tableName WHERE $condition ",
      parameters,
      clazz
    )
  }

  def existById(entityInfo: JDBCEntityInfo, context: EZContext, idValue: Any): Future[Resp[Boolean]] = {
    val tableName = entityInfo.tableName
    val idFieldName = entityInfo.idFieldName
    DBProcessor.exist(
      s"SELECT 1 FROM $tableName WHERE $idFieldName  = ? ",
      List(idValue)
    )
  }

  def existByCond(entityInfo: JDBCEntityInfo, context: EZContext, condition: String, parameters: List[Any]): Future[Resp[Boolean]] = {
    val tableName = entityInfo.tableName
    DBProcessor.exist(
      s"SELECT 1 FROM $tableName WHERE $condition ",
      parameters
    )
  }

  def find[E](entityInfo: JDBCEntityInfo, context: EZContext, condition: String = " 1=1 ", parameters: List[Any] = List()): Future[Resp[List[E]]] = {
    val tableName = entityInfo.tableName
    val clazz = entityInfo.clazz.asInstanceOf[Class[E]]
    DBProcessor.find(
      s"SELECT * FROM $tableName WHERE $condition ",
      parameters,
      clazz
    )
  }

  def page[E](entityInfo: JDBCEntityInfo, context: EZContext, condition: String = " 1=1 ", parameters: List[Any] = List(), pageNumber: Long = 1, pageSize: Int = 10): Future[Resp[Page[E]]] = {
    val tableName = entityInfo.tableName
    val clazz = entityInfo.clazz.asInstanceOf[Class[E]]
    DBProcessor.page(
      s"SELECT * FROM $tableName WHERE $condition ",
      parameters,
      pageNumber, pageSize,
      clazz
    )
  }

  def count(entityInfo: JDBCEntityInfo, context: EZContext, condition: String = " 1=1 ", parameters: List[Any] = List()): Future[Resp[Long]] = {
    val tableName = entityInfo.tableName
    DBProcessor.count(
      s"SELECT count(1) FROM $tableName WHERE $condition ",
      parameters
    )
  }

}
