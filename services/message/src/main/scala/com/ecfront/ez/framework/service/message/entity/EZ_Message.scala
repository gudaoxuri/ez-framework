package com.ecfront.ez.framework.service.message.entity

import java.util.Date

import com.ecfront.common.Resp
import com.ecfront.ez.framework.core.helper.TimeHelper
import com.ecfront.ez.framework.service.jdbc._

import scala.beans.BeanProperty

@Entity("消息")
case class EZ_Message() extends SecureModel {

  @Desc("个人消息",200,0)
  @BeanProperty var to_account: String = _
  @Desc("角色消息",200,0)
  @BeanProperty var to_role: String = _
  @Desc("类型",200,0)
  @BeanProperty var category: String = _
  @Desc("级别",200,0)
  @BeanProperty var level: String = _
  @Desc("模板Code",200,0)
  @BeanProperty var template_code: String = _
  @Desc("内容",0,0)
  @BeanProperty var content: String = _
  @Desc("标题",500,0)
  @BeanProperty var title: String = _
  @Desc("开始时间,yyyyMMddHHmmss",0,0)
  @BeanProperty var start_time: Long = _
  @Desc("结束时间,yyyyMMddHHmmss",0,0)
  @BeanProperty var end_time: Long = _

}

object EZ_Message extends SecureStorage[EZ_Message] {


  override def preSave(model: EZ_Message): Resp[EZ_Message] = {
    preSaveOrUpdate(model)
  }

  override def preUpdate(model: EZ_Message): Resp[EZ_Message] = {
    preSaveOrUpdate(model)
  }

  override def preSaveOrUpdate(model: EZ_Message): Resp[EZ_Message] = {
    if (model.to_account == null) {
      model.to_account = ""
    }
    if (model.to_role == null) {
      model.to_role = ""
    }
    if (model.title == null) {
      model.title = ""
    }
    if (model.template_code == null) {
      model.template_code = ""
    }
    super.preSaveOrUpdate(model)
  }

  def fetchUnReadMessages(accountCode: String, roleCodes: Set[String], markRead: Boolean): Resp[List[EZ_Message]] = {
    val now = TimeHelper.sf.format(new Date()).toLong
    val result = JDBCProcessor.find(
      s"""
         |SELECT msg.* FROM $tableName msg
         | LEFT JOIN ${EZ_Message_Log.tableName} log ON msg.id = log.message_id AND log.read_account_code = ?
         | WHERE
         |  ((msg.to_account = ? OR msg.to_role IN (${roleCodes.map(_ => "?").mkString(",")})) OR (msg.to_account = '' AND msg.to_role =''))
         |   AND msg.start_time <= ? AND msg.end_time >= ? AND log.id IS NULL ORDER BY msg.update_time DESC
       """.stripMargin, accountCode :: accountCode :: roleCodes.toList ++ List(now, now), classOf[EZ_Message])
    if (markRead) {
      result.body.foreach {
        item =>
          EZ_Message_Log.save(EZ_Message_Log(item.id, accountCode))
      }
    }
    result
  }

  def fetchUnReadMessageNumber(accountCode: String, roleCodes: Set[String]): Resp[Long] = {
    val now = TimeHelper.sf.format(new Date()).toLong
    JDBCProcessor.count(
      s"""
         |SELECT msg.* FROM $tableName msg
         | LEFT JOIN ${EZ_Message_Log.tableName} log ON msg.id = log.message_id AND log.read_account_code = ?
         | WHERE
         |  ((msg.to_account = ? OR msg.to_role IN (${roleCodes.map(_ => "?").mkString(",")})) OR (msg.to_account = '' AND msg.to_role =''))
         |   AND msg.start_time <= ? AND msg.end_time >= ? AND log.id IS NULL ORDER BY msg.update_time DESC
       """.stripMargin, accountCode :: accountCode :: roleCodes.toList ++ List(now, now))
  }

  def fetchReadMessages(accountCode: String, roleCodes: Set[String],
                        pageNumber: Long, pageSize: Int): Resp[Page[EZ_Message]] = {
    val now = TimeHelper.sf.format(new Date()).toLong
    JDBCProcessor.page(
      s"""
         |SELECT msg.* FROM $tableName msg
         | LEFT JOIN ${EZ_Message_Log.tableName} log ON msg.id = log.message_id AND log.read_account_code = ?
         | WHERE
         |  ((msg.to_account = ? OR msg.to_role IN (${roleCodes.map(_ => "?").mkString(",")})) OR (msg.to_account = '' AND msg.to_role =''))
         |   AND msg.start_time <= ? AND msg.end_time >= ? AND log.id IS NOT NULL ORDER BY msg.update_time DESC
       """.stripMargin, accountCode :: accountCode :: roleCodes.toList ++ List(now, now), pageNumber, pageSize, classOf[EZ_Message])
  }

}
