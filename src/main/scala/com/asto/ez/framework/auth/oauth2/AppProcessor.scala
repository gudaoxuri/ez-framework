package com.asto.ez.framework.auth.oauth2

import com.asto.ez.framework.EZGlobal
import com.asto.ez.framework.auth.EZ_Account
import com.ecfront.common.Resp
import com.typesafe.scalalogging.slf4j.LazyLogging
import io.vertx.core.json.JsonObject
import io.vertx.core.{AsyncResult, Handler}
import io.vertx.ext.auth.oauth2.{AccessToken, OAuth2Auth, OAuth2FlowType}

import scala.concurrent.{Future, Promise}

trait AppProcessor extends LazyLogging {

  var oauth2: OAuth2Auth = _
  var config: JsonObject = _

  def init(_config: JsonObject) = {
    config = _config
    config.put("site", site)
      .put("tokenPath", accessTokenPath)
      .put("authorizationPath", authorizationPath)
    oauth2 = OAuth2Auth.create(EZGlobal.vertx, flowType, config)
  }

  def getCode: String = {
    oauth2.authorizeURL(new JsonObject()
      //fix weixin use appid instead of client_id
      .put("appid", config.getString("clientID"))
      .put("redirect_uri", EZGlobal.ez_rpc_http_public_url + "public/oauth2/callback/" + appName + "/")
      .put("scope", scope)
      .put("state", System.nanoTime() + ""))
  }

  def getToken(code: String, state: String): Future[Resp[AccessToken]] = {
    val p = Promise[Resp[AccessToken]]()
    //TODO csrf
    oauth2.getToken(new JsonObject()
      //fix weixin use appid instead of client_id
      .put("appid", config.getString("clientID"))
      //fix weixin use secret instead of clientSecret
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
    p.future
  }

  def getAccount(accessToken: AccessToken): Future[Resp[EZ_Account]]


  protected def appName: String

  protected def site: String

  protected def authorizationPath: String

  protected def accessTokenPath: String

  protected def refreshTokenPath: String

  protected def scope: String

  protected def flowType: OAuth2FlowType = OAuth2FlowType.AUTH_CODE

}

