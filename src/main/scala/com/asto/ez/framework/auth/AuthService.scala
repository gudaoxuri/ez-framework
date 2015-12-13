package com.asto.ez.framework.auth

import java.util.UUID

import com.asto.ez.framework.EZContext
import com.asto.ez.framework.auth.manage.AccountService
import com.asto.ez.framework.rpc.{GET, HTTP, POST, RPC}
import com.ecfront.common.AsyncResp

import scala.async.Async.{async, await}
import scala.concurrent.ExecutionContext.Implicits.global

@RPC("/auth/")
@HTTP
object AuthService {

  @POST("/public/auth/login/")
  def login(parameter: Map[String, String], body: Map[String, String], p: AsyncResp[Token_Info_VO], context: EZContext) = async {
    if (body.contains("login_id")) {
      if (body.contains("password")) {
        await(doLogin(body("loginId"), body("password"), p))
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
  private def doLogin(loginId: String, password: String, p: AsyncResp[Token_Info_VO]) = async {
    val getResp = await(EZ_Account.getByLoginId(loginId))
    if (getResp && getResp.body != null) {
      val account = getResp.body
      if (AccountService.packageEncryptPwd(loginId, password) == account.password) {
        val rolesResp = EZ_Role.findByCodes(account.role_codes)
        val orgResp = EZ_Organization.getByCode(account.organization_code)
        val tokenInfo = EZ_Token_Info()
        tokenInfo.id = UUID.randomUUID().toString
        tokenInfo.login_id = account.id
        tokenInfo.login_name = account.name
        tokenInfo.organization = await(orgResp).body
        tokenInfo.ext_id = account.ext_id
        tokenInfo.last_login_time = System.currentTimeMillis()
        tokenInfo.roles = await(rolesResp).body
        await(tokenInfo.save())
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
        p.notFound(s"【password】NOT match")
      }
    } else {
      p.notFound(s"【 $loginId】NOT exist")
    }
  }

  @GET("logout/")
  def logout(parameter: Map[String, String], p: AsyncResp[Void], context: EZContext) = async {
    doLogout(parameter(EZ_Token_Info.TOKEN_FLAG), p)
  }

  /**
    * 注销
    *
    */
  private def doLogout(token: String, p: AsyncResp[Void]) = async {
    EZ_Token_Info.model.deleteById(token)
  }

  @GET("logininfo/")
  def getLoginInfo(parameter: Map[String, String], p: AsyncResp[Token_Info_VO], context: EZContext) = async {
    doGetLoginInfo(parameter(EZ_Token_Info.TOKEN_FLAG), p)
  }

  /**
    * 获取登录信息
    *
    */
  private def doGetLoginInfo(token: String, p: AsyncResp[Token_Info_VO]) = async {
    val tokenResp = await(EZ_Token_Info.model.getById(token))
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
      p.resp(tokenResp)
    }
  }

}
