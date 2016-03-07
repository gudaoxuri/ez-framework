package com.asto.ez.framework.auth.oauth2

import java.util.UUID

import com.asto.ez.framework.EZContext
import com.asto.ez.framework.auth._
import com.asto.ez.framework.rpc._
import com.ecfront.common.AsyncResp
import io.vertx.core.json.JsonObject

import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global

@RPC("/public/oauth2/")
@HTTP
object OAuth2Service {

  @GET("code/:app/")
  def login(parameter: Map[String, String], p: AsyncResp[Resp_Redirect], context: EZContext) = {
    val appName = parameter("app")
    val processor = getAppProcessor(appName)
    if (processor != null) {
      p.success(Resp_Redirect(processor.getCode))
    } else {
      p.badRequest(s"App name [$appName] not found.")
    }
  }

  @GET("callback/:app/")
  def callback(parameter: Map[String, String], p: AsyncResp[Token_Info_VO], context: EZContext) = {
    val appName = parameter("app")
    val processor = getAppProcessor(appName)
    if (processor != null) {
      processor.getToken(parameter("code"), parameter("state")).onSuccess {
        case getTokenR =>
          if (getTokenR) {
            processor.getAccount(getTokenR.body).onSuccess {
              case oauthAccountR =>
                if (oauthAccountR) {
                  val oauthAccount = oauthAccountR.body
                  EZ_Account.getByOAuth(appName, oauthAccount.oauth(appName)).onSuccess {
                    case accountR =>
                      if (accountR.body != null) {
                        AuthService.addLoginInfo(accountR.body, p)
                      } else {
                        oauthAccount.login_id = oauthAccount.oauth(appName) + "@" + appName
                        oauthAccount.email = oauthAccount.oauth(appName) + "@" + appName + EZ_Account.VIRTUAL_EMAIL
                        oauthAccount.password = UUID.randomUUID().toString
                        oauthAccount.organization_code = ""
                        oauthAccount.role_codes = List(EZ_Role.USER_ROLE_CODE)
                        oauthAccount.enable = true
                        EZ_Account.save(oauthAccount).onSuccess {
                          case _ =>
                            AuthService.addLoginInfo(accountR.body, p)
                        }
                      }
                  }
                } else {
                  p.resp(oauthAccountR)
                }
            }
          } else {
            p.resp(getTokenR)
          }
      }
    } else {
      p.badRequest(s"App name [$appName] not found.")
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


  def init(oauth2Config: JsonObject) = {
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
