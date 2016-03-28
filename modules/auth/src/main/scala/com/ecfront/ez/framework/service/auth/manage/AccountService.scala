package com.ecfront.ez.framework.service.auth.manage

import java.util.UUID

import com.ecfront.common.Resp
import com.ecfront.ez.framework.core.EZContext
import com.ecfront.ez.framework.core.helper.FileType
import com.ecfront.ez.framework.service.auth._
import com.ecfront.ez.framework.service.auth.model.{EZ_Account, EZ_Role}
import com.ecfront.ez.framework.service.email.EmailProcessor
import com.ecfront.ez.framework.service.rpc.foundation._
import com.ecfront.ez.framework.service.rpc.http.HTTP
import com.ecfront.ez.framework.service.rpc.http.scaffold.SimpleHttpService
import com.ecfront.ez.framework.service.storage.foundation.BaseStorage

/**
  * 账号管理
  */
@RPC("/auth/manage/account/")
@HTTP
object AccountService extends SimpleHttpService[EZ_Account, EZAuthContext] {

  override protected val storageObj: BaseStorage[EZ_Account] = EZ_Account

  // 只能上传图片类型
  override protected def allowUploadTypes = List(FileType.TYPE_IMAGE)

  /**
    * 注册账号，需要激活
    *
    * @param parameter 请求参数
    * @param body      账号VO
    * @param context   PRC上下文
    * @return 是否成功
    */
  @POST("/public/register/")
  def register(parameter: Map[String, String], body: Account_VO, context: EZAuthContext): Resp[Void] = {
    if (ServiceAdapter.allowRegister) {
      val account = EZ_Account()
      account.login_id = body.login_id
      account.name = body.name
      account.email = body.email
      account.password = body.new_password
      account.image = body.image
      account.enable = false
      // 用户自助注册只能到默认组织下
      account.organization_code = ServiceAdapter.defaultOrganizationCode
      // 用户自助注册的角色默认是 user
      account.role_codes = List(EZ_Role.assembleCode(ServiceAdapter.defaultRoleFlag, account.organization_code))
      val saveR = EZ_Account.save(account, context)
      if (saveR) {
        if (ServiceAdapter.selfActive) {
          val encryption = UUID.randomUUID().toString + System.nanoTime()
          CacheManager.addActiveAccount(encryption, saveR.body.code)
          val activeUrl = com.ecfront.ez.framework.service.rpc.http.ServiceAdapter.publicUrl +
            s"public/active/account/$encryption/"
          EmailProcessor.send(body.email, s"${EZContext.app} activate your account",
            s"""Please visit this link to activate your account:
                | <a href="$activeUrl">
                | $activeUrl</a>""".stripMargin)
        } else {
          EmailProcessor.send(body.email, s"${EZContext.app} waiting audit",
            s"""Waiting audit to activate your account.""".stripMargin)
        }
        Resp.success(null)
      } else {
        saveR
      }
    } else {
      Resp.notImplemented("Register NOT allow")
    }
  }

  /**
    * 激活账号
    *
    * @param parameter 请求参数
    * @param context   PRC上下文
    * @return 是否成功，成功后跳转到登录url，带 `action=active` 参数
    */
  @GET("/public/active/account/:encryption/")
  def activeNewAccount(parameter: Map[String, String], context: EZAuthContext): Resp[RespRedirect] = {
    val encryption = parameter("encryption")
    val codeR = CacheManager.getAndRemoveActiveAccount(encryption)
    if (codeR && codeR.body != null) {
      val accountR = EZ_Account.getByCode(codeR.body)
      if (accountR && accountR.body != null) {
        accountR.body.enable = true
        EZ_Account.update(accountR.body, context)
        Resp.success(RespRedirect(ServiceAdapter.loginUrl + "?action=active"))
      } else {
        Resp.notFound("Link illegal")
      }
    } else {
      Resp.notFound("Link illegal")
    }
  }

  /**
    * 获取登录账号的信息
    *
    * @param parameter 请求参数
    * @param context   PRC上下文
    * @return 登录账号的信息
    */
  @GET("bylogin/")
  def getAccountByLoginId(parameter: Map[String, String], context: EZAuthContext): Resp[Account_VO] = {
    if (context.token.isDefined && context.loginInfo.isDefined) {
      val accountR = EZ_Account.getByLoginId(context.loginInfo.get.login_id, context.loginInfo.get.organization_code)
      if (accountR) {
        if (accountR.body != null && accountR.body.enable) {
          val account = accountR.body
          val vo = Account_VO()
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
    * @param context   PRC上下文
    * @return 是否成功
    */
  @PUT("bylogin/")
  def updateAccountByLoginId(parameter: Map[String, String], body: Account_VO, context: EZAuthContext): Resp[Void] = {
    if (context.token.isDefined && context.loginInfo.isDefined) {
      if (body.login_id == context.loginInfo.get.login_id) {
        val accountR = EZ_Account.getByLoginId(context.loginInfo.get.login_id, context.loginInfo.get.organization_code)
        if (accountR) {
          if (accountR.body != null) {
            val account = accountR.body
            // 验证密码
            if (EZ_Account.packageEncryptPwd(account.login_id, body.current_password) == account.password) {
              if (body.new_password != null && body.new_password.nonEmpty) {
                account.password = body.new_password
              }
              account.name = body.name
              account.email = body.email
              account.image = body.image
              val updateR = EZ_Account.update(account, context)
              if (updateR) {
                CacheManager.updateTokenInfo(updateR.body)
              } else {
                updateR
              }
            } else {
              Resp.badRequest("Old Password Error")
            }
          } else {
            Resp.unAuthorized("")
          }
        } else {
          accountR
        }
      } else {
        Resp.unAuthorized("")
      }
    } else {
      Resp.unAuthorized("Login Info not found")
    }
  }

  /**
    * 找回密码，需求确认
    *
    * @param parameter 请求参数
    * @param body      包含 newPassword 参数，表示新的密码
    * @param context   PRC上下文
    * @return 是否成功
    */
  @PUT("/public/findpassword/:email/")
  def findPassword(parameter: Map[String, String], body: Map[String, String], context: EZAuthContext): Resp[Void] = {
    val email = parameter("email")
    val newPassword = body("newPassword")
    // 找回密码只针对默认组织
    val accountR = EZ_Account.getByEmail(email, "")
    if (accountR && accountR.body != null) {
      val encryption = UUID.randomUUID().toString + System.nanoTime()
      CacheManager.addActiveNewPassword(encryption, accountR.body.code, newPassword)
      val activeUrl = com.ecfront.ez.framework.service.rpc.http.ServiceAdapter.publicUrl +
        s"public/active/password/$encryption/"
      EmailProcessor.send(email, s"${EZContext.app} Activate new password",
        s"""
           | Please visit this link to activate your new password:
           | <a href="$activeUrl">
           | $activeUrl</a>
       """.stripMargin)
    } else {
      Resp.notFound("Not found this email")
    }
  }

  /**
    * 激活（确认）新密码
    *
    * @param parameter 请求参数
    * @param context   PRC上下文
    * @return 是否成功，成功后跳转到登录url，带 `action=findpassword` 参数
    */
  @GET("/public/active/password/:encryption/")
  def activeNewPassword(parameter: Map[String, String], context: EZAuthContext): Resp[RespRedirect] = {
    val encryption = parameter("encryption")
    val newPasswordR = CacheManager.getAndRemoveNewPassword(encryption)
    if (newPasswordR && newPasswordR.body != null) {
      val accountR = EZ_Account.getByCode(newPasswordR.body._1)
      if (accountR && accountR.body != null) {
        accountR.body.exchange_pwd = newPasswordR.body._2
        EZ_Account.update(accountR.body, context)
        Resp.success(RespRedirect(ServiceAdapter.loginUrl + "?action=findpassword"))
      } else {
        Resp.notFound("Link illegal")
      }
    } else {
      Resp.notFound("Link illegal")
    }
  }

}