package com.ecfront.ez.framework.module.auth

import java.util.UUID

import com.ecfront.common.Resp
import com.ecfront.ez.framework.module.auth.manage.{AccountService, ResourceService}
import com.ecfront.ez.framework.module.core.EZReq
import com.ecfront.ez.framework.module.keylog.KeyLogService
import com.ecfront.ez.framework.rpc.{GET, HTTP, POST, RPC}
import com.ecfront.ez.framework.service.IdModel

@RPC("/auth/")
@HTTP
object AuthService {

  @POST("/public/auth/login/")
  def login(parameter: Map[String, String], body: Map[String, String], req: Option[EZReq]): Resp[TokenInfo] = {
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

  @GET("logout/")
  def logout(parameter: Map[String, String], req: Option[EZReq]): Resp[Void] = {
    doLogout(parameter(EZReq.TOKEN), req)
  }

  @GET("logininfo/")
  def getLoginInfo(parameter: Map[String, String], req: Option[EZReq]): Resp[Void] = {
    doGetLoginInfo(parameter(EZReq.TOKEN), req)
  }

  /**
   * 登录
   */
  private def doLogin(loginId: String, password: String): Resp[TokenInfo] = {
    val account = AccountService._getById(loginId).body
    if (account != null) {
      if (AccountService.packageEncryptPwd(loginId, password) == account.password) {
        val tokenInfo = TokenInfo()
        tokenInfo.id = UUID.randomUUID().toString
        tokenInfo.login_id = account.id
        tokenInfo.login_name = account.name
        tokenInfo.role_ids = account.role_ids
        tokenInfo.ext_id = account.ext_id
        tokenInfo.last_login_time = System.currentTimeMillis()
        val req = EZReq(tokenInfo.id, tokenInfo.login_id, tokenInfo.login_name, tokenInfo.role_ids)
        TokenService._save(tokenInfo, Some(req))
        KeyLogService.success(s"Login Success by ${tokenInfo.login_id} , token : ${tokenInfo.id}", Some(req))
        Resp.success(tokenInfo)
      } else {
        Resp.notFound(s"[ Password ] NOT match.")
      }
    } else {
      Resp.notFound(s"[ $loginId ] NOT exist.")
    }
  }

  /**
   * 注销
   *
   */
  private def doLogout(token: String, req: Option[EZReq]): Resp[Void] = {
    val loginInfo = TokenService._getById(token, req).body
    if (loginInfo != null) {
      TokenService._deleteById(token, req)
      KeyLogService.success(s"Logout Success by ${loginInfo.login_id} , token : ${loginInfo.id}", req)
    }
    Resp.success(null)
  }

  /**
   * 获取登录信息
   *
   */
  private def doGetLoginInfo(token: String, req: Option[EZReq]): Resp[TokenInfo] = {
    TokenService._getById(token, req)
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
  def authorization(method: String, uri: String, token: String): Resp[Void] = {
    val res = ResourceService._getById(method + IdModel.SPLIT_FLAG + uri).body
    if (res != null && res.role_ids.nonEmpty) {
      //请求资源（action）需要认证
      val loginInfoWrap = TokenService._getById(token)
      if (loginInfoWrap) {
        if ((res.role_ids.toSet & loginInfoWrap.body.role_ids.keySet).nonEmpty) {
          Resp.success(null)
        } else {
          KeyLogService.unAuthorized(s"The action [$method] [$uri] allowed role not in request.", None)
          Resp.unAuthorized(s"The action [$method] [$uri] allowed role not in request.")
        }
      } else {
        KeyLogService.unAuthorized(s"The action [$method] [$uri] need authorization.", None)
        loginInfoWrap
      }
    } else {
      //可匿名访问
      Resp.success(null)
    }

  }
}
