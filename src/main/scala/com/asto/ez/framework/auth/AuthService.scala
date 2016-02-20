package com.asto.ez.framework.auth

import java.util.UUID

import com.asto.ez.framework.EZContext
import com.asto.ez.framework.rpc.{GET, HTTP, POST, RPC}
import com.asto.ez.framework.storage.mongo.SortEnum
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
  def doLogin(loginIdOrEmail: String, password: String, p: AsyncResp[Token_Info_VO]) = {
    EZ_Account.getByLoginIdOrEmail(loginIdOrEmail).onSuccess {
      case getResp =>
        if (getResp && getResp.body != null) {
          val account = getResp.body
          if (EZ_Account.packageEncryptPwd(account.login_id, password) == account.password) {
            if(account.enable) {
              val rolesF = EZ_Role.findByCodes(account.role_codes)
              val orgF = EZ_Organization.getByCode(account.organization_code)
              val tokenInfo = EZ_Token_Info()
              tokenInfo.id = UUID.randomUUID().toString
              tokenInfo.login_id = account.login_id
              tokenInfo.login_name = account.name
              tokenInfo.image = account.image
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
                            tokenInfo.image,
                            tokenInfo.organization.code,
                            tokenInfo.organization.name,
                            tokenInfo.roles.map { role => role.code -> role.name }.toMap,
                            tokenInfo.ext_id,
                            tokenInfo.last_login_time
                          ))
                    }
                }
              }
            }else{
              p.notFound(s"Account disabled")
            }
          } else {
            p.notFound(s"【password】NOT match")
          }
        } else {
          p.notFound(s"【 $loginIdOrEmail】NOT exist")
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
              tokenInfo.image,
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

  @GET("/public/menu/")
  def getMenus(parameter: Map[String, String], p: AsyncResp[List[EZ_Menu]], context: EZContext) = {
    val tokenOpt = parameter.get(EZ_Token_Info.TOKEN_FLAG)
    if (tokenOpt.isDefined) {
      EZ_Token_Info.getById(tokenOpt.get).onSuccess {
        case tokenResp =>
          if (tokenResp && tokenResp.body != null) {
            doGetMenus(tokenResp.body.roles.map(_.getCode), p)
          } else {
            p.unAuthorized(s"Token【${tokenOpt.get}】已失效")
          }
      }
    } else {
      doGetMenus(List(), p)
    }
  }

  def doGetMenus(roleCodes: List[String], p: AsyncResp[List[EZ_Menu]]) = {
    EZ_Menu.findWithOpt(s"""{"enable":true}""", Map("sort" -> SortEnum.DESC)).onSuccess {
      case allMenuResp =>
        val roleCodeSet = roleCodes.toSet
        val filteredMenus = allMenuResp.body.filter {
          menu =>
            menu.role_codes == null || menu.role_codes.isEmpty || (menu.role_codes.toSet & roleCodeSet).nonEmpty
        }
        p.success(filteredMenus)
    }
  }

}
