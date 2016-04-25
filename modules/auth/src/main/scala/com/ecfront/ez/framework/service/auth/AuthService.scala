package com.ecfront.ez.framework.service.auth

import com.ecfront.common.Resp
import com.ecfront.ez.framework.service.auth.model._
import com.ecfront.ez.framework.service.rpc.foundation.{GET, POST, RPC}
import com.ecfront.ez.framework.service.rpc.http.HTTP
import com.typesafe.scalalogging.slf4j.LazyLogging

@RPC("/auth/")
@HTTP
object AuthService extends LazyLogging {

  // 前端传入的token标识
  val VIEW_TOKEN_FLAG = "__ez_token__"

  @POST("/public/auth/login/")
  def login(parameter: Map[String, String], body: Map[String, String], context: EZAuthContext): Resp[Token_Info_VO] = {
    if (!ServiceAdapter.customLogin) {
      // id 可以是 login_id 或 email
      val id = body.getOrElse("id", "")
      val password = body.getOrElse("password", "")
      val organizationCode = body.getOrElse("organizationCode", ServiceAdapter.defaultOrganizationCode)
      if (id != "" && password != "") {
        doLogin(id, password, organizationCode, context)
      } else {
        logger.warn(s"[login] missing required field : 【id】or 【password】from ${context.remoteIP}")
        Resp.badRequest(s"Missing required field : 【id】or 【password】")
      }
    } else {
      Resp.notImplemented("Custom login enabled")
    }
  }

  /**
    * 登录
    */
  def doLogin(loginIdOrEmail: String, password: String, organizationCode: String, context: EZAuthContext): Resp[Token_Info_VO] = {
    val org = EZ_Organization.getByCode(organizationCode).body
    if (org != null && org.enable) {
      val getR = EZ_Account.getByLoginIdOrEmail(loginIdOrEmail, organizationCode)
      if (getR && getR.body != null) {
        val account = getR.body
        if (EZ_Account.packageEncryptPwd(account.login_id, password) == account.password) {
          if (account.enable) {
            val tokenInfo = CacheManager.addTokenInfo(account)
            logger.info(s"[login] success ,token:${tokenInfo.body.token} id:$loginIdOrEmail , organization:$organizationCode from ${context.remoteIP}")
            tokenInfo
          } else {
            logger.warn(s"[login] account disabled by id:$loginIdOrEmail , organization:$organizationCode from ${context.remoteIP}")
            Resp.locked(s"Account disabled")
          }
        } else {
          logger.warn(s"[login] password not match by id:$loginIdOrEmail , organization:$organizationCode from ${context.remoteIP}")
          Resp.conflict(s"【password】 not match")
        }
      } else {
        logger.warn(s"[login] account not exist in  by id:$loginIdOrEmail , organization:$organizationCode from ${context.remoteIP}")
        Resp.notFound(s"Account not exist")
      }
    } else {
      Resp.locked(s"Organization disabled")
    }
  }

  @GET("logout/")
  def logout(parameter: Map[String, String], context: EZAuthContext): Resp[Void] = {
    doLogout(parameter(VIEW_TOKEN_FLAG))
  }

  /**
    * 注销
    *
    */
  def doLogout(token: String): Resp[Void] = {
    CacheManager.removeTokenInfo(token)
  }

  @GET("logininfo/")
  def getLoginInfo(parameter: Map[String, String], context: EZAuthContext): Resp[Token_Info_VO] = {
    goGetLoginInfo(parameter(VIEW_TOKEN_FLAG))
  }

  def goGetLoginInfo(token: String): Resp[Token_Info_VO] = {
    CacheManager.getTokenInfo(token)
  }

  @GET("/public/menu/")
  def getMenus(parameter: Map[String, String], context: EZAuthContext): Resp[List[EZ_Menu]] = {
    if (parameter.contains(VIEW_TOKEN_FLAG)) {
      val tokenInfoR = CacheManager.getTokenInfo(parameter(VIEW_TOKEN_FLAG))
      if (tokenInfoR) {
        doGetMenus(tokenInfoR.body.role_codes, tokenInfoR.body.organization_code)
      } else {
        Resp.success(List())
      }
    } else {
      doGetMenus(List(), "")
    }
  }

  def doGetMenus(roleCodes: List[String], organizationCode: String): Resp[List[EZ_Menu]] = {
    val allMenuR = EZ_Menu.findEnableByOrganizationCodeWithSort(organizationCode)
    val roleCodeSet = roleCodes.toSet
    val filteredMenus = allMenuR.body.filter {
      menu =>
        menu.role_codes == null || menu.role_codes.isEmpty || (menu.role_codes.toSet & roleCodeSet).nonEmpty
    }
    Resp.success(filteredMenus)
  }

}
