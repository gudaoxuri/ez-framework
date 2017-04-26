package com.ecfront.ez.framework.service.message.entity

import java.util.Date

import com.ecfront.common.Resp
import com.ecfront.ez.framework.core.helper.TimeHelper
import com.ecfront.ez.framework.service.jdbc.{BaseModel, BaseStorage, Desc, Entity}

import scala.beans.BeanProperty

@Entity("消息日志")
case class EZ_Message_Log() extends BaseModel {

  @Desc("消息Id", 200, 0)
  @BeanProperty var message_id: String = _
  @Desc("阅读账号", 200, 0)
  // 用于角色和公共消息
  @BeanProperty var read_account_code: String = _
  @Desc("阅读时间,yyyyMMddHHmmss", 0, 0)
  @BeanProperty var read_time: Long = _

}

object EZ_Message_Log extends BaseStorage[EZ_Message_Log] {

  def apply(messageId: String, accountCode: String): EZ_Message_Log = {
    val log = EZ_Message_Log()
    log.message_id = messageId
    log.read_account_code = accountCode
    log
  }

  override def preSave(model: EZ_Message_Log): Resp[EZ_Message_Log] = {
    model.read_time = TimeHelper.sf.format(new Date()).toLong
    super.preSave(model)
  }

  override def preSaveOrUpdate(model: EZ_Message_Log): Resp[EZ_Message_Log] = {
    model.read_time = TimeHelper.sf.format(new Date()).toLong
    super.preSaveOrUpdate(model)
  }

}