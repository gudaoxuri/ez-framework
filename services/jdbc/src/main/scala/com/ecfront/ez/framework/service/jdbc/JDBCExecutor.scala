package com.ecfront.ez.framework.service.jdbc

import com.ecfront.common.Resp
import com.ecfront.ez.framework.core.EZ
import com.ecfront.ez.framework.core.i18n.I18NProcessor.Impl
import com.ecfront.ez.framework.core.logger.Logging

import scala.collection.mutable

private[jdbc] object JDBCExecutor extends Logging {

  def save[M](entityInfo: EntityInfo, valueInfo: Map[String, Any], clazz: Class[M]): Resp[M] = {
    val tableName = entityInfo.tableName
    val idFieldName = entityInfo.idFieldName
    val uuidFieldName = entityInfo.uuidFieldName
    val richValueInfo = collection.mutable.Map[String, Any]()
    richValueInfo ++= valueInfo
    if (entityInfo.idStrategy == Id.STRATEGY_SEQ && richValueInfo.contains(idFieldName) && richValueInfo(idFieldName) == 0) {
      richValueInfo -= idFieldName
    }
    if (uuidFieldName != null && !richValueInfo.contains(uuidFieldName)) {
      richValueInfo += uuidFieldName -> EZ.createUUID
    }
    if (entityInfo.uniqueFieldNames.nonEmpty && (entityInfo.uniqueFieldNames.toSet & richValueInfo.keys.toSet).nonEmpty) {
      val existQuery = entityInfo.uniqueFieldNames.filter(richValueInfo.contains).map {
        field =>
          field + "= ?" -> richValueInfo(field)
      }.filterNot(field => field._2.isInstanceOf[String] && field._2.asInstanceOf[String].isEmpty).toMap
      val existR = JDBCProcessor.exist(
        s"SELECT 1 FROM $tableName WHERE ${existQuery.keys.toList.mkString(" OR ")} ",
        existQuery.values.toList.filter(_ != null)
      )
      if (!existR) {
        existR
      } else {
        if (existR.body) {
          val badRequest = entityInfo.uniqueFieldNames.map {
            field =>
              if (entityInfo.fieldDesc.contains(field)) {
                entityInfo.fieldDesc(field)._1.x
              } else {
                field.x
              }
          }.mkString("[", ",", "]") + " must be unique"
          logger.warn(badRequest)
          Resp.badRequest(badRequest)
        } else {
          doSave(tableName, idFieldName, richValueInfo, clazz, entityInfo)
        }
      }
    } else {
      doSave(tableName, idFieldName, richValueInfo, clazz, entityInfo)
    }
  }

  private def doSave[M](tableName: String, idFieldName: String, richValueInfo: mutable.Map[String, Any], clazz: Class[M], entityInfo: EntityInfo): Resp[M] = {
    var fieldsSql = richValueInfo.keys.mkString(",")
    var valuesSql = (for (_ <- 0 until richValueInfo.size) yield "?").mkString(",")
    if (entityInfo.nowBySaveFieldNames.nonEmpty) {
      fieldsSql += entityInfo.nowBySaveFieldNames.mkString(",", ",", "")
      valuesSql += entityInfo.nowBySaveFieldNames.map(_ => " now() ").mkString(",", ",", "")
    }
    val saveR = JDBCProcessor.insert(s"INSERT INTO $tableName ( $fieldsSql ) VALUES ( $valuesSql )", richValueInfo.values.toList)
    if (!saveR) {
      saveR
    } else {
      val idValue =
        if (richValueInfo.contains(idFieldName)) {
          richValueInfo(idFieldName)
        } else {
          saveR.body
        }
      JDBCProcessor.get(s"SELECT * FROM $tableName WHERE $idFieldName  = ? ", List(idValue), clazz)
    }
  }

  def update[M](entityInfo: EntityInfo, idValue: Any, valueInfo: Map[String, Any], clazz: Class[M]): Resp[M] = {
    val tableName = entityInfo.tableName
    val idFieldName = entityInfo.idFieldName
    val richValueInfo = collection.mutable.Map[String, Any]()
    richValueInfo ++= valueInfo.filterNot(_._1 == SecureModel.CREATE_TIME_FLAG)
    if (!richValueInfo.forall(_._1 == idFieldName)) {
      if (entityInfo.uniqueFieldNames.nonEmpty && (entityInfo.uniqueFieldNames.toSet & richValueInfo.keys.toSet).nonEmpty) {
        val existQuery = entityInfo.uniqueFieldNames.filter(richValueInfo.contains).map {
          field =>
            field + "= ?" -> richValueInfo(field)
        }.toMap
        val existR = JDBCProcessor.exist(
          s"SELECT 1 FROM $tableName WHERE ${existQuery.keys.toList.mkString("( ", "OR ", " )") + s" AND $idFieldName != ? "} ",
          existQuery.values.toList.filter(_ != null) ++ List(idValue)
        )
        if (!existR) {
          existR
        } else {
          if (existR.body) {
            val badRequest = entityInfo.uniqueFieldNames.map {
              field =>
                if (entityInfo.fieldDesc.contains(field)) {
                  entityInfo.fieldDesc(field)._1.x
                } else {
                  field.x
                }
            }.mkString("[", ",", "]") + " must be unique"
            logger.warn(badRequest)
            Resp.badRequest(badRequest)
          } else {
            doUpdate(tableName, idFieldName, idValue, richValueInfo, clazz, entityInfo)
          }
        }
      } else {
        doUpdate(tableName, idFieldName, idValue, richValueInfo, clazz, entityInfo)
      }
    } else {
      JDBCProcessor.get(s"SELECT * FROM $tableName WHERE $idFieldName  = ? ", List(idValue), clazz)
    }
  }

  private def doUpdate[M](tableName: String, idFieldName: String, idValue: Any, richValueInfo: mutable.Map[String, Any],
                          clazz: Class[M], entityInfo: EntityInfo): Resp[M] = {
    val setFields = richValueInfo.filterNot(_._1 == idFieldName).toList
    var setSql = setFields.map(f => s"${f._1} = ? ").mkString(",")
    if (entityInfo.nowByUpdateFieldNames.nonEmpty) {
      setSql += entityInfo.nowByUpdateFieldNames.map(i => s"$i = now() ").mkString(",", ",", "")
    }
    val updateR = JDBCProcessor.update(s"UPDATE $tableName SET $setSql WHERE $idFieldName = ?", setFields.map(_._2) :+ richValueInfo(idFieldName))
    if (updateR) {
      JDBCProcessor.get(s"SELECT * FROM $tableName WHERE $idFieldName  = ? ", List(idValue), clazz)
    } else {
      updateR
    }
  }

  def saveOrUpdate[M](entityInfo: EntityInfo, idValue: Any, valueInfo: Map[String, Any], clazz: Class[M]): Resp[M] = {
    val idFieldName = entityInfo.idFieldName
    if (!valueInfo.contains(idFieldName) || entityInfo.idStrategy == Id.STRATEGY_SEQ
      && valueInfo.contains(idFieldName) && valueInfo(idFieldName) == 0) {
      save(entityInfo, valueInfo, clazz)
    } else {
      update(entityInfo, valueInfo(idFieldName), valueInfo, clazz)
    }
  }

}
