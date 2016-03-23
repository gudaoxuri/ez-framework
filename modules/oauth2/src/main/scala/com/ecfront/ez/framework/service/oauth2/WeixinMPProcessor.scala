package com.ecfront.ez.framework.service.oauth2

import com.ecfront.common.{JsonHelper, Resp}
import com.ecfront.ez.framework.service.auth.model.EZ_Account
import com.ecfront.ez.framework.service.rpc.http.HttpClientProcessor

object WeixinMPProcessor extends AppProcessor {

  override def fetchAccount(accessToken: AccessToken): Resp[EZ_Account] = {
    try {
      val resp = HttpClientProcessor.get(
        s"""
           |https://api.weixin.qq.com/sns/userinfo?access_token=${accessToken.access_token}&openid=${accessToken.openid}&lang=zh_CN
       """.stripMargin)
      val result = JsonHelper.toObject[Map[String, Any]](resp)
      val account = EZ_Account()
      account.name = result("nickname").asInstanceOf[String]
      account.image = result("headimgurl").asInstanceOf[String]
      account.oauth = Map(
        appName -> result("openid").asInstanceOf[String]
      )
      Resp.success(account)
    } catch {
      case e: Throwable =>
        logger.warn("Fetch account error." + e.getMessage, e)
        Resp.serverError("Fetch account error." + e.getMessage)
    }
  }

  override protected def packageFetchCodeParameters(): String = {
    super.packageFetchCodeParameters() + "#wechat_redirect"
  }

  override protected val fetchCodeBaseUrl: String = "https://open.weixin.qq.com/connect/oauth2/authorize"
  override protected val fetchAccessTokenBaseUrl: String = "https://api.weixin.qq.com/sns/oauth2/access_token"

  override protected val appName: String = "weixin_mp"

}
