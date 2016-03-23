package com.ecfront.ez.framework.service.auth

import java.util.UUID

import com.ecfront.common.Resp
import com.ecfront.ez.framework.service.auth.model._
import com.ecfront.ez.framework.service.rpc.foundation.{GET, POST, RPC}
import com.ecfront.ez.framework.service.rpc.http.HTTP

@RPC("/auth/")
@HTTP
object AuthService {

  @POST("/public/auth/login/")
  def login(parameter: Map[String, String], body: Map[String, String], context: EZAuthContext): Resp[Token_Info_VO] = {
    if (body.contains("login_id")) {
      if (body.contains("password")) {
        doLogin(body("login_id"), body("password"))
      } else {
        Resp.badRequest("Missing required field : 【password】")
      }
    } else {
      Resp.badRequest("Missing required field : 【login_id】")
    }
  }

  /**
    * 登录
    */
  def doLogin(loginIdOrEmail: String, password: String): Resp[Token_Info_VO] = {
    val getR = EZ_Account.getByLoginIdOrEmail(loginIdOrEmail)
    if (getR && getR.body != null) {
      val account = getR.body
      if (EZ_Account.packageEncryptPwd(account.login_id, password) == account.password) {
        addLoginInfo(account)
      } else {
        Resp.notFound(s"【password】NOT match")
      }
    } else {
      Resp.notFound(s"【 $loginIdOrEmail】NOT exist")
    }
  }

  def addLoginInfo(account: EZ_Account): Resp[Token_Info_VO] = {
    if (account.enable) {
      val tokenInfo = EZ_Token_Info()
      tokenInfo.id = UUID.randomUUID().toString
      tokenInfo.login_id = account.login_id
      tokenInfo.login_name = account.name
      tokenInfo.image = account.image
      tokenInfo.ext_id = account.ext_id
      tokenInfo.ext_info = account.ext_info
      tokenInfo.last_login_time = System.currentTimeMillis()
      tokenInfo.roles = EZ_Role.findByCodes(account.role_codes).body
      tokenInfo.organization = EZ_Organization.getByCode(account.organization_code).body
      EZ_Token_Info.deleteByLoginId(tokenInfo.login_id)
      val saveR = EZ_Token_Info.save(tokenInfo)
      if (saveR) {
        Resp.success(
          Token_Info_VO(
            tokenInfo.id,
            tokenInfo.login_id,
            tokenInfo.login_name,
            tokenInfo.image,
            tokenInfo.organization.code,
            tokenInfo.organization.name,
            tokenInfo.roles.map { role => role.code -> role.name }.toMap,
            tokenInfo.ext_id,
            tokenInfo.ext_info,
            tokenInfo.last_login_time
          ))
      } else {
        saveR
      }
    } else {
      Resp.notFound(s"Account disabled")
    }
  }

  @GET("logout/")
  def logout(parameter: Map[String, String], context: EZAuthContext): Resp[Void] = {
    doLogout(parameter(EZ_Token_Info.TOKEN_FLAG))
  }

  /**
    * 注销
    *
    */
  def doLogout(token: String): Resp[Void] = {
    EZ_Token_Info.deleteById(token)
  }

  @GET("logininfo/")
  def getLoginInfo(parameter: Map[String, String], context: EZAuthContext): Resp[Token_Info_VO] = {
    doGetLoginInfo(parameter(EZ_Token_Info.TOKEN_FLAG))
  }

  /**
    * 获取登录信息
    *
    */
  def doGetLoginInfo(token: String): Resp[Token_Info_VO] = {
    val tokenR = EZ_Token_Info.getById(token)
    if (tokenR && tokenR.body != null) {
      val tokenInfo = tokenR.body
      Resp.success(
        Token_Info_VO(
          tokenInfo.id,
          tokenInfo.login_id,
          tokenInfo.login_name,
          tokenInfo.image,
          tokenInfo.organization.code,
          tokenInfo.organization.name,
          tokenInfo.roles.map { role => role.code -> role.name }.toMap,
          tokenInfo.ext_id,
          tokenInfo.ext_info,
          tokenInfo.last_login_time
        ))
    } else {
      Resp.unAuthorized(s"Token【$token】已失效")
    }
  }

  @GET("/public/menu/")
  def getMenus(parameter: Map[String, String], context: EZAuthContext): Resp[List[EZ_Menu]] = {
    val tokenOpt = parameter.get(EZ_Token_Info.TOKEN_FLAG)
    if (tokenOpt.isDefined) {
      val tokenR = EZ_Token_Info.getById(tokenOpt.get)
      if (tokenR && tokenR.body != null) {
        doGetMenus(tokenR.body.roles.map(_.getCode))
      } else {
        Resp.unAuthorized(s"Token【${tokenOpt.get}】已失效")
      }
    } else {
      doGetMenus(List())
    }
  }

  def doGetMenus(roleCodes: List[String]): Resp[List[EZ_Menu]] = {
    val allMenuR = EZ_Menu.findEnableWithSort()
    val roleCodeSet = roleCodes.toSet
    val filteredMenus = allMenuR.body.filter {
      menu =>
        menu.role_codes == null || menu.role_codes.isEmpty || (menu.role_codes.toSet & roleCodeSet).nonEmpty
    }
    Resp.success(filteredMenus)
  }

}
