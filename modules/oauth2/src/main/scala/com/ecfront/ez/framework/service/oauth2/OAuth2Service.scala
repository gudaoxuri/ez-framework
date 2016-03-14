package com.ecfront.ez.framework.service.oauth2

import java.util.UUID

import com.ecfront.common.Resp
import com.ecfront.ez.framework.service.auth._
import com.ecfront.ez.framework.service.rpc.foundation.{GET, RPC, RespRedirect}
import com.ecfront.ez.framework.service.rpc.http.HTTP
import io.vertx.core.json.JsonObject

import scala.collection.JavaConversions._

@RPC("/public/oauth2/")
@HTTP
object OAuth2Service {

  @GET("code/:app/")
  def login(parameter: Map[String, String], context: EZAuthContext): Resp[RespRedirect] = {
    val appName = parameter("app")
    val processor = getAppProcessor(appName)
    if (processor != null) {
      Resp.success(RespRedirect(processor.getCode))
    } else {
      Resp.badRequest(s"App name [$appName] not found.")
    }
  }

  @GET("callback/:app/")
  def callback(parameter: Map[String, String], context: EZAuthContext): Resp[Token_Info_VO] = {
    val appName = parameter("app")
    val processor = getAppProcessor(appName)
    if (processor != null) {
      val getTokenR = processor.getToken(parameter("code"), parameter("state"))
      if (getTokenR) {
        val oauthAccountR = processor.getAccount(getTokenR.body)
        if (oauthAccountR) {
          val oauthAccount = oauthAccountR.body
          val accountR = EZ_Account.getByOAuth(appName, oauthAccount.oauth(appName))
          if (accountR.body != null) {
            AuthService.addLoginInfo(accountR.body)
          } else {
            oauthAccount.login_id = oauthAccount.oauth(appName) + "@" + appName
            oauthAccount.email = oauthAccount.oauth(appName) + "@" + appName + EZ_Account.VIRTUAL_EMAIL
            oauthAccount.password = UUID.randomUUID().toString
            oauthAccount.organization_code = ""
            oauthAccount.role_codes = List(EZ_Role.USER_ROLE_CODE)
            oauthAccount.enable = true
            EZ_Account.save(oauthAccount)
            AuthService.addLoginInfo(accountR.body)
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

  /*@POST("register/")
  def register(parameter: Map[String, String], body: Account_VO, p: AsyncResp[Token_Info_VO], context: EZContext) = {
    val account = EZ_Account()
    account.login_id = body.login_id
    account.name = body.name
    account.email = body.email
    account.password = body.new_password
    account.image = body.image
    account.enable = true
    account.organization_code = ""
    account.role_codes = List(BaseModel.SPLIT + EZ_Role.USER_ROLE_CODE)
    account.ext_id = body.ext_id
    account.ext_info = body.ext_info
    EZ_Account.save(account, context).onSuccess {
      case saveR =>
        if (saveR) {
          AuthService.addLoginInfo(account, p)
        } else {
          p.resp(saveR)
        }
    }
  }*/


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
      case "weixin_open" =>
        WeixinOpenProcessor
      case "github" =>
        GitHubProcessor
      case _ =>
        null
    }
  }

}
