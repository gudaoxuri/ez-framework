package com.ecfront.ez.framework.service.oauth2

import com.ecfront.common.Resp
import com.ecfront.ez.framework.core.EZContext
import com.ecfront.ez.framework.service.auth.EZ_Account
import com.typesafe.scalalogging.slf4j.LazyLogging
import io.vertx.core.json.JsonObject
import io.vertx.core.{AsyncResult, Handler}
import io.vertx.ext.auth.oauth2.{AccessToken, OAuth2Auth, OAuth2FlowType}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Promise}

trait AppProcessor extends LazyLogging {

  var oauth2: OAuth2Auth = _
  var config: JsonObject = _

  def init(_config: JsonObject): Unit = {
    config = _config
    config.put("site", site)
      .put("tokenPath", accessTokenPath)
      .put("authorizationPath", authorizationPath)
    oauth2 = OAuth2Auth.create(EZContext.vertx, flowType, config)
  }

  def getCode: String = {
    oauth2.authorizeURL(new JsonObject()
      // fix weixin use appid instead of client_id
      .put("appid", config.getString("clientID"))
      .put("redirect_uri", com.ecfront.ez.framework.service.rpc.http.ServiceAdapter.publicUrl + "public/oauth2/callback/" + appName + "/")
      .put("scope", scope)
      .put("state", System.nanoTime() + ""))
  }

  def getToken(code: String, state: String): Resp[AccessToken] = {
    val p = Promise[Resp[AccessToken]]()
    //TODO csrf
    oauth2.getToken(new JsonObject()
      // fix weixin use appid instead of client_id
      .put("appid", config.getString("clientID"))
      // fix weixin use secret instead of clientSecret
      .put("secret", config.getString("clientSecret"))
      .put("code", code), new Handler[AsyncResult[AccessToken]] {
      override def handle(e: AsyncResult[AccessToken]): Unit = {
        if (e.failed()) {
          p.success(Resp.serverError("Fetch token error : " + e.cause().getMessage))
        } else {
          p.success(Resp.success(e.result()))
        }
      }
    })
    Await.result(p.future,Duration.Inf)
  }

  def getAccount(accessToken: AccessToken): Resp[EZ_Account]


  protected def appName: String

  protected def site: String

  protected def authorizationPath: String

  protected def accessTokenPath: String

  protected def refreshTokenPath: String

  protected def scope: String

  protected def flowType: OAuth2FlowType = OAuth2FlowType.AUTH_CODE

}

