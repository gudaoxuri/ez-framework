package com.ecfront.ez.framework.service.weixin.api

import com.ecfront.common.{EncryptHelper, Resp}
import com.ecfront.ez.framework.service.auth.EZAuthContext
import com.ecfront.ez.framework.service.rpc.foundation.{GET, POST, RPC, Raw}
import com.ecfront.ez.framework.service.rpc.http.HTTP
import com.ecfront.ez.framework.service.weixin.vo.TextRecMsgVO
import com.ecfront.ez.framework.service.weixin.{ServiceAdapter, Weixin}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

@RPC("/public/weixin/message/")
@HTTP
object MessageProcessor extends BaseProcessor {

  @GET("listening/")
  def listeningCheck(parameter: Map[String, String], context: EZAuthContext): Resp[Raw] = {
    if (validity(parameter("signature"), parameter("timestamp"), parameter("nonce"))) {
      Resp.success(Raw(parameter("echostr")))
    } else {
      Resp.success(Raw(""))
    }
  }

  @POST("listening/")
  def listening(parameter: Map[String, String], body: Document, context: EZAuthContext): Resp[Document] = {
    val startTime = System.currentTimeMillis()
    val reply = body.getElementsByTag("MsgType").text() match {
      case "text" => processTextMsg(body)
    }
    val endTime = System.currentTimeMillis()
    if (endTime - startTime > 5000) {
      logger.warn(s"message process timeout [${endTime - startTime}] : ${body.outerHtml()}")
    }
    if (reply != null) {
      Resp.success(reply)
    } else {
      Resp.success(Jsoup.parse(""))
    }
  }

  def validity(signature: String, timestamp: String, nonce: String): Boolean = {
    val token = ServiceAdapter.messageToken
    EncryptHelper.encrypt(Set(token, timestamp, nonce).mkString(""), "SHA1") == signature
  }

  private def processTextMsg(body: Document): Document = {
    val vo = TextRecMsgVO(body)
    Weixin.messageService.processTextMsg(vo).toDoc(vo)
  }

}
