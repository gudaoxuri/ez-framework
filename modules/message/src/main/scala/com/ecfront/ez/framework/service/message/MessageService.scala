package com.ecfront.ez.framework.service.message

import java.util.Date

import com.ecfront.common.Resp
import com.ecfront.ez.framework.core.helper.TimeHelper
import com.ecfront.ez.framework.service.auth.EZAuthContext
import com.ecfront.ez.framework.service.message.entity.{EZ_Message, EZ_Message_Log}
import com.ecfront.ez.framework.service.message.helper.TemplateEngineHelper
import com.ecfront.ez.framework.service.rpc.foundation._
import com.ecfront.ez.framework.service.rpc.http.HTTP
import com.ecfront.ez.framework.service.storage.foundation.{EZStorageContext, Page}
import com.typesafe.scalalogging.slf4j.LazyLogging

/**
  * 消息服务
  */
@RPC("/message/")
@HTTP
object MessageService extends LazyLogging {

  /**
    * 根据登录信息获取未读消息条数
    *
    * @param parameter 参数
    * @param context   上下文
    * @return 未读消息条数
    */
  @GET("unRead/number/")
  def fetchUnReadMessageNumber(parameter: Map[String, String], context: EZAuthContext): Resp[Long] = {
    EZ_Message.fetchUnReadMessageNumber(context.loginInfo.get.account_code, context.loginInfo.get.role_codes, context)
  }

  /**
    * 根据登录信息获取未读消息
    *
    * @param parameter 参数，markRead=true时表示获取并标记为已读
    * @param context   上下文
    * @return 未读消息
    */
  @GET("unRead/")
  def fetchUnReadMessages(parameter: Map[String, String], context: EZAuthContext): Resp[List[EZ_Message]] = {
    val markRead = if (parameter.contains("markRead")) parameter("markRead").toBoolean else false
    EZ_Message.fetchUnReadMessages(context.loginInfo.get.account_code, context.loginInfo.get.role_codes, markRead, context)
  }

  /**
    * 根据登录信息分页获取已读消息
    *
    * @param parameter 参数
    * @param context   上下文
    * @return 已读消息
    */
  @GET("read/:pageNumber/:pageSize/")
  def fetchReadMessages(parameter: Map[String, String], context: EZAuthContext): Resp[Page[EZ_Message]] = {
    val pageNumber = if (parameter.contains("pageNumber")) parameter("pageNumber").toLong else 1L
    val pageSize = if (parameter.contains("pageSize")) parameter("pageSize").toInt else 10
    EZ_Message.fetchReadMessages(context.loginInfo.get.account_code, context.loginInfo.get.role_codes, pageNumber, pageSize, context)
  }

  /**
    * 标记消息已读
    *
    * @param parameter 参数
    * @param context   上下文
    * @return 已读消息
    */
  @GET(":messageId/markRead/")
  def markReadMessage(parameter: Map[String, String], context: EZAuthContext): Resp[Void] = {
    val messageId = parameter("messageId")
    EZ_Message_Log.save(EZ_Message_Log(messageId, context.loginInfo.get.account_code), context)
  }

  /**
    * 保存消息
    *
    * @param parameter 参数
    * @param body      消息体
    * @param context   上下文
    * @return 保存后的消息
    */
  @POST("")
  def addMessage(parameter: Map[String, String], body: EZ_Message, context: EZRPCContext): Resp[EZ_Message] = {
    EZ_Message.save(body, context)
  }

  /**
    * 更新消息
    *
    * @param parameter 参数
    * @param body      消息体
    * @param context   上下文
    * @return 更新后的消息
    */
  @PUT(":messageId/")
  def updateMessage(parameter: Map[String, String], body: EZ_Message, context: EZRPCContext): Resp[Void] = {
    body.id = parameter("messageId")
    EZ_Message.update(body, context)
  }

  /**
    * 删除消息
    *
    * @param parameter 参数
    * @param context   上下文
    * @return 是否成功
    */
  @DELETE(":messageId/")
  def deleteMessage(parameter: Map[String, String], context: EZRPCContext): Resp[Void] = {
    EZ_Message.deleteById(parameter("messageId"), context)
  }

  /**
    * 使用模板发送公共消息
    *
    * @param category     类型
    * @param level        级别
    * @param templateCode 模板编码
    * @param variable     模板变量
    * @param startTime    开始时间
    * @param endTime      结束时间
    * @param context      上下文
    * @return 是否成功
    */
  def sendToPublic(category: String, level: String, templateCode: String, variable: Map[String, String],
                   startTime: Date, endTime: Date, context: EZStorageContext): Resp[Void] = {
    val templateR = TemplateEngineHelper.generateMessageByTemplate(templateCode, variable)
    if (templateR) {
      sendToPublic(category, level, templateR.body._1, templateR.body._2, startTime, endTime, context)
    } else {
      templateR
    }
  }

  /**
    * 使用模板发送个人消息
    *
    * @param accountCode  账号编码
    * @param category     类型
    * @param level        级别
    * @param templateCode 模板编码
    * @param variable     模板变量
    * @param startTime    开始时间
    * @param endTime      结束时间
    * @param context      上下文
    * @return 是否成功
    */
  def sendToAccount(accountCode: String, category: String, level: String, templateCode: String, variable: Map[String, String],
                    startTime: Date, endTime: Date, context: EZStorageContext): Resp[Void] = {
    val templateR = TemplateEngineHelper.generateMessageByTemplate(templateCode, variable)
    if (templateR) {
      sendToAccount(accountCode, category, level, templateR.body._1, templateR.body._2, startTime, endTime, context)
    } else {
      templateR
    }
  }

  /**
    * 使用模板发送角色消息
    *
    * @param roleCode     角色编码
    * @param category     类型
    * @param level        级别
    * @param templateCode 模板编码
    * @param variable     模板变量
    * @param startTime    开始时间
    * @param endTime      结束时间
    * @param context      上下文
    * @return 是否成功
    */
  def sendToRole(roleCode: String, category: String, level: String, templateCode: String, variable: Map[String, String],
                 startTime: Date, endTime: Date, context: EZStorageContext): Resp[Void] = {
    val templateR = TemplateEngineHelper.generateMessageByTemplate(templateCode, variable)
    if (templateR) {
      sendToRole(roleCode, category, level, templateR.body._1, templateR.body._2, startTime, endTime, context)
    } else {
      templateR
    }
  }

  /**
    * 发送公共消息
    *
    * @param category  类型
    * @param level     级别
    * @param content   内容
    * @param title     标题
    * @param startTime 开始时间
    * @param endTime   结束时间
    * @param context   上下文
    * @return 是否成功
    */
  def sendToPublic(category: String, level: String, content: String, title: String,
                   startTime: Date, endTime: Date, context: EZStorageContext): Resp[Void] = {
    doAddMessage("", "", category, level, content, title, startTime, endTime, context)
  }

  /**
    * 发送个人消息
    *
    * @param accountCode 账号编码
    * @param category    类型
    * @param level       级别
    * @param content     内容
    * @param title       标题
    * @param startTime   开始时间
    * @param endTime     结束时间
    * @param context     上下文
    * @return 是否成功
    */
  def sendToAccount(accountCode: String, category: String, level: String, content: String, title: String,
                    startTime: Date, endTime: Date, context: EZStorageContext): Resp[Void] = {
    doAddMessage(accountCode, "", category, level, content, title, startTime, endTime, context)
  }

  /**
    * 发送角色消息
    *
    * @param roleCode  角色编码
    * @param category  类型
    * @param level     级别
    * @param content   内容
    * @param title     标题
    * @param startTime 开始时间
    * @param endTime   结束时间
    * @param context   上下文
    * @return 是否成功
    */
  def sendToRole(roleCode: String, category: String, level: String, content: String, title: String,
                 startTime: Date, endTime: Date, context: EZStorageContext): Resp[Void] = {
    doAddMessage("", roleCode, category, level, content, title, startTime, endTime, context)
  }

  private def doAddMessage(accountCode: String, roleCode: String, category: String, level: String, content: String, title: String,
                           startTime: Date, endTime: Date, context: EZStorageContext): Resp[Void] = {
    val message = EZ_Message()
    message.to_account = accountCode
    message.to_role = roleCode
    message.category = category
    message.level = level
    message.content = content
    message.title = title
    message.start_time = TimeHelper.sf.format(startTime).toLong
    message.end_time = TimeHelper.sf.format(endTime).toLong
    EZ_Message.save(message, context)
    Resp.success(null)
  }

}