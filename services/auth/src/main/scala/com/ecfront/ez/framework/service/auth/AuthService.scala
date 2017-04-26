package com.ecfront.ez.framework.service.auth

import com.ecfront.common.Resp
import com.ecfront.ez.framework.core.EZ
import com.ecfront.ez.framework.core.logger.Logging
import com.ecfront.ez.framework.core.rpc._
import com.ecfront.ez.framework.service.auth.helper.CaptchaHelper
import com.ecfront.ez.framework.service.auth.model._
import com.ecfront.ez.framework.service.jdbc.BaseModel

@RPC("/ez/auth/", "EZ-权限服务", "")
object AuthService extends Logging {

  private val random = new scala.util.Random

  @POST("/public/ez/auth/login/", "登录", "",
    """
      |id|String|Id，可以是登录Id或email|true
      |password|String|密码|true
      |organizationCode|String|对应的组织编码|false
    """, "")
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

  @GET("/public/auth/captcha/:organizationCode/:id/", "获取图片验证码", "", "||File|验证码图片文件")
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

  @GET("logout/", "注销", "", "")
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
    } else {
      Resp.unAuthorized(null)
    }
  }

  @GET("logininfo/", "获取当前登录信息", "", "")
  def getLoginInfo(parameter: Map[String, String]): Resp[OptInfo] = {
    goGetLoginInfo(parameter(RPCProcessor.VIEW_TOKEN_FLAG))
  }

  def goGetLoginInfo(token: String): Resp[OptInfo] = {
    Resp.success(CacheManager.Token.getTokenInfo(token))
  }

  /**
    * 获取登录账号的信息
    *
    * @param parameter 请求参数
    * @return 登录账号的信息
    */
  @GET("account/bylogin/", "获取登录账号的信息", "", "")
  def getAccountByLoginId(parameter: Map[String, String]): Resp[AccountVO] = {
    if (EZ.context.optAccCode.nonEmpty) {
      val accountR = EZ_Account.getByCode(EZ.context.optAccCode)
      if (accountR) {
        if (accountR.body != null && accountR.body.enable) {
          val account = accountR.body
          val vo = AccountVO()
          vo.id = account.id
          vo.login_id = account.login_id
          vo.name = account.name
          vo.email = account.email
          vo.image = account.image
          vo.ext_id = account.ext_id
          vo.ext_info = account.ext_info
          Resp.success(vo)
        } else {
          Resp.unAuthorized("")
        }
      } else {
        accountR
      }
    } else {
      Resp.unAuthorized("")
    }
  }

  /**
    * 更新登录账号的信息
    *
    * @param parameter 请求参数
    * @param body      账号VO
    * @return 是否成功
    */
  @PUT("account/bylogin/", "更新登录账号的信息", "", "", "")
  def updateAccountByLoginId(parameter: Map[String, String], body: AccountVO): Resp[Void] = {
    if (EZ.context.optAccCode.nonEmpty) {
      val accountR = EZ_Account.getByCode(EZ.context.optAccCode)
      if (accountR) {
        if (accountR.body != null) {
          val account = accountR.body
          // 验证密码
          if (EZ_Account.validateEncryptPwd(account.code, body.current_password, account.password)) {
            if (body.new_password != null && body.new_password.nonEmpty) {
              account.password = body.new_password
            } else {
              account.exchange_pwd = account.password
            }
            account.name = body.name
            account.email = body.email
            account.image = body.image
            EZ_Account.update(account)
          } else {
            logger.warn(s"Old Password Error by id:${EZ.context.optInfo.get.loginId} from ${EZ.context.sourceIP}")
            Resp.conflict("Old Password Error")
          }
        } else {
          Resp.unAuthorized("")
        }
      } else {
        accountR
      }
    } else {
      logger.warn("Login Info not found")
      Resp.unAuthorized("Login Info not found")
    }
  }

  @GET("/public/ez/menu/", "获取菜单列表", "", "")
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
