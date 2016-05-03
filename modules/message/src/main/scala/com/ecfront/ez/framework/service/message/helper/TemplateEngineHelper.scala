package com.ecfront.ez.framework.service.message.helper

import com.ecfront.common.{JsonHelper, Resp}
import com.ecfront.ez.framework.core.EZContext

import scala.beans.BeanProperty
import scala.io.Source

/**
  * Mustache模板引擎辅助类
  */
object TemplateEngineHelper {

  /**
    * 渲染模板，返回结果
    *
    * @param template 模板
    * @param variable 变量
    * @return 结果
    */
  def render(template: String, variable: Map[String, String]): String = {
    new Mustache(template).render(variable)
  }

  private var messageTemplatesCache = getMessageTemplates

  /**
    * 根据模板Code及变量渲染模板，返回结果
    *
    * @param templateCode 模板Code
    * @param variable     变量
    * @return 内容、标题
    */
  def generateMessageByTemplate(templateCode: String, variable: Map[String, String]): Resp[(String, String)] = {
    if (!messageTemplatesCache.contains(templateCode)) {
      // 缓存中不包含，重新获取一次
      messageTemplatesCache = getMessageTemplates
    }
    if (!messageTemplatesCache.contains(templateCode)) {
      // 重建缓存后还找不到视为错误code
      Resp.notFound("消息模板不存在")
    } else {
      val template = messageTemplatesCache(templateCode)
      val content = TemplateEngineHelper.render(template.content, variable)
      val title = TemplateEngineHelper.render(template.title, variable)
      Resp.success((content, title))
    }
  }


  private def getMessageTemplates: Map[String, MessageTemplateVO] = {
    // TODO  get by DB
    JsonHelper.toObject[List[MessageTemplateVO]](Source.fromFile(EZContext.confPath + "message_template.json", "UTF-8").mkString).map {
      item =>
        item.templateCode -> item
    }.toMap
  }

  class MessageTemplateVO {
    @BeanProperty var templateCode: String = _
    @BeanProperty var templateName: String = _
    @BeanProperty var title: String = _
    @BeanProperty var content: String = _
  }

}
