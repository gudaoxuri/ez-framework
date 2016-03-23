package com.ecfront.ez.framework.service.oauth2

import java.util.UUID

import com.ecfront.common.Resp
import com.ecfront.ez.framework.service.auth._
import com.ecfront.ez.framework.service.auth.model.{EZ_Account, EZ_Role, EZ_Token_Info}
import com.ecfront.ez.framework.service.rpc.foundation.{GET, RPC, RespRedirect}
import com.ecfront.ez.framework.service.rpc.http.HTTP
import io.vertx.core.json.JsonObject

import scala.collection.JavaConversions._

@RPC("/public/oauth2/")
@HTTP
object OAuth2Service {

  private val indexUrl = com.ecfront.ez.framework.service.rpc.http.ServiceAdapter.webUrl

  @GET("code/:app/")
  def login(parameter: Map[String, String], context: EZAuthContext): Resp[RespRedirect] = {
    val appName = parameter("app")
    val processor = getAppProcessor(appName)
    if (processor != null) {
      Resp.success(RespRedirect(processor.fetchCodeUrl))
    } else {
      Resp.badRequest(s"App name [$appName] not found.")
    }
  }

  @GET("callback/:app/")
  def callback(parameter: Map[String, String], context: EZAuthContext): Resp[RespRedirect] = {
    val appName = parameter("app")
    val processor = getAppProcessor(appName)
    if (processor != null) {
      val getTokenR = processor.fetchAccessToken(parameter("code"), parameter("state"))
      if (getTokenR) {
        val oauthAccountR = processor.fetchAccount(getTokenR.body)
        if (oauthAccountR) {
          val oauthAccount = oauthAccountR.body
          val accountR = EZ_Account.getByOAuth(appName, oauthAccount.oauth(appName))
          if (accountR.body != null) {
            val loginInfo = AuthService.addLoginInfo(accountR.body)
            Resp.success(RespRedirect(indexUrl + "?" + EZ_Token_Info.TOKEN_FLAG + "=" + loginInfo.body.token))
          } else {
            oauthAccount.login_id = oauthAccount.oauth(appName) + "@" + appName
            oauthAccount.email = oauthAccount.oauth(appName) + "@" + appName + EZ_Account.VIRTUAL_EMAIL
            oauthAccount.password = UUID.randomUUID().toString
            oauthAccount.organization_code = ""
            oauthAccount.role_codes = List(EZ_Role.USER_ROLE_CODE)
            oauthAccount.enable = true
            val loginInfo = AuthService.addLoginInfo(EZ_Account.save(oauthAccount).body)
            Resp.success(RespRedirect(indexUrl + "?" + EZ_Token_Info.TOKEN_FLAG + "=" + loginInfo.body.token))
          }
        } else {
          oauthAccountR
        }
      } else {
        getTokenR
      }
    } else {
      Resp.badRequest(s"App name [$appName] not found.")
    }
  }

  def init(oauth2Config: JsonObject): Unit = {
    oauth2Config.iterator().foreach {
      item =>
        getAppProcessor(item.getKey).init(item.getValue.asInstanceOf[JsonObject])
    }
  }

  private def getAppProcessor(app: String): AppProcessor = {
    app match {
      case "weixin_mp" =>
        WeixinMPProcessor
      case _ =>
        null
    }
  }

}
