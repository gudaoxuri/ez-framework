package com.ecfront.ez.framework.service.oauth2

import com.ecfront.common.{JsonHelper, Resp}
import com.ecfront.ez.framework.service.auth.model.EZ_Account
import com.ecfront.ez.framework.service.rpc.http.HttpClientProcessor
import com.typesafe.scalalogging.slf4j.LazyLogging
import io.vertx.core.json.JsonObject

/**
  * Oauth2处理
  */
trait AppProcessor extends LazyLogging {

  /**
    * 初始化调用，设置 appId 和 secret
    *
    * @param config 配置信息
    */
  def init(config: JsonObject): Unit = {
    appId = config.getString("appId")
    secret = config.getString("secret")
  }

  /**
    * 拼装获取Code的URL参数
    *
    * @return URL参数
    */
  protected def packageFetchCodeParameters(): String = {
    val redirectUri = com.ecfront.ez.framework.service.rpc.http.ServiceAdapter.publicUrl + "public/oauth2/callback/" + appName + "/"
    Map(
      "appid" -> appId,
      "redirect_uri" -> redirectUri,
      "response_type" -> "code",
      "scope" -> "snsapi_userinfo",
      "state" -> s"${System.nanoTime()}"
    ).map(i => s"${i._1}=${i._2}").mkString("&")
  }

  /**
    * 返回获取Code的URL
    *
    * @return 获取Code的URL
    */
  def fetchCodeUrl: String = {
    fetchCodeBaseUrl + "?" + packageFetchCodeParameters
  }

  /**
    * 拼装获取Access Token的URL参数
    *
    * @param code 获取到的Code
    * @return URL参数
    */
  protected def packageFetchAccessTokenParameters(code: String): String = {
    Map(
      "appid" -> appId,
      "secret" -> secret,
      "code" -> code,
      "grant_type" -> "authorization_code"
    ).map(i => s"${i._1}=${i._2}").mkString("&")
  }

  /**
    * 获取Access Token
    *
    * @param code  获取到的Code
    * @param state 初始传入的state
    * @return Access Token
    */
  def fetchAccessToken(code: String, state: String): Resp[AccessToken] = {
    //TODO csrf
    val url = fetchAccessTokenBaseUrl + "?" + packageFetchAccessTokenParameters(code)
    try {
      val resp = JsonHelper.toObject[Map[String, String]](HttpClientProcessor.get(url))
      parseAccessToken(resp)
    } catch {
      case e: Throwable =>
        logger.warn("Fetch access token error." + e.getMessage, e)
        Resp.serverError("Fetch access token error." + e.getMessage)
    }
  }

  /**
    * 解析成Access Token
    *
    * @param resp 获取Access Token URL 的返回值
    * @return Access Token
    */
  protected def parseAccessToken(resp: Map[String, String]): Resp[AccessToken] = {
    if (!resp.contains("errcode")) {
      val accessToken = JsonHelper.toObject[AccessToken](resp)
      Resp.success(accessToken)
    } else {
      Resp.serverError(s"Fetch access token error [${resp("errcode")}] ${resp("errmsg")}")
    }
  }

  /**
    * 获取账号信息
    *
    * @param accessToken Access Token
    * @return 账号信息
    */
  def fetchAccount(accessToken: AccessToken): Resp[EZ_Account]

  // App ID 由平台提供
  var appId: String = _
  // Secret 由平台提供
  var secret: String = _

  // 处理类型，用于区别不同的oauth2
  protected val appName: String
  // 获取Code的基础URL
  protected val fetchCodeBaseUrl: String
  // 获取Access Token的基础URL
  protected val fetchAccessTokenBaseUrl: String

}



