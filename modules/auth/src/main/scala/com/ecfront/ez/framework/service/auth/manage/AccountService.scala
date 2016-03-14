package com.ecfront.ez.framework.service.auth.manage

import java.util.UUID

import com.ecfront.common.Resp
import com.ecfront.ez.framework.core.EZContext
import com.ecfront.ez.framework.core.helper.FileType
import com.ecfront.ez.framework.service.auth._
import com.ecfront.ez.framework.service.email.EmailProcessor
import com.ecfront.ez.framework.service.redis.RedisProcessor
import com.ecfront.ez.framework.service.rpc.foundation.{GET, POST, PUT, RPC}
import com.ecfront.ez.framework.service.rpc.http.scaffold.SimpleHttpService
import com.ecfront.ez.framework.service.rpc.http.HTTP
import com.ecfront.ez.framework.service.storage.foundation.{BaseModel, BaseStorage}

@RPC("/auth/manage/account/")
@HTTP
object AccountService extends SimpleHttpService[EZ_Account, EZAuthContext] {

  override protected val storageObj: BaseStorage[EZ_Account] = EZ_Account

  override protected def allowUploadTypes: List[String] = List(FileType.TYPE_IMAGE)

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
        EmailProcessor.send(body.email, s"${EZContext.app} activate your account",
          s"""
             | Please visit this link to activate your account:
             | <a href="${ServiceAdapter.activeUrl}?email=${body.email}&key=$encryption">
             | ${ServiceAdapter.activeUrl}?email=${body.email}&key=$encryption</a>
              """.stripMargin)
        Resp.success(null)
      } else {
        saveR
      }
    } else {
      Resp.notImplemented("Register NOT allow")
    }
  }

  @GET("/public/active/account/:email/:encryption/")
  def activeNewAccount(parameter: Map[String, String], context: EZAuthContext): Resp[String] = {
    val email = parameter("email")
    val encryption = parameter("encryption")
    val keyR = RedisProcessor.get(s"ez-active_account_$email")
    if (keyR && keyR.body == encryption) {
      RedisProcessor.del(s"ez-active_account_$email")
      val accountR = EZ_Account.getByCond(s"""{"email":"$email"}""")
      if (accountR && accountR.body != null) {
        accountR.body.enable = true
        EZ_Account.update(accountR.body, context)
      } else {
        Resp.notFound("Link illegal")
      }
    } else {
      Resp.notFound("Link illegal")
    }
  }

  @GET("bylogin/")
  def getAccountByLoginId(parameter: Map[String, String], context: EZAuthContext): Resp[Account_VO] = {
    if (context.token.isDefined&&context.loginInfo.isDefined) {
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

  @PUT("bylogin/")
  def updateAccountByLoginId(parameter: Map[String, String], body: Account_VO, context: EZAuthContext): Resp[Void] = {
    if (context.token.isDefined&&context.loginInfo.isDefined) {
      if (body.login_id == context.loginInfo.get.login_id) {
        val accountR = EZ_Account.getByLoginId(context.loginInfo.get.login_id)
        if (accountR) {
          if (accountR.body != null) {
            val account = accountR.body
            if (EZ_Account.packageEncryptPwd(account.login_id, body.old_password) == account.password) {
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

  @PUT("/public/findpassword/:email/")
  def findPassword(parameter: Map[String, String], body: Map[String, String], context: EZAuthContext): Resp[Void] = {
    val email = parameter("email")
    val existR = EZ_Account.existByCond(s"""{"email":"$email"}""")
    if (existR && existR.body) {
      val encryption = UUID.randomUUID().toString + System.nanoTime()
      RedisProcessor.set(s"ez-rest_password_key_$email", encryption, 60 * 60 * 24)
      RedisProcessor.set(s"ez-rest_password_pwd_$email", body("newPassword"), 60 * 60 * 24)
      EmailProcessor.send(email, s"${EZContext.app} Found password",
        s"""
           | Please visit this link to activate your new password:
           | <a href="${ServiceAdapter.restPasswordUrl}?email=$email&key=$encryption">
           | ${ServiceAdapter.restPasswordUrl}?email=$email&key=$encryption</a>
       """.stripMargin)
    } else {
      Resp.notFound("Not found this email")
    }
  }

  @GET("/public/active/password/:email/:encryption/")
  def activeNewPassword(parameter: Map[String, String], context: EZAuthContext): Resp[String] = {
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