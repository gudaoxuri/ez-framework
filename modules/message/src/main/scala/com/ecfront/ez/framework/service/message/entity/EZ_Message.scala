package com.ecfront.ez.framework.service.message.entity

import java.util.Date

import com.ecfront.common.Resp
import com.ecfront.ez.framework.core.helper.TimeHelper
import com.ecfront.ez.framework.service.message.ServiceAdapter
import com.ecfront.ez.framework.service.storage.foundation._
import com.ecfront.ez.framework.service.storage.jdbc.{JDBCProcessor, JDBCSecureStorage}
import com.ecfront.ez.framework.service.storage.mongo.MongoSecureStorage

import scala.beans.BeanProperty

@Entity("消息")
case class EZ_Message() extends SecureModel {

  //@(QuerySqlField@field)(index = true)
  @Label("个人消息")
  @BeanProperty var to_account: String = _
  //@(QuerySqlField@field)(index = true)
  @Label("角色消息")
  @BeanProperty var to_role: String = _
  @Label("类型")
  @BeanProperty var category: String = _
  @Label("级别")
  @BeanProperty var level: String = _
  @Label("模板Code")
  @BeanProperty var template_code: String = _
  @Label("内容")
  @BeanProperty var content: String = _
  @Label("标题")
  @BeanProperty var title: String = _
  @Label("开始时间,yyyyMMddHHmmss")
  @BeanProperty var start_time: Long = _
  @Label("结束时间,yyyyMMddHHmmss")
  @BeanProperty var end_time: Long = _

}

object EZ_Message extends SecureStorageAdapter[EZ_Message, EZ_Message_Base] with EZ_Message_Base {

  //val cache = IgniteHelper.ignite.cache("ez.message")

  override protected val storageObj: EZ_Message_Base =
    if (ServiceAdapter.mongoStorage) EZ_Message_Mongo else EZ_Message_JDBC

  override def fetchReadMessages(accountCode: String, roleCodes: List[String],
                                 pageNumber: Long, pageSize: Int, context: EZStorageContext): Resp[Page[EZ_Message]] =
    storageObj.fetchReadMessages(accountCode, roleCodes, pageNumber, pageSize, context)

  def fetchUnReadMessages(accountCode: String, roleCodes: List[String], markRead: Boolean, context: EZStorageContext): Resp[List[EZ_Message]] =
    storageObj.fetchUnReadMessages(accountCode, roleCodes, markRead, context)

  def fetchUnReadMessageNumber(accountCode: String, roleCodes: List[String], context: EZStorageContext): Resp[Long] =
    storageObj.fetchUnReadMessageNumber(accountCode, roleCodes, context)

}

trait EZ_Message_Base extends SecureStorage[EZ_Message] {

  override def preSave(model: EZ_Message, context: EZStorageContext): Resp[EZ_Message] = {
    preSaveOrUpdate(model, context)
  }

  override def preUpdate(model: EZ_Message, context: EZStorageContext): Resp[EZ_Message] = {
    preSaveOrUpdate(model, context)
  }

  override def preSaveOrUpdate(model: EZ_Message, context: EZStorageContext): Resp[EZ_Message] = {
    if (model.to_account == null) {
      model.to_account = ""
    }
    if (model.to_role == null) {
      model.to_role = ""
    }
    if (model.title == null) {
      model.title = ""
    }
    if (model.template_id == null) {
      model.template_id = ""
    }
    super.preSaveOrUpdate(model, context)
  }

  def fetchReadMessages(accountCode: String, roleCodes: List[String], pageNumber: Long, pageSize: Int, context: EZStorageContext): Resp[Page[EZ_Message]]

  def fetchUnReadMessages(accountCode: String, roleCodes: List[String], markRead: Boolean, context: EZStorageContext): Resp[List[EZ_Message]]

  def fetchUnReadMessageNumber(accountCode: String, roleCodes: List[String], context: EZStorageContext): Resp[Long]

}

object EZ_Message_Mongo extends MongoSecureStorage[EZ_Message] with EZ_Message_Base {

  // TODO
  override def fetchReadMessages(accountCode: String, roleCodes: List[String],
                                 pageNumber: Long, pageSize: Int, context: EZStorageContext): Resp[Page[EZ_Message]] = ???

  def fetchUnReadMessages(accountCode: String, roleCodes: List[String], markRead: Boolean, context: EZStorageContext): Resp[List[EZ_Message]] = ???

  def fetchUnReadMessageNumber(accountCode: String, roleCodes: List[String], context: EZStorageContext): Resp[Long] = ???

}

object EZ_Message_JDBC extends JDBCSecureStorage[EZ_Message] with EZ_Message_Base {

  def fetchUnReadMessages(accountCode: String, roleCodes: List[String], markRead: Boolean, context: EZStorageContext): Resp[List[EZ_Message]] = {
    val now = TimeHelper.sf.format(new Date()).toLong
    val result = JDBCProcessor.find(
      s"""
         |SELECT msg.* FROM $tableName msg
         | LEFT JOIN ${EZ_Message_Log.tableName} log ON msg.id = log.message_id AND log.read_account_code = ?
         | WHERE
         |  ((msg.to_account = ? OR msg.to_role IN (${roleCodes.map(_ => "?").mkString(",")})) OR (msg.to_account = '' AND msg.to_role =''))
         |   AND start_time <= ? AND end_time >= ? AND log.id IS NULL
       """.stripMargin, accountCode :: accountCode :: roleCodes ++ List(now, now), classOf[EZ_Message])
    if (markRead) {
      result.body.foreach {
        item =>
          EZ_Message_Log.save(EZ_Message_Log(item.id, accountCode), context)
      }
    }
    result
  }

  def fetchUnReadMessageNumber(accountCode: String, roleCodes: List[String], context: EZStorageContext): Resp[Long] = {
    val now = TimeHelper.sf.format(new Date()).toLong
    JDBCProcessor.count(
      s"""
         |SELECT msg.* FROM $tableName msg
         | LEFT JOIN ${EZ_Message_Log.tableName} log ON msg.id = log.message_id AND log.read_account_code = ?
         | WHERE
         |  ((msg.to_account = ? OR msg.to_role IN (${roleCodes.map(_ => "?").mkString(",")})) OR (msg.to_account = '' AND msg.to_role =''))
         |   AND start_time <= ? AND end_time >= ? AND log.id IS NULL
       """.stripMargin, accountCode :: accountCode :: roleCodes ++ List(now, now))
  }

  override def fetchReadMessages(accountCode: String, roleCodes: List[String],
                                 pageNumber: Long, pageSize: Int, context: EZStorageContext): Resp[Page[EZ_Message]] = {
    val now = TimeHelper.sf.format(new Date()).toLong
    JDBCProcessor.page(
      s"""
         |SELECT msg.* FROM $tableName msg
         | LEFT JOIN ${EZ_Message_Log.tableName} log ON msg.id = log.message_id AND log.read_account_code = ?
         | WHERE
         |  ((msg.to_account = ? OR msg.to_role IN (${roleCodes.map(_ => "?").mkString(",")})) OR (msg.to_account = '' AND msg.to_role =''))
         |   AND start_time <= ? AND end_time >= ? AND log.id IS NOT NULL
       """.stripMargin, accountCode :: accountCode :: roleCodes ++ List(now, now), pageNumber, pageSize, classOf[EZ_Message])
  }


}
