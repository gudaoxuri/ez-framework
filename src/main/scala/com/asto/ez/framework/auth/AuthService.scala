package com.asto.ez.framework.auth

import java.util.UUID

import com.asto.ez.framework.EZContext
import com.asto.ez.framework.rpc.{GET, HTTP, POST, RPC}
import com.ecfront.common.AsyncResp

import scala.concurrent.ExecutionContext.Implicits.global

@RPC("/auth/")
@HTTP
object AuthService {

  @POST("/public/auth/login/")
  def login(parameter: Map[String, String], body: Map[String, String], p: AsyncResp[Token_Info_VO], context: EZContext) = {
    if (body.contains("login_id")) {
      if (body.contains("password")) {
        doLogin(body("login_id"), body("password"), p)
      } else {
        p.badRequest("Missing required field : 【password】")
      }
    } else {
      p.badRequest("Missing required field : 【login_id】")
    }
  }

  /**
    * 登录
    */
  def doLogin(loginId: String, password: String, p: AsyncResp[Token_Info_VO]) = {
    EZ_Account.getByLoginId(loginId).onSuccess {
      case getResp =>
        if (getResp && getResp.body != null) {
          val account = getResp.body
          if (EZ_Account.packageEncryptPwd(loginId, password) == account.password) {
            val rolesF = EZ_Role.findByCodes(account.role_codes)
            val orgF = EZ_Organization.getByCode(account.organization_code)
            val tokenInfo = EZ_Token_Info()
            tokenInfo.id = UUID.randomUUID().toString
            tokenInfo.login_id = account.login_id
            tokenInfo.login_name = account.name
            tokenInfo.ext_id = account.ext_id
            tokenInfo.last_login_time = System.currentTimeMillis()
            for {
              rolesResp <- rolesF
              orgResp <- orgF
            } yield {
              tokenInfo.roles = rolesResp.body
              tokenInfo.organization = orgResp.body
              EZ_Token_Info.deleteByCond(s"""{"login_id":"${tokenInfo.login_id}"}""").onSuccess {
                case logoutResp =>
                  EZ_Token_Info.save(tokenInfo).onSuccess {
                    case saveResp =>
                      p.success(
                        Token_Info_VO(
                          tokenInfo.id,
                          tokenInfo.login_id,
                          tokenInfo.login_name,
                          tokenInfo.organization.code,
                          tokenInfo.organization.name,
                          tokenInfo.roles.map { role => role.code -> role.name }.toMap,
                          tokenInfo.ext_id,
                          tokenInfo.last_login_time
                        ))
                  }
              }
            }
          } else {
            p.notFound(s"【password】NOT match")
          }
        } else {
          p.notFound(s"【 $loginId】NOT exist")
        }
    }
  }

  @GET("logout/")
  def logout(parameter: Map[String, String], p: AsyncResp[Void], context: EZContext) = {
    doLogout(parameter(EZ_Token_Info.TOKEN_FLAG), p)
  }

  /**
    * 注销
    *
    */
  def doLogout(token: String, p: AsyncResp[Void]) = {
    EZ_Token_Info.deleteById(token).onSuccess {
      case logoutResp => p.resp(logoutResp)
    }
  }

  @GET("logininfo/")
  def getLoginInfo(parameter: Map[String, String], p: AsyncResp[Token_Info_VO], context: EZContext) = {
    doGetLoginInfo(parameter(EZ_Token_Info.TOKEN_FLAG), p)
  }

  /**
    * 获取登录信息
    *
    */
  def doGetLoginInfo(token: String, p: AsyncResp[Token_Info_VO]) = {
    EZ_Token_Info.getById(token).onSuccess {
      case tokenResp =>
        if (tokenResp && tokenResp.body != null) {
          val tokenInfo = tokenResp.body
          p.success(
            Token_Info_VO(
              tokenInfo.id,
              tokenInfo.login_id,
              tokenInfo.login_name,
              tokenInfo.organization.code,
              tokenInfo.organization.name,
              tokenInfo.roles.map { role => role.code -> role.name }.toMap,
              tokenInfo.ext_id,
              tokenInfo.last_login_time
            ))
        } else {
          p.unAuthorized(s"Token【$token】已失效")
        }
    }
  }

}
