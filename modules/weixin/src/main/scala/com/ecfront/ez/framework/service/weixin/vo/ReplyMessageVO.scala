package com.ecfront.ez.framework.service.weixin.vo

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.parser.Parser

abstract class BaseReplyMessageVO extends Serializable {
  def toDoc(vo: BaseRecMessageVO): Document
}

object BaseReplyMessageVO {
  def apply[R <: BaseRecMessageVO](recVo: R, msgType: String = null): Document = {
    val doc = Jsoup.parse("<xml></xml>", "", Parser.xmlParser())
    doc.getElementsByTag("xml").append(s"""<ToUserName><![CDATA[${recVo.fromUserName}]]></ToUserName>""")
    doc.getElementsByTag("xml").append(s"""<FromUserName><![CDATA[${recVo.toUserName}]]></FromUserName>""")
    doc.getElementsByTag("xml").append(s"""<CreateTime>${System.currentTimeMillis()}</CreateTime>""")
    doc.getElementsByTag("xml").append(s"""<MsgType>${if (msgType == null) recVo.msgType else msgType}</MsgType>""")
    doc
  }
}

case class TextReplyMsgVO() extends BaseReplyMessageVO {
  // 回复的消息内容（换行：在content中能够换行，微信客户端就支持换行显示）
  var content: String = _

  override def toDoc(vo: BaseRecMessageVO): Document = {
    val doc = BaseReplyMessageVO(vo)
    doc.getElementsByTag("xml").append(s"""<Content><![CDATA[$content]]></Content>""")
    doc
  }
}
