package com.ecfront.ez.framework.storage

import com.ecfront.easybi.dbutils.exchange.{DB, DS}
import com.typesafe.scalalogging.slf4j.LazyLogging

import scala.collection.JavaConversions._

trait JDBCStorable[M <: AnyRef, Q <: AnyRef] extends Storable[M, Q] {

  override protected def __doGetById(id: String, request: Option[Q]): Option[M] = {
    val nId = if (__entityInfo.seqIdAnnotation == null) id else id.toLong
    __getByCondition(s"${Model.ID_FLAG} = ? ", Some(List(nId)), request)
  }

  /**
   * 解析SQL，目前只支持带简单order by 子句的SQL
   */
  private def __packageCondition(condition: String, parameters: Option[List[Any]], request: Option[Q]): (String, Array[Object]) = {
    val authInfo = __appendAuth(request)
    val allParameters = (parameters.getOrElse(List()) ++ authInfo._2).toArray.asInstanceOf[Array[Object]]
    val orderIndex = condition.toUpperCase.indexOf("ORDER BY")
    if (orderIndex != -1) {
      val whereCond = condition.substring(0, orderIndex)
      val orderCond = condition.substring(orderIndex)
      (whereCond + " " + authInfo._1 + " " + orderCond, allParameters)
    } else {
      (condition + " " + authInfo._1, allParameters)
    }
  }

  override protected def __doGetByCondition(condition: String, parameters: Option[List[Any]], request: Option[Q]): Option[M] = {
    val conditionWrap = __packageCondition(condition, parameters, request)
    val model = JDBCStorable.db.getObject(s"SELECT * FROM ${__tableName} WHERE " + conditionWrap._1, conditionWrap._2, __modelClazz)
    if (model != null) {
      __getRel(model, request)
    }
    Some(model)
  }

  private def __getRel(model: M, request: Option[Q]): Unit = {
    __entityInfo.manyToManyFields.filter(_._1.fetch).foreach {
      ann =>
        val relTableName = __getManyToManyRelTableName(ann._1.mapping, ann._1.master)
        val (masterFieldName, relFieldName) = __getManyToManyRelTableFields(ann._1.mapping, ann._1.master)
        val relEntityInfo = EntityContainer.CONTAINER(ann._1.mapping.toUpperCase())
        val attachSql = if (relEntityInfo.persistentFields.contains(StatusModel.ENABLE_FLAG)) " AND a.enable = true " else ""
        val value = ann._3 match {
          case f if classOf[Map[_, _]].isAssignableFrom(f) =>
            JDBCStorable.db.findObjects(
              s"SELECT a.* FROM ${ann._1.mapping} a,$relTableName b WHERE a.${Model.ID_FLAG} = b.$relFieldName AND b.$masterFieldName= ? $attachSql",
              Array[Object](__getIdValue(model)),
              relEntityInfo.clazz
            ).map(item => (__getIdValue(item.asInstanceOf[IdModel]), item)).toMap
          case f if classOf[Iterable[_]].isAssignableFrom(f) =>
            JDBCStorable.db.find(
              s"SELECT a.${Model.ID_FLAG} FROM ${ann._1.mapping} a,$relTableName b WHERE a.${Model.ID_FLAG} = b.$relFieldName AND b.$masterFieldName= ? $attachSql",
              Array[Object](__getIdValue(model))
            ).map(item => item(Model.ID_FLAG).asInstanceOf[String]).toList
        }
        __setValueByField(model, ann._2, value)
    }
    __entityInfo.oneToManyFields.filter(_._1.fetch).foreach {
      ann =>
        val relTableName = ann._1.mapping
        val relEntityInfo = EntityContainer.CONTAINER(relTableName.toUpperCase())
        val attachSql = if (relEntityInfo.persistentFields.contains(StatusModel.ENABLE_FLAG)) " AND enable = true " else ""
        val value = ann._3 match {
          case f if classOf[Map[_, _]].isAssignableFrom(f) =>
            JDBCStorable.db.findObjects(
              s"SELECT * FROM $relTableName WHERE ${ann._1.relField}= ? $attachSql ",
              Array[Object](__getIdValue(model)),
              relEntityInfo.clazz
            ).map(item => (__getIdValue(item.asInstanceOf[IdModel]), item)).toMap
          case f if classOf[Iterable[_]].isAssignableFrom(f) =>
            JDBCStorable.db.find(
              s"SELECT ${Model.ID_FLAG} FROM $relTableName WHERE ${ann._1.relField}= ? $attachSql ",
              Array[Object](__getIdValue(model))
            ).map(_.get(Model.ID_FLAG).asInstanceOf[String]).toList
        }
        __setValueByField(model, ann._2, value)
    }
  }

  override protected def __doFindAll(request: Option[Q]): Option[List[M]] = {
    __findByCondition(" 1=1 ", None, request)
  }

  override protected def __doFindAllEnable(request: Option[Q]): Option[List[M]] = {
    __findByCondition(" enable = true ", None, request)
  }

  override protected def __doFindAllDisable(request: Option[Q]): Option[List[M]] = {
    __findByCondition(" enable = false ", None, request)
  }

  override protected def __doFindByCondition(condition: String, parameters: Option[List[Any]], request: Option[Q]): Option[List[M]] = {
    val conditionWrap = __packageCondition(condition, parameters, request)
    Some(JDBCStorable.db.findObjects("SELECT * FROM " + __tableName + " WHERE " + conditionWrap._1, conditionWrap._2, __modelClazz).toList)
  }

  override protected def __doPageAll(pageNumber: Long, pageSize: Long, request: Option[Q]): Option[PageModel[M]] = {
    __pageByCondition(" 1=1 ", None, pageNumber, pageSize, request)
  }

  override protected def __doPageAllEnable(pageNumber: Long, pageSize: Long, request: Option[Q]): Option[PageModel[M]] = {
    __pageByCondition(" enable = true ", None, pageNumber, pageSize, request)
  }

  override protected def __doPageAllDisable(pageNumber: Long, pageSize: Long, request: Option[Q]): Option[PageModel[M]] = {
    __pageByCondition(" enable = false ", None, pageNumber, pageSize, request)
  }

  override protected def __doPageByCondition(condition: String, parameters: Option[List[Any]], pageNumber: Long, pageSize: Long, request: Option[Q]): Option[PageModel[M]] = {
    val conditionWrap = __packageCondition(condition, parameters, request)
    val page = JDBCStorable.db.findObjects("SELECT * FROM " + __tableName + " WHERE " + conditionWrap._1, conditionWrap._2, pageNumber, pageSize, __modelClazz)
    Some(PageModel(page.pageNumber, page.pageSize, page.pageTotal, page.recordTotal, page.objects.toList))
  }

  override protected def __doSave(model: M, request: Option[Q]): Option[String] = {
    JDBCStorable.db.open()
    val id = __saveWithoutTransaction(model, request)
    JDBCStorable.db.commit()
    id
  }

  override protected def __doSaveWithoutTransaction(model: M, request: Option[Q]): Option[String] = {
    JDBCStorable.db.save(__tableName, __getMapValue(model).asInstanceOf[Map[String, AnyRef]])
    val id = __getIdValue(model)
    __saveRel(id, model, request)
    Some(id)
  }

  private def __saveRel(mainId: String, model: M, request: Option[Q]): Unit = {
    __entityInfo.manyToManyFields.filter(_._1.master).foreach {
      ann =>
        val value = __getValueByField(model, ann._2)
        if (value != null) {
          val params = value match {
            case v: Map[_, _] =>
              v.map {
                item =>
                  Array(mainId, item._1.asInstanceOf[AnyRef])
              }
            case v: Iterable[_] =>
              v.map {
                item =>
                  Array(mainId, item.asInstanceOf[AnyRef])
              }
          }
          val (masterFieldName, relFieldName) = __getManyToManyRelTableFields(ann._1.mapping, ann._1.master)
          JDBCStorable.db.batch(s"INSERT INTO ${__getManyToManyRelTableName(ann._1.mapping, ann._1.master)}  ($masterFieldName,$relFieldName)  VALUES (?,?)", params.toArray)
        }
    }
  }

  override protected def __doUpdate(id: String, model: M, request: Option[Q]): Option[String] = {
    JDBCStorable.db.open()
    __updateWithoutTransaction(id, model, request)
    JDBCStorable.db.commit()
    Some(id)
  }

  override protected def __doUpdateWithoutTransaction(id: String, model: M, request: Option[Q]): Option[String] = {
    val authPass = __getById(id).get != null
    if (authPass) {
      JDBCStorable.db.update(__tableName, __convertIdToDB(id), __getMapValue(model).asInstanceOf[Map[String, AnyRef]])
      __updateRel(id, model, request)
    }
    Some(id)
  }

  private def __updateRel(mainId: String, model: M, request: Option[Q]): Unit = {
    __entityInfo.manyToManyFields.filter(_._1.master).foreach {
      ann =>
        val relTableName = __getManyToManyRelTableName(ann._1.mapping, ann._1.master)
        val (masterFieldName, relFieldName) = __getManyToManyRelTableFields(ann._1.mapping, ann._1.master)
        JDBCStorable.db.update(s"DELETE FROM $relTableName WHERE $masterFieldName = ? ", Array(mainId))
        val value = __getValueByField(model, ann._2)
        if (value != null) {
          val params = value match {
            case v: Map[_, _] =>
              v.map {
                item =>
                  Array(mainId, item._1.asInstanceOf[AnyRef])
              }
            case v: Iterable[_] =>
              v.map {
                item =>
                  Array(mainId, item.asInstanceOf[AnyRef])
              }
          }
          JDBCStorable.db.batch(s"INSERT INTO $relTableName  ($masterFieldName,$relFieldName)  VALUES (?,?)", params.toArray)
        }
    }
  }

  override protected def __doDeleteById(id: String, request: Option[Q]): Option[String] = {
    val nId = if (__entityInfo.seqIdAnnotation == null) id else id.toLong
    __deleteByCondition(s"${Model.ID_FLAG}  = ? ", Some(List(nId)), request)
    Some(id)
  }

  override protected def __doDeleteByIdWithoutTransaction(id: String, request: Option[Q]): Option[String] = {
    val nId = if (__entityInfo.seqIdAnnotation == null) id else id.toLong
    __deleteByConditionWithoutTransaction(s"${Model.ID_FLAG}  = ? ", Some(List(nId)), request)
    Some(id)
  }

  override protected def __doDeleteAll(request: Option[Q]): Option[List[String]] = {
    __deleteByCondition("1=1", None, request)
  }

  override protected def __doDeleteAllWithoutTransaction(request: Option[Q]): Option[List[String]] = {
    __deleteByConditionWithoutTransaction("1=1", None, request)
  }

  override protected def __doDeleteByCondition(condition: String, parameters: Option[List[Any]], request: Option[Q]): Option[List[String]] = {
    JDBCStorable.db.open()
    val res = __deleteByConditionWithoutTransaction(condition, parameters, request)
    JDBCStorable.db.commit()
    res
  }

  override protected def __doDeleteByConditionWithoutTransaction(condition: String, parameters: Option[List[Any]], request: Option[Q]): Option[List[String]] = {
    val conditionWrap = __packageCondition(condition, parameters, request)
    __deleteRel(conditionWrap._1, conditionWrap._2, request)
    JDBCStorable.db.update("DELETE FROM " + __tableName + " WHERE " + conditionWrap._1, conditionWrap._2)
    Some(List())
  }

  private def __deleteRel(condition: String, allParameters: Array[Object], request: Option[Q]): Unit = {
    __entityInfo.manyToManyFields.foreach {
      ann =>
        val relTableName = __getManyToManyRelTableName(ann._1.mapping, ann._1.master)
        val sql = if (condition == "1=1") {
          "DELETE FROM " + relTableName
        } else {
          s"DELETE FROM $relTableName WHERE" +
            s" ${__tableName + "_" + Model.ID_FLAG} in" +
            s" (SELECT ${Model.ID_FLAG} FROM ${__tableName} WHERE $condition)"
        }
        JDBCStorable.db.update(sql, allParameters)
    }
  }

  private def __getManyToManyRelTableName(tableName: String, isMaster: Boolean): String = {
    if (isMaster) {
      Model.REL_FLAG + "_" + __tableName + "_" + tableName
    } else {
      Model.REL_FLAG + "_" + tableName + "_" + __tableName
    }
  }

  private def __getManyToManyRelTableFields(tableName: String, isMaster: Boolean): (String, String) = {
    if (isMaster) {
      (__tableName + "_" + Model.ID_FLAG, tableName + "_" + Model.ID_FLAG)
    } else {
      (tableName + "_" + Model.ID_FLAG, __tableName + "_" + Model.ID_FLAG)
    }
  }

  override protected def __appendAuth(request: Option[Q]): (String, List[Any]) = ("", List())
}

object JDBCStorable extends LazyLogging {

  var db: DB = _

  def init(dbConfig: String): Unit = {
    DS.setConfigPath(dbConfig)
    db = new DB()
  }

}
