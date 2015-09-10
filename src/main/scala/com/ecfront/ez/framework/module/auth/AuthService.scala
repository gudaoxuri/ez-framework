package com.ecfront.ez.framework.module.auth

import java.util.UUID

import com.ecfront.common.{JsonHelper, Req, Resp}
import com.ecfront.ez.framework.module.auth.manage.AccountService
import com.ecfront.ez.framework.module.core.CommonUtils
import com.ecfront.ez.framework.module.keylog.KeyLogService
import com.ecfront.ez.framework.rpc.{GET, HTTP, POST, RPC}
import com.ecfront.ez.framework.storage.IdModel

@RPC("/auth/")
@HTTP
object AuthService {

  @POST("/public/auth/login/")
  def login(parameter: Map[String, String], body: Map[String, String], req: Option[Req]): Resp[Token_Info_VO] = {
    if (body.contains("loginId")) {
      if (body.contains("password")) {
        doLogin(body("loginId"), body("password"))
      } else {
        Resp.badRequest("Missing required field : [ Password ].")
      }
    } else {
      Resp.badRequest("Missing required field : [ LoginId ].")
    }
  }

  /**
   * 登录
   */
  private def doLogin(loginId: String, password: String): Resp[Token_Info_VO] = {
    val account = AccountService._getById(loginId).body
    if (account != null) {
      if (AccountService.packageEncryptPwd(loginId, password) == account.password) {
        val tokenInfo = EZ_Token_Info()
        tokenInfo.id = UUID.randomUUID().toString
        tokenInfo.login_id = account.id
        tokenInfo.login_name = account.name
        tokenInfo.role_ids_json = JsonHelper.toJsonString(account.role_ids.map(role => role._1 -> role._2.name))
        tokenInfo.ext_id = account.ext_id
        tokenInfo.last_login_time = System.currentTimeMillis()
        val req = Req(tokenInfo.id, tokenInfo.login_id, tokenInfo.login_name, tokenInfo.organization_id, tokenInfo.organization_name, account.role_ids.map { role => role._1 -> role._2.name })
        TokenService._save(tokenInfo, Some(req))
        KeyLogService.success(s"Login Success by ${tokenInfo.login_id} , token : ${tokenInfo.id}", Some(req))
        Resp.success(Token_Info_VO(tokenInfo.id, tokenInfo.login_id, tokenInfo.login_name, tokenInfo.organization_id, tokenInfo.organization_name, account.role_ids.map { role => role._1 -> role._2.name }, tokenInfo.ext_id, tokenInfo.last_login_time))
      } else {
        Resp.notFound(s"[ Password ] NOT match.")
      }
    } else {
      Resp.notFound(s"[ $loginId ] NOT exist.")
    }
  }

  @GET("logout/")
  def logout(parameter: Map[String, String], req: Option[Req]): Resp[Void] = {
    doLogout(parameter(CommonUtils.TOKEN), req)
  }

  /**
   * 注销
   *
   */
  private def doLogout(token: String, req: Option[Req]): Resp[Void] = {
    val loginInfo = TokenService._getById(token, req).body
    if (loginInfo != null) {
      TokenService._deleteById(token, req)
      KeyLogService.success(s"Logout Success by ${loginInfo.login_id} , token : ${loginInfo.id}", req)
    }
    Resp.success(null)
  }

  @GET("logininfo/")
  def getLoginInfo(parameter: Map[String, String], req: Option[Req]): Resp[Token_Info_VO] = {
    doGetLoginInfo(req.get.token, req)
  }

  /**
   * 获取登录信息
   *
   */
  private def doGetLoginInfo(token: String, req: Option[Req]): Resp[Token_Info_VO] = {
    val tokenInfoWrap = TokenService._getById(token, req)
    if (tokenInfoWrap) {
      val tokenInfo = tokenInfoWrap.body
      val roleIds = JsonHelper.toGenericObject[Map[String,String]](tokenInfo.role_ids_json)
      Resp.success(
        Token_Info_VO(
          tokenInfo.id,
          tokenInfo.login_id,
          tokenInfo.login_name,
          tokenInfo.organization_id,
          tokenInfo.organization_name,
          roleIds,
          tokenInfo.ext_id,
          tokenInfo.last_login_time
        )
      )
    } else {
      tokenInfoWrap
    }

  }

  /**
   * 授权
   * <ul>核心流程：
   * <li>action在资源表中是否存在，不存在表示可匿名访问，通过</li>
   * <li>action在资源表存在但没有关联任务角色，表示可匿名访问，通过</li>
   * <li>获取登录用户信息</li>
   * <li>比对登录用户的角色与action可访问的角色是否有交集，有则通过反之不通过</li>
   * </ul>
   */
  def authorizationPublicServer(method: String, uri: String, token: String): Resp[Req] = {
    val resourceCode = method + IdModel.SPLIT_FLAG + uri
    if (LocalCacheContainer.existResource(resourceCode)) {
      //请求资源（action）需要认证
      val tokenInfoWrap = TokenService._getById(token)
      if (tokenInfoWrap && tokenInfoWrap.body != null) {
        val tokenInfo = tokenInfoWrap.body
        val roleIds = JsonHelper.toGenericObject[Map[String,String]](tokenInfo.role_ids_json)
        if (LocalCacheContainer.matchInRoles(resourceCode, roleIds.keySet)) {
          Resp.success(Req(tokenInfo.id, tokenInfo.login_id, tokenInfo.login_name, tokenInfo.organization_id, tokenInfo.organization_name, roleIds))
        } else {
          KeyLogService.unAuthorized(s"The action [$method] [$uri] allowed role not in request.", None)
          Resp.unAuthorized(s"The action [$method] [$uri] allowed role not in request.")
        }
      } else {
        KeyLogService.unAuthorized(s"The action [$method] [$uri] need authorization.", None)
        Resp.unAuthorized(s"The action [$method] [$uri] need authorization.")
      }
    } else {
      //可匿名访问
      Resp.success(Req.anonymousReq)
    }
  }

  def authorizationInnerServer(method: String, uri: String, token: String): Resp[Req] = {
    val tokenInfoWrap = TokenService._getById(token)
    if (tokenInfoWrap && tokenInfoWrap.body != null) {
      val tokenInfo = tokenInfoWrap.body
      val roleIds = JsonHelper.toGenericObject[Map[String,String]](tokenInfo.role_ids_json)
      Resp.success(Req(tokenInfo.id, tokenInfo.login_id, tokenInfo.login_name, tokenInfo.organization_id, tokenInfo.organization_name, roleIds))
    } else {
      Resp.success(Req.anonymousReq)
    }
  }

}
