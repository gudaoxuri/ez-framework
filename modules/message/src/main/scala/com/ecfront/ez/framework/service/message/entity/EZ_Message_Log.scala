package com.ecfront.ez.framework.service.message.entity

import java.util.Date

import com.ecfront.common.Resp
import com.ecfront.ez.framework.core.helper.TimeHelper
import com.ecfront.ez.framework.service.message.ServiceAdapter
import com.ecfront.ez.framework.service.storage.foundation._
import com.ecfront.ez.framework.service.storage.jdbc.JDBCBaseStorage
import com.ecfront.ez.framework.service.storage.mongo.MongoBaseStorage

import scala.beans.BeanProperty

@Entity("消息")
case class EZ_Message_Log() extends BaseModel {

  //@(QuerySqlField@field)(index = true)
  @Label("消息Id")
  @BeanProperty var message_id: String = _
  @Label("阅读账号")
  // 用于角色和公共消息
  @BeanProperty var read_account_code: String = _
  @Label("阅读时间,yyyyMMddHHmmss")
  @BeanProperty var read_time: Long = _

}

object EZ_Message_Log extends BaseStorageAdapter[EZ_Message_Log, EZ_Message_Log_Base] with EZ_Message_Log_Base {

  def apply(messageId: String, accountCode: String): EZ_Message_Log = {
    val log = EZ_Message_Log()
    log.message_id = messageId
    log.read_account_code = accountCode
    log
  }

  override protected val storageObj: EZ_Message_Log_Base =
    if (ServiceAdapter.mongoStorage) EZ_Message_Log_Mongo else EZ_Message_Log_JDBC

}

trait EZ_Message_Log_Base extends BaseStorage[EZ_Message_Log] {

  override def preSave(model: EZ_Message_Log, context: EZStorageContext): Resp[EZ_Message_Log] = {
    model.read_time = TimeHelper.sf.format(new Date()).toLong
    super.preSave(model, context)
  }

  override def preSaveOrUpdate(model: EZ_Message_Log, context: EZStorageContext): Resp[EZ_Message_Log] = {
    model.read_time = TimeHelper.sf.format(new Date()).toLong
    super.preSaveOrUpdate(model, context)
  }

}

object EZ_Message_Log_Mongo extends MongoBaseStorage[EZ_Message_Log] with EZ_Message_Log_Base

object EZ_Message_Log_JDBC extends JDBCBaseStorage[EZ_Message_Log] with EZ_Message_Log_Base