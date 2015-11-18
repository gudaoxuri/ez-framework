package com.asto.ez.framework.storage.jdbc

import java.util.Date

import com.asto.ez.framework.EZContext
import com.asto.ez.framework.helper.{DBHelper, Page}
import com.asto.ez.framework.storage.jdbc.EntityContainer.EntityInfo
import com.ecfront.common.Resp

import scala.concurrent.Future

object DBExecutor {

  def save(entityInfo: EntityInfo, context:EZContext, valueInfos: Map[String, Any]): Future[Resp[Void]] = {
    val tableName = entityInfo.tableName
    val idFieldName = entityInfo.idFieldName
    val clazz = entityInfo.clazz
    val richValueInfos=collection.mutable.Map[String,Any]()
    richValueInfos++=valueInfos
    if(classOf[SecureModel].isAssignableFrom(clazz)){
      val now=SecureModel.df.format(new Date()).toLong
      if(!valueInfos.contains(SecureModel.CREATE_USER_FLAG)||valueInfos(SecureModel.CREATE_USER_FLAG)==null){
        richValueInfos+= SecureModel.CREATE_USER_FLAG -> context.userId
      }
      if(!valueInfos.contains(SecureModel.CREATE_TIME_FLAG)||valueInfos(SecureModel.CREATE_TIME_FLAG)==0){
        richValueInfos+= SecureModel.CREATE_TIME_FLAG -> now
      }
      if(!valueInfos.contains(SecureModel.CREATE_ORG_FLAG)||valueInfos(SecureModel.CREATE_ORG_FLAG)==null){
        richValueInfos+= SecureModel.CREATE_ORG_FLAG -> context.orgId
      }
      if(!valueInfos.contains(SecureModel.UPDATE_USER_FLAG)||valueInfos(SecureModel.UPDATE_USER_FLAG)==null){
        richValueInfos+= SecureModel.UPDATE_USER_FLAG -> context.userId
      }
      if(!valueInfos.contains(SecureModel.UPDATE_TIME_FLAG)||valueInfos(SecureModel.UPDATE_TIME_FLAG)==0){
        richValueInfos+= SecureModel.UPDATE_TIME_FLAG -> now
      }
      if(!valueInfos.contains(SecureModel.UPDATE_ORG_FLAG)||valueInfos(SecureModel.UPDATE_ORG_FLAG)==null){
        richValueInfos+= SecureModel.UPDATE_ORG_FLAG -> context.orgId
      }
    }
    if(classOf[StatusModel].isAssignableFrom(clazz)){
      if(!valueInfos.contains(StatusModel.ENABLE_FLAG)||valueInfos(StatusModel.ENABLE_FLAG)==null){
        richValueInfos+= StatusModel.ENABLE_FLAG -> false
      }
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
      DBHelper.update(sql, richValueInfos.values.toList ++ List(idValue))
    } else {
      val sql =
        s"""
           |INSERT INTO $tableName
           | (${richValueInfos.keys.mkString(",")})
           | VALUES ( ${(for (i <- 0 until richValueInfos.size) yield "?").mkString(",")} )
       """.stripMargin
      DBHelper.update(sql, richValueInfos.values.toList)
    }
  }

  def update(entityInfo: EntityInfo, context:EZContext, idValue: Any, valueInfos: Map[String, Any]): Future[Resp[Void]] = {
    val idFieldName = entityInfo.idFieldName
    val clazz = entityInfo.clazz
    val richValueInfos=collection.mutable.Map[String,Any]()
    richValueInfos++=valueInfos.filterNot(_._1==entityInfo.idFieldName)
    if(classOf[SecureModel].isAssignableFrom(clazz)){
      val now=SecureModel.df.format(new Date()).toLong
      if(!valueInfos.contains(SecureModel.UPDATE_USER_FLAG)||valueInfos(SecureModel.UPDATE_USER_FLAG)==null){
        richValueInfos+= SecureModel.UPDATE_USER_FLAG -> context.userId
      }
      if(!valueInfos.contains(SecureModel.UPDATE_TIME_FLAG)||valueInfos(SecureModel.UPDATE_TIME_FLAG)==0){
        richValueInfos+= SecureModel.UPDATE_TIME_FLAG -> now
      }
      if(!valueInfos.contains(SecureModel.UPDATE_ORG_FLAG)||valueInfos(SecureModel.UPDATE_ORG_FLAG)==null){
        richValueInfos+= SecureModel.UPDATE_ORG_FLAG -> context.orgId
      }
    }
    //keys.toList.map ，toList 让map有序
    val newValues = richValueInfos.keys.toList.map(key => s"$key = ? ").mkString(",")
    val condition = s" $idFieldName = ? "
    update(entityInfo, context,newValues, condition, richValueInfos.values.toList ++ List(idValue))
  }

  def saveOrUpdate(entityInfo: EntityInfo, context:EZContext, idValue: Any, valueInfos: Map[String, Any]): Future[Resp[Void]] = {
    val tableName = entityInfo.tableName
    val idFieldName = entityInfo.idFieldName
    val idValue = if (valueInfos.contains(idFieldName)) valueInfos(idFieldName) else null
    if (idValue != null) {
      val clazz = entityInfo.clazz
      val richValueInfos=collection.mutable.Map[String,Any]()
      richValueInfos++=valueInfos.filterNot(_._1==entityInfo.idFieldName)
      if(classOf[SecureModel].isAssignableFrom(clazz)){
        val now=SecureModel.df.format(new Date()).toLong
        if(!valueInfos.contains(SecureModel.UPDATE_USER_FLAG)||valueInfos(SecureModel.UPDATE_USER_FLAG)==null){
          richValueInfos+= SecureModel.UPDATE_USER_FLAG -> context.userId
        }
        if(!valueInfos.contains(SecureModel.UPDATE_TIME_FLAG)||valueInfos(SecureModel.UPDATE_TIME_FLAG)==0){
          richValueInfos+= SecureModel.UPDATE_TIME_FLAG -> now
        }
        if(!valueInfos.contains(SecureModel.UPDATE_ORG_FLAG)||valueInfos(SecureModel.UPDATE_ORG_FLAG)==null){
          richValueInfos+= SecureModel.UPDATE_ORG_FLAG -> context.orgId
        }
      }
      val sql =
        s"""
           |INSERT INTO $tableName
           | (${richValueInfos.keys.toList.mkString(",")})
           | VALUES ( ${(for (i <- 0 until richValueInfos.size) yield "?").mkString(",")} )
           | ON DUPLICATE KEY UPDATE
           | ${richValueInfos.keys.filterNot(_ == idFieldName).toList.map(key => s"$key = VALUES($key)").mkString(",")}
       """.stripMargin
      DBHelper.update(sql, richValueInfos.values.toList)
    } else {
      save(entityInfo,context, valueInfos)
    }
  }

  def update(entityInfo: EntityInfo, context:EZContext, newValues: String, condition: String, parameters: List[Any]): Future[Resp[Void]] = {
    val tableName = entityInfo.tableName
    DBHelper.update(
      s"UPDATE $tableName Set $newValues WHERE $condition",
      parameters
    )
  }

  def delete(entityInfo: EntityInfo, context:EZContext, idValue: Any): Future[Resp[Void]] = {
    val tableName = entityInfo.tableName
    val idFieldName = entityInfo.idFieldName
    DBHelper.update(
      s"DELETE FROM $tableName WHERE $idFieldName = ? ",
      List(idValue)
    )
  }

  def delete(entityInfo: EntityInfo, context:EZContext, condition: String, parameters: List[Any]): Future[Resp[Void]] = {
    val tableName = entityInfo.tableName
    DBHelper.update(
      s"DELETE FROM $tableName WHERE $condition ",
      parameters
    )
  }

  def get[E](entityInfo: EntityInfo, context:EZContext, idValue: Any): Future[Resp[E]] = {
    val tableName = entityInfo.tableName
    val idFieldName = entityInfo.idFieldName
    val clazz = entityInfo.clazz.asInstanceOf[Class[E]]
    DBHelper.get(
      s"SELECT * FROM $tableName WHERE $idFieldName  = ? ",
      List(idValue),
      clazz
    )
  }

  def get[E](entityInfo: EntityInfo, context:EZContext, condition: String, parameters: List[Any]): Future[Resp[E]] = {
    val tableName = entityInfo.tableName
    val clazz = entityInfo.clazz.asInstanceOf[Class[E]]
    DBHelper.get(
      s"SELECT * FROM $tableName WHERE $condition ",
      parameters,
      clazz
    )
  }

  def exist(entityInfo: EntityInfo, context:EZContext, condition: String, parameters: List[Any]): Future[Resp[Boolean]] = {
    val tableName = entityInfo.tableName
    DBHelper.exist(
      s"SELECT 1 FROM $tableName WHERE $condition ",
      parameters
    )
  }

  def find[E](entityInfo: EntityInfo, context:EZContext, condition: String = " 1=1 ", parameters: List[Any] = List()): Future[Resp[List[E]]] = {
    val tableName = entityInfo.tableName
    val clazz = entityInfo.clazz.asInstanceOf[Class[E]]
    DBHelper.find(
      s"SELECT * FROM $tableName WHERE $condition ",
      parameters,
      clazz
    )
  }

  def page[E](entityInfo: EntityInfo, context:EZContext, condition: String = " 1=1 ", parameters: List[Any] = List(), pageNumber: Long = 1, pageSize: Int = 10): Future[Resp[Page[E]]] = {
    val tableName = entityInfo.tableName
    val clazz = entityInfo.clazz.asInstanceOf[Class[E]]
    DBHelper.page(
      s"SELECT * FROM $tableName WHERE $condition ",
      parameters,
      pageNumber, pageSize,
      clazz
    )
  }

  def count(entityInfo: EntityInfo, context:EZContext, condition: String = " 1=1 ", parameters: List[Any] = List()): Future[Resp[Long]] = {
    val tableName = entityInfo.tableName
    DBHelper.count(
      s"SELECT count(1) FROM $tableName WHERE $condition ",
      parameters
    )
  }

}
