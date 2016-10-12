package com.ecfront.ez.framework.service.auth

import com.ecfront.common.Resp
import com.ecfront.ez.framework.core.EZ
import com.ecfront.ez.framework.core.rpc._
import com.ecfront.ez.framework.service.auth.helper.CaptchaHelper
import com.ecfront.ez.framework.service.auth.model._
import com.ecfront.ez.framework.service.jdbc.BaseModel
import com.typesafe.scalalogging.slf4j.LazyLogging

@RPC("/ez/auth/")
object AuthService extends LazyLogging {

  private val random = new scala.util.Random

  @POST("/public/ez/auth/login/")
  def login(parameter: Map[String, String], body: Map[String, String]): Resp[OptInfo] = {
    if (!ServiceAdapter.customLogin) {
      // id 可以是 login_id 或 email
      val id = body.getOrElse("id", "")
      val password = body.getOrElse("password", "")
      val organizationCode = body.getOrElse("organizationCode", ServiceAdapter.defaultOrganizationCode)
      val captchaText = body.getOrElse("captcha", "")
      if (id != "" && password != "") {
        doLogin(id, password, organizationCode, captchaText)
      } else {
        logger.warn(s"[login] missing required field : 【id】or【password】from ${EZ.context.sourceIP}")
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
  def doLogin(loginIdOrEmail: String, password: String, organizationCode: String, captchaText: String): Resp[OptInfo] = {
    executeLogin(loginIdOrEmail, password, organizationCode, null, captchaText)
  }

  /**
    * 登录
    */
  def doLoginWithRoleFlags(loginIdOrEmail: String, password: String, organizationCode: String, roleFlags: Seq[String], captchaText: String): Resp[OptInfo] = {
    executeLogin(loginIdOrEmail, password, organizationCode, roleFlags, captchaText)
  }

  private def executeLogin(loginIdOrEmail: String, password: String, organizationCode: String, roleFlags: Seq[String], captchaText: String): Resp[OptInfo] = {
    val accountLoginIdOrEmailAndOrg = loginIdOrEmail + BaseModel.SPLIT + organizationCode
    val errorTimes = CacheManager.Login.getLoginErrorTimes(accountLoginIdOrEmailAndOrg)
    if (errorTimes < ServiceAdapter.loginLimit_showCaptcha
      || (captchaText.nonEmpty
      && errorTimes >= ServiceAdapter.loginLimit_showCaptcha
      && captchaText == CacheManager.Login.getCaptchaText(accountLoginIdOrEmailAndOrg)
      )) {
      val org = EZ_Organization.getByCode(organizationCode).body
      if (org != null) {
        if (org.enable) {
          val getR = EZ_Account.getByLoginIdOrEmail(loginIdOrEmail, organizationCode)
          if (getR && getR.body != null) {
            val account = getR.body
            if (EZ_Account.validateEncryptPwd(account.code, password, account.password)) {
              if (account.enable) {
                if (roleFlags == null || (roleFlags.toSet & account.role_codes.map(_.split(BaseModel.SPLIT)(1))).nonEmpty) {
                  val tokenInfo = CacheManager.Token.addToken(account, org)
                  CacheManager.Login.removeLoginErrorTimes(accountLoginIdOrEmailAndOrg)
                  CacheManager.Login.removeCaptcha(accountLoginIdOrEmailAndOrg)
                  logger.info(s"[login] success ,token:${tokenInfo.token} id:$loginIdOrEmail , organization:$organizationCode from ${EZ.context.sourceIP}")
                  EZ.eb.pubReq(ServiceAdapter.EB_LOGIN_SUCCESS_FLAG, tokenInfo)
                  Resp.success(tokenInfo)
                } else {
                  logger.warn(s"[login] account role not contains [${roleFlags.mkString(",")}] by id:$loginIdOrEmail , organization:$organizationCode from ${EZ.context.sourceIP}")
                  Resp.conflict(s"Account role not contains [${roleFlags.mkString(",")}]")
                }
              } else {
                logger.warn(s"[login] account disabled by id:$loginIdOrEmail , organization:$organizationCode from ${EZ.context.sourceIP}")
                Resp.locked(s"Account disabled")
              }
            } else {
              CacheManager.Login.addLoginErrorTimes(accountLoginIdOrEmailAndOrg)
              createCaptcha(accountLoginIdOrEmailAndOrg)
              logger.warn(s"[login] password not match by id:$loginIdOrEmail , organization:$organizationCode from ${EZ.context.sourceIP}")
              Resp.conflict(s"【password】 not match")
            }
          } else {
            logger.warn(s"[login] account not exist by id:$loginIdOrEmail , organization:$organizationCode from ${EZ.context.sourceIP}")
            Resp.notFound(s"Account not exist")
          }
        } else {
          logger.warn(s"Organization disabled by id:$loginIdOrEmail , organization:$organizationCode from ${EZ.context.sourceIP}")
          Resp.locked(s"Organization disabled")
        }
      } else {
        logger.warn(s"Organization not exist by id:$loginIdOrEmail , organization:$organizationCode from ${EZ.context.sourceIP}")
        Resp.notFound(s"Organization not exist")
      }
    } else {
      createCaptcha(accountLoginIdOrEmailAndOrg)
      logger.warn(s"[login] captcha not match by id:$loginIdOrEmail , organization:$organizationCode from ${EZ.context.sourceIP}")
      Resp.forbidden(s"【captcha】not match")
    }
  }

  @GET("/public/auth/captcha/:organizationCode/:id/")
  def getCaptcha(parameter: Map[String, String]): Resp[DownloadFile] = {
    val id = parameter.getOrElse("id", "")
    val organizationCode = parameter.getOrElse("organizationCode", ServiceAdapter.defaultOrganizationCode)
    val accountLoginIdOrEmailAndOrg = id + BaseModel.SPLIT + organizationCode
    Resp.success(createCaptcha(accountLoginIdOrEmailAndOrg))
  }

  def createCaptcha(accountLoginIdOrEmailAndOrg: String): DownloadFile = {
    if (CacheManager.Login.getLoginErrorTimes(accountLoginIdOrEmailAndOrg) >= ServiceAdapter.loginLimit_showCaptcha) {
      var text = random.nextDouble.toString
      text = text.substring(text.length - 4)
      val file = CaptchaHelper.generate(text)
      CacheManager.Login.addCaptcha(accountLoginIdOrEmailAndOrg, text, file.getPath)
      DownloadFile(file)
    } else {
      null
    }
  }

  @GET("logout/")
  def logout(parameter: Map[String, String]): Resp[Void] = {
    val token = parameter.getOrElse(RPCProcessor.VIEW_TOKEN_FLAG, "")
    if (token.nonEmpty) {
      doLogout(token)
    } else {
      Resp.badRequest(null)
    }
  }

  /**
    * 注销
    *
    */
  def doLogout(token: String): Resp[Void] = {
    val tokenInfo = CacheManager.Token.getTokenInfo(token)
    if (tokenInfo != null) {
      EZ.eb.pubReq(ServiceAdapter.EB_LOGOUT_FLAG, tokenInfo)
      CacheManager.Token.removeToken(token)
      Resp.success(null)
    }else{
      Resp.unAuthorized(null)
    }
  }

  @GET("logininfo/")
  def getLoginInfo(parameter: Map[String, String]): Resp[OptInfo] = {
    goGetLoginInfo(parameter(RPCProcessor.VIEW_TOKEN_FLAG))
  }

  def goGetLoginInfo(token: String): Resp[OptInfo] = {
    Resp.success(CacheManager.Token.getTokenInfo(token))
  }

  @GET("/public/ez/menu/")
  def getMenus(parameter: Map[String, String]): Resp[List[EZ_Menu]] = {
    if (parameter.contains(RPCProcessor.VIEW_TOKEN_FLAG)) {
      val tokenInfo = CacheManager.Token.getTokenInfo(parameter(RPCProcessor.VIEW_TOKEN_FLAG))
      if (tokenInfo != null) {
        doGetMenus(tokenInfo.roleCodes, tokenInfo.organizationCode)
      } else {
        Resp.success(List())
      }
    } else {
      doGetMenus(Set(), "")
    }
  }

  def doGetMenus(roleCodes: Set[String], organizationCode: String): Resp[List[EZ_Menu]] = {
    val allMenuR = EZ_Menu.findEnableByOrganizationCodeWithSort(organizationCode)
    val roleCodeSet = roleCodes
    val filteredMenus = allMenuR.body.filter {
      menu =>
        menu.role_codes == null || menu.role_codes.isEmpty || (menu.role_codes.toSet & roleCodeSet).nonEmpty
    }
    Resp.success(filteredMenus)
  }

}
