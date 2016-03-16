package com.ecfront.ez.framework.service.auth.manage

import java.util.UUID

import com.ecfront.common.Resp
import com.ecfront.ez.framework.core.EZContext
import com.ecfront.ez.framework.core.helper.FileType
import com.ecfront.ez.framework.service.auth._
import com.ecfront.ez.framework.service.email.EmailProcessor
import com.ecfront.ez.framework.service.redis.RedisProcessor
import com.ecfront.ez.framework.service.rpc.foundation._
import com.ecfront.ez.framework.service.rpc.http.HTTP
import com.ecfront.ez.framework.service.rpc.http.scaffold.SimpleHttpService
import com.ecfront.ez.framework.service.storage.foundation.{BaseModel, BaseStorage}

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
      account.organization_code = ""
      account.role_codes = List(BaseModel.SPLIT + EZ_Role.USER_ROLE_CODE)
      val saveR = EZ_Account.save(account, context)
      if (saveR) {
        val encryption = UUID.randomUUID().toString + System.nanoTime()
        RedisProcessor.set(s"ez-active_account_${body.email}", encryption, 60 * 60 * 24)
        val activeUrl = com.ecfront.ez.framework.service.rpc.http.ServiceAdapter.publicUrl +
          s"public/active/account/${body.email}/$encryption/"
        EmailProcessor.send(body.email, s"${EZContext.app} activate your account",
          s"""
             | Please visit this link to activate your account:
             | <a href="$activeUrl">
             | $activeUrl</a>
              """.stripMargin)
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
  @GET("/public/active/account/:email/:encryption/")
  def activeNewAccount(parameter: Map[String, String], context: EZAuthContext): Resp[RespRedirect] = {
    val email = parameter("email")
    val encryption = parameter("encryption")
    val keyR = RedisProcessor.get(s"ez-active_account_$email")
    if (keyR && keyR.body == encryption) {
      RedisProcessor.del(s"ez-active_account_$email")
      val accountR = EZ_Account.getByCond(s"""{"email":"$email"}""")
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
      val accountR = EZ_Account.getByLoginId(context.loginInfo.get.login_id)
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
      Resp.badRequest("Login Info not found")
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
        val accountR = EZ_Account.getByLoginId(context.loginInfo.get.login_id)
        if (accountR) {
          if (accountR.body != null) {
            val account = accountR.body
            // 验证密码
            if (EZ_Account.packageEncryptPwd(account.login_id, body.current_password) == account.password) {
              if (body.new_password != null && body.new_password.nonEmpty) {
                account.password = EZ_Account.packageEncryptPwd(account.login_id, body.new_password)
              }
              account.name = body.name
              account.email = body.email
              account.image = body.image
              val updateR = EZ_Account.update(account, context)
              if (updateR) {
                val tokenInfoR = EZ_Token_Info.getById(context.token.get, context)
                val tokenInfo = tokenInfoR.body
                tokenInfo.login_name = account.name
                tokenInfo.image = account.image
                EZ_Token_Info.update(tokenInfo, context)
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
      Resp.badRequest("Login Info not found")
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
    val existR = EZ_Account.existByCond(s"""{"email":"$email"}""")
    if (existR && existR.body) {
      val encryption = UUID.randomUUID().toString + System.nanoTime()
      RedisProcessor.set(s"ez-rest_password_key_$email", encryption, 60 * 60 * 24)
      RedisProcessor.set(s"ez-rest_password_pwd_$email", body("newPassword"), 60 * 60 * 24)
      val activeUrl = com.ecfront.ez.framework.service.rpc.http.ServiceAdapter.publicUrl +
        s"public/active/password/$email/$encryption/"
      EmailProcessor.send(email, s"${EZContext.app} Found password",
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
  @GET("/public/active/password/:email/:encryption/")
  def activeNewPassword(parameter: Map[String, String], context: EZAuthContext): Resp[RespRedirect] = {
    val email = parameter("email")
    val encryption = parameter("encryption")
    val keyR = RedisProcessor.get(s"ez-rest_password_key_$email")
    if (keyR && keyR.body == encryption) {
      RedisProcessor.del(s"ez-rest_password_key_$email")
      val newPwdR = RedisProcessor.get(s"ez-rest_password_pwd_$email")
      if (newPwdR && newPwdR.body != null) {
        RedisProcessor.del(s"ez-rest_password_pwd_$email")
        val newPassword = newPwdR.body
        val accountR = EZ_Account.getByCond(s"""{"email":"$email"}""")
        if (accountR && accountR.body != null) {
          accountR.body.password = EZ_Account.packageEncryptPwd(accountR.body.login_id, newPassword)
          EZ_Account.update(accountR.body, context)
          Resp.success(RespRedirect(ServiceAdapter.loginUrl + "?action=findpassword"))
        } else {
          Resp.notFound("Link illegal")
        }
      } else {
        Resp.notFound("Link illegal")
      }
    } else {
      Resp.notFound("Link illegal")
    }
  }

}