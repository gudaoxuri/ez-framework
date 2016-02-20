package com.asto.ez.framework.auth.manage

import java.util.UUID

import com.asto.ez.framework.auth._
import com.asto.ez.framework.cache.RedisProcessor
import com.asto.ez.framework.mail.MailProcessor
import com.asto.ez.framework.rpc._
import com.asto.ez.framework.scaffold.SimpleRPCService
import com.asto.ez.framework.storage.{BaseModel, BaseStorage}
import com.asto.ez.framework.{EZContext, EZGlobal}
import com.ecfront.common.AsyncResp

import scala.concurrent.ExecutionContext.Implicits.global

@RPC("/auth/manage/account/")
@HTTP
object AccountService extends SimpleRPCService[EZ_Account] {

  override protected val storageObj: BaseStorage[EZ_Account] = EZ_Account

  override protected def allowUploadTypes: List[String] = List(FileType.TYPE_IMAGE)

  @POST("/public/register/")
  def register(parameter: Map[String, String], body: Account_VO, p: AsyncResp[String], context: EZContext) = {
    if (EZGlobal.ez_auth_allow_register) {
      val account = EZ_Account()
      account.login_id = body.login_id
      account.name = body.name
      account.email = body.email
      account.password = body.new_password
      account.image = body.image
      account.enable = false
      account.organization_code = ""
      account.role_codes = List(BaseModel.SPLIT + EZ_Role.USER_ROLE_CODE)
      EZ_Account.save(account, context).onSuccess {
        case saveResp =>
          if (saveResp) {
            val encryption = UUID.randomUUID().toString + System.nanoTime()
            RedisProcessor.set(s"ez-active_account_${body.email}", encryption, 60 * 60 * 24)
            MailProcessor.send(body.email, s"${EZGlobal.appName} activate your account",
              s"""
                 | Please visit this link to activate your account:
                 | <a href="${EZGlobal.ez_auth_active_url}?email=${body.email}&key=$encryption">
                 | ${EZGlobal.ez_auth_active_url}?email=${body.email}&key=$encryption</a>
              """.stripMargin).onSuccess {
              case emailResp =>
                p.resp(
                  emailResp)
            }
          } else {
            p.resp(saveResp)
          }
      }
    } else {
      p.notImplemented("Register NOT allow")
    }
  }

  @GET("/public/active/account/:email/:encryption/")
  def activeNewAccount(parameter: Map[String, String], p: AsyncResp[String], context: EZContext) = {
    val email = parameter("email")
    val encryption = parameter("encryption")
    RedisProcessor.get(s"ez-active_account_$email").onSuccess {
      case keyResp =>
        RedisProcessor.del(s"ez-active_account_$email")
        if (keyResp && keyResp.body == encryption) {
          EZ_Account.getByCond(s"""{"email":"$email"}""").onSuccess {
            case accountResp =>
              if (accountResp && accountResp.body != null) {
                accountResp.body.enable = true
                EZ_Account.update(accountResp.body, context).onSuccess {
                  case updateResp =>
                    p.resp(updateResp)
                }
              } else {
                p.notFound("Link illegal")
              }
          }
        } else {
          p.notFound("Link illegal")
        }
    }
  }

  @GET("bylogin/")
  def getAccountByLoginId(parameter: Map[String, String], p: AsyncResp[Account_VO], context: EZContext) = {
    EZ_Account.getByLoginId(context.login_info.login_id).onSuccess {
      case accountResp =>
        if (accountResp) {
          if (accountResp.body != null && accountResp.body.enable) {
            val account = accountResp.body
            val vo = Account_VO()
            vo.id = account.id
            vo.login_id = account.login_id
            vo.name = account.name
            vo.email = account.email
            vo.image = account.image
            p.success(vo)
          } else {
            p.unAuthorized("")
          }
        } else {
          p.resp(accountResp)
        }
    }
  }

  @PUT("bylogin/")
  def updateAccountByLoginId(parameter: Map[String, String], body: Account_VO, p: AsyncResp[Void], context: EZContext) = {
    if (body.login_id == context.login_info.login_id) {
      EZ_Account.getByLoginId(context.login_info.login_id).onSuccess {
        case accountResp =>
          if (accountResp) {
            if (accountResp.body != null) {
              val account = accountResp.body
              if (EZ_Account.packageEncryptPwd(account.login_id, body.old_password) == account.password) {
                if (body.new_password != null && body.new_password.nonEmpty) {
                  account.password = EZ_Account.packageEncryptPwd(account.login_id, body.new_password)
                }
                account.name = body.name
                account.email = body.email
                account.image = body.image
                EZ_Account.update(account, context).onSuccess {
                  case updateResp =>
                    if (updateResp) {
                      EZ_Token_Info.getById(context.token, context).onSuccess {
                        case tokenInfoResp =>
                          val tokenInfo = tokenInfoResp.body
                          tokenInfo.login_name = account.name
                          tokenInfo.image = account.image
                          EZ_Token_Info.update(tokenInfo, context).onSuccess {
                            case tokenResp =>
                              p.success(null)
                          }
                      }
                    } else {
                      p.resp(updateResp)
                    }
                }
              } else {
                p.badRequest("Old Password Error")
              }
            } else {
              p.unAuthorized("")
            }
          } else {
            p.resp(accountResp)
          }
      }
    } else {
      p.unAuthorized("")
    }
  }

  @PUT("/public/findpassword/:email/")
  def findPassword(parameter: Map[String, String], body: Map[String, String], p: AsyncResp[Void], context: EZContext) = {
    val email = parameter("email")
    EZ_Account.existByCond(s"""{"email":"$email"}""").onSuccess {
      case existResp =>
        if (existResp && existResp.body) {
          val encryption = UUID.randomUUID().toString + System.nanoTime()
          RedisProcessor.set(s"ez-rest_password_key_$email", encryption, 60 * 60 * 24)
          RedisProcessor.set(s"ez-rest_password_pwd_$email", body("newPassword"), 60 * 60 * 24)
          MailProcessor.send(email, s"${EZGlobal.appName} Found password",
            s"""
               | Please visit this link to activate your new password:
               | <a href="${EZGlobal.ez_auth_rest_password_url}?email=$email&key=$encryption">
               | ${EZGlobal.ez_auth_rest_password_url}?email=$email&key=$encryption</a>
       """.stripMargin).onSuccess {
            case emailResp =>
              p.resp(emailResp)
          }
        } else {
          p.notFound("Not found this email")
        }
    }
  }

  @GET("/public/active/password/:email/:encryption/")
  def activeNewPassword(parameter: Map[String, String], p: AsyncResp[String], context: EZContext) = {
    val email = parameter("email")
    val encryption = parameter("encryption")
    RedisProcessor.get(s"ez-rest_password_key_$email").onSuccess {
      case keyResp =>
        RedisProcessor.del(s"ez-rest_password_key_$email")
        if (keyResp && keyResp.body == encryption) {
          RedisProcessor.get(s"ez-rest_password_pwd_$email").onSuccess {
            case newPwdResp =>
              RedisProcessor.del(s"ez-rest_password_pwd_$email")
              if (newPwdResp && newPwdResp.body != null) {
                val newPassword = newPwdResp.body
                EZ_Account.getByCond(s"""{"email":"$email"}""").onSuccess {
                  case accountResp =>
                    if (accountResp && accountResp.body != null) {
                      accountResp.body.password = EZ_Account.packageEncryptPwd(accountResp.body.login_id, newPassword)
                      EZ_Account.update(accountResp.body, context).onSuccess {
                        case updateResp =>
                          p.resp(updateResp)
                      }
                    } else {
                      p.notFound("Link illegal")
                    }
                }
              } else {
                p.notFound("Link illegal")
              }
          }
        } else {
          p.notFound("Link illegal")
        }
    }
  }

}