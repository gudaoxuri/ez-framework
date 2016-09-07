package com.ecfront.ez.framework.service.auth

import java.io.File

import com.ecfront.common.Resp
import com.ecfront.ez.framework.service.auth.helper.CaptchaHelper
import com.ecfront.ez.framework.service.auth.model._
import com.ecfront.ez.framework.service.rpc.foundation.{GET, POST, RPC}
import com.ecfront.ez.framework.service.rpc.http.HTTP
import com.ecfront.ez.framework.service.storage.foundation.BaseModel
import com.typesafe.scalalogging.slf4j.LazyLogging

@RPC("/auth/")
@HTTP
object AuthService extends LazyLogging {

  // 前端传入的token标识
  val VIEW_TOKEN_FLAG = "__ez_token__"
  val random = new scala.util.Random

  @POST("/public/auth/login/")
  def login(parameter: Map[String, String], body: Map[String, String], context: EZAuthContext): Resp[Token_Info_VO] = {
    if (!ServiceAdapter.customLogin) {
      // id 可以是 login_id 或 email
      val id = body.getOrElse("id", "")
      val password = body.getOrElse("password", "")
      val organizationCode = body.getOrElse("organizationCode", ServiceAdapter.defaultOrganizationCode)
      val captchaText = body.getOrElse("captcha", "")
      if (id != "" && password != "") {
        doLogin(id, password, organizationCode, captchaText, context)
      } else {
        logger.warn(s"[login] missing required field : 【id】or【password】from ${context.remoteIP}")
        Resp.badRequest(s"Missing required field : 【id】or【password】")
      }
    } else {
      logger.warn(s"Custom login enabled")
      Resp.notImplemented("Custom login enabled")
    }
  }

  /**
    * 登录
    */
  def doLogin(loginIdOrEmail: String, password: String, organizationCode: String,
              captchaText: String, context: EZAuthContext): Resp[Token_Info_VO] = {
    executeLogin(loginIdOrEmail, password, organizationCode, null, captchaText, context)
  }

  /**
    * 登录
    */
  def doLoginWithRoleFlags(loginIdOrEmail: String, password: String, organizationCode: String, roleFlags: Seq[String],
                           captchaText: String, context: EZAuthContext): Resp[Token_Info_VO] = {
    executeLogin(loginIdOrEmail, password, organizationCode, roleFlags, captchaText, context)
  }

  private def executeLogin(loginIdOrEmail: String, password: String, organizationCode: String, roleFlags: Seq[String],
                           captchaText: String, context: EZAuthContext): Resp[Token_Info_VO] = {
    val accountLoginIdOrEmailAndOrg = loginIdOrEmail + "@" + organizationCode
    val errorTimes = CacheManager.getLoginErrorTimes(accountLoginIdOrEmailAndOrg)
    if (errorTimes < ServiceAdapter.loginLimit_showCaptcha
      || (captchaText.nonEmpty
      && errorTimes >= ServiceAdapter.loginLimit_showCaptcha
      && CacheManager.getCaptchaText(accountLoginIdOrEmailAndOrg) == captchaText
      )) {
      val org = EZ_Organization.getByCode(organizationCode).body
      if (org != null) {
        if (org.enable) {
          val getR = EZ_Account.getByLoginIdOrEmail(loginIdOrEmail, organizationCode)
          if (getR && getR.body != null) {
            val account = getR.body
            if (EZ_Account.validateEncryptPwd(account.login_id, password, account.password)) {
              if (account.enable) {
                if (roleFlags == null || (roleFlags.toSet & account.role_codes.map(_.split(BaseModel.SPLIT)(1)).toSet).nonEmpty) {
                  val tokenInfoR = CacheManager.addTokenInfo(account, org)
                  CacheManager.removeLoginErrorTimes(accountLoginIdOrEmailAndOrg)
                  CacheManager.removeCaptcha(accountLoginIdOrEmailAndOrg)
                  logger.info(s"[login] success ,token:${tokenInfoR.body.token} id:$loginIdOrEmail , organization:$organizationCode from ${context.remoteIP}")
                  ServiceAdapter.ezEvent_loginSuccess.publish(tokenInfoR.body)
                  tokenInfoR
                } else {
                  logger.warn(s"[login] account role not contains [${roleFlags.mkString(",")}] by id:$loginIdOrEmail , organization:$organizationCode from ${context.remoteIP}")
                  Resp.conflict(s"Account role not contains [${roleFlags.mkString(",")}]")
                }
              } else {
                logger.warn(s"[login] account disabled by id:$loginIdOrEmail , organization:$organizationCode from ${context.remoteIP}")
                Resp.locked(s"Account disabled")
              }
            } else {
              CacheManager.addLoginErrorTimes(accountLoginIdOrEmailAndOrg)
              createCaptcha(accountLoginIdOrEmailAndOrg)
              logger.warn(s"[login] password not match by id:$loginIdOrEmail , organization:$organizationCode from ${context.remoteIP}")
              Resp.conflict(s"【password】 not match")
            }
          } else {
            logger.warn(s"[login] account not exist by id:$loginIdOrEmail , organization:$organizationCode from ${context.remoteIP}")
            Resp.notFound(s"Account not exist")
          }
        } else {
          logger.warn(s"Organization disabled by id:$loginIdOrEmail , organization:$organizationCode from ${context.remoteIP}")
          Resp.locked(s"Organization disabled")
        }
      } else {
        logger.warn(s"Organization not exist by id:$loginIdOrEmail , organization:$organizationCode from ${context.remoteIP}")
        Resp.notFound(s"Organization not exist")
      }
    } else {
      createCaptcha(accountLoginIdOrEmailAndOrg)
      logger.warn(s"[login] captcha not match by id:$loginIdOrEmail , organization:$organizationCode from ${context.remoteIP}")
      Resp.forbidden(s"【captcha】not match")
    }
  }

  @GET("/public/auth/captcha/:organizationCode/:id/")
  def getCaptcha(parameter: Map[String, String], context: EZAuthContext): Resp[File] = {
    val id = parameter.getOrElse("id", "")
    val organizationCode = parameter.getOrElse("organizationCode", ServiceAdapter.defaultOrganizationCode)
    val accountLoginIdOrEmailAndOrg = id + "@" + organizationCode
    Resp.success(createCaptcha(accountLoginIdOrEmailAndOrg))
  }

  def createCaptcha(accountLoginIdOrEmailAndOrg: String): File = {
    if (CacheManager.getLoginErrorTimes(accountLoginIdOrEmailAndOrg) >= ServiceAdapter.loginLimit_showCaptcha) {
      var text = random.nextDouble.toString
      text = text.substring(text.length - 4)
      val file = CaptchaHelper.generate(text)
      CacheManager.addCaptcha(accountLoginIdOrEmailAndOrg, text, file.getPath)
      file
    } else {
      null
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
    val tokenInfoR = CacheManager.getTokenInfo(token)
    if (tokenInfoR && tokenInfoR.body != null) {
      ServiceAdapter.ezEvent_logout.publish(tokenInfoR.body)
    }
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
