package com.ecfront.ez.framework.service.oauth2

import com.ecfront.common.Resp
import com.ecfront.ez.framework.service.auth.EZ_Account
import io.vertx.core.http.HttpMethod
import io.vertx.core.json.JsonObject
import io.vertx.core.{AsyncResult, Handler}
import io.vertx.ext.auth.oauth2.{AccessToken, OAuth2FlowType}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Promise}

object WeixinOpenProcessor extends AppProcessor {

  override def getAccount(accessToken: AccessToken): Resp[EZ_Account] = {
    val p = Promise[Resp[EZ_Account]]()
    oauth2.api(HttpMethod.GET, "/userinfo",
      new JsonObject()
        .put("access_token", accessToken.principal().getString("access_token"))
        .put("openid", accessToken.principal().getString("openid"))
      , new Handler[AsyncResult[JsonObject]] {
        override def handle(e: AsyncResult[JsonObject]): Unit = {
          if (e.failed()) {
            p.success(Resp.serverError("Fetch user info error :" + e.cause().getMessage))
          } else {
            val result = e.result()
            val account = EZ_Account()
            account.name = result.getString("nickname")
            account.image = result.getString("headimgurl")
            account.oauth = Map(
              appName -> result.getString("openid")
            )
            p.success(Resp.success(account))
          }
        }
      })
    Await.result(p.future, Duration.Inf)
  }

  override def site: String = ""

  override def authorizationPath: String = "https://open.weixin.qq.com/connect/qrconnect"

  override def accessTokenPath: String = "https://api.weixin.qq.com/sns/oauth2/access_token"

  override def refreshTokenPath: String = "https://api.weixin.qq.com/sns/oauth2/refresh_token"

  override def scope: String = "snsapi_login"

  override protected def appName: String = "weixin_open"

  override def flowType: OAuth2FlowType = OAuth2FlowType.AUTH_CODE
}
