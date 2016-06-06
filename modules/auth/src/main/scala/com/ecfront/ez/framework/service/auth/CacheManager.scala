package com.ecfront.ez.framework.service.auth

import java.io.File
import java.util.UUID
import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger}
import java.util.concurrent.locks.ReentrantLock

import com.ecfront.common.{JsonHelper, Resp}
import com.ecfront.ez.framework.service.auth.model._
import com.ecfront.ez.framework.service.redis.RedisProcessor
import com.typesafe.scalalogging.slf4j.LazyLogging

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future, Promise}

object CacheManager extends LazyLogging{

  // Token信息 key : ez.token.info:<token Id> value : <token info>
  private val TOKEN_INFO_FLAG = "ez.token.info:"
  // Token Id 关联 key : ez.token.id.rel:<login Id> value : <token Id>
  private val TOKEN_ID_REL_FLAG = "ez.token.id.rel:"

  // 资源列表 key : ez.resource.<resource code> value : any
  private val RESOURCES_FLAG = "ez.resources"
  // 资源 关联 key : ez.resource.rel:<role code> value : <resource codes>
  private val RESOURCES_REL_FLAG = "ez.resource.rel:"

  // 用户注册激活 key : ez.active.account:<encryption> value : <account code>
  private val ACTIVE_ACCOUNT_FLAG = "ez.active.account:"

  // 找回密码激活 key : ez.active.find-pwd:<encryption> value : <account code>
  private val ACTIVE_FIND_PASSWORD_FLAG = "ez.active.find-pwd:"
  // 找回密码激活 key : ez.active.new-pwd:<account code> value : <new password>
  private val ACTIVE_NEW_PASSWORD_FLAG = "ez.active.new-pwd:"

  // 组织信息
  private val ORGANIZATIONS_FLAG = "ez.organizations"

  // 连续登录错误次数
  private val LOGIN_ERROR_TIMES_FLAG = "ez.login.error.times:"
  // 登录验证码的字符
  private val LOGIN_CAPTCHA_TEXT_FLAG = "ez.login.captcha.text"
  // 登录验证码的文件路径
  private val LOGIN_CAPTCHA_FILE_FLAG = "ez.login.captcha.file"

  val tokenLock = new ReentrantLock()

  def addTokenInfo(account: EZ_Account, org: EZ_Organization): Resp[Token_Info_VO] = {
    // 加锁，避免在多线程下`TOKEN_ID_REL_FLAG + account.code`竞争问题
    tokenLock.lock()
    removeToken(account.code)
    val newTokenInfo = Token_Info_VO(
      UUID.randomUUID().toString,
      EZ_Account.assembleCode(account.login_id, account.organization_code),
      account.login_id,
      account.name,
      account.email,
      account.image,
      account.organization_code,
      org.name,
      account.role_codes,
      account.ext_id,
      account.ext_info
    )
    RedisProcessor.set(TOKEN_ID_REL_FLAG + account.code, newTokenInfo.token, ServiceAdapter.loginKeepSeconds)
    RedisProcessor.set(TOKEN_INFO_FLAG + newTokenInfo.token, JsonHelper.toJsonString(newTokenInfo), ServiceAdapter.loginKeepSeconds)
    tokenLock.unlock()
    Resp.success(newTokenInfo)
  }

  def getToken(accountCode: String): String = {
    val tokenR = RedisProcessor.get(TOKEN_ID_REL_FLAG + accountCode)
    if (tokenR.body != null) {
      tokenR.body.asInstanceOf[String]
    } else {
      null
    }
  }

  def removeToken(accountCode: String): Unit = {
    val token = getToken(accountCode)
    if (token != null) {
      removeTokenInfo(token)
    }
  }

  def updateTokenInfo(account: EZ_Account): Resp[Void] = {
    val token = getToken(account.code)
    if (token != null) {
      val oldTokenInfo = getTokenInfo(token).body
      if (oldTokenInfo == null) {
        // 在某些情况下（如缓存被清空）可能存在原token信息不存在，此时要求重新登录
        removeTokenInfo(token)
      } else {
        val newTokenInfo = Token_Info_VO(
          token,
          EZ_Account.assembleCode(account.login_id, account.organization_code),
          account.login_id,
          account.name,
          account.email,
          account.image,
          oldTokenInfo.organization_code,
          oldTokenInfo.organization_name,
          account.role_codes,
          account.ext_id,
          account.ext_info
        )
        RedisProcessor.set(TOKEN_INFO_FLAG + newTokenInfo.token, JsonHelper.toJsonString(newTokenInfo), ServiceAdapter.loginKeepSeconds)
      }
    }
    Resp.success(null)
  }

  def getTokenInfo(token: String): Resp[Token_Info_VO] = {
    val infoR = Await.result(getTokenInfoAsync(token), Duration.Inf)
    if (infoR) {
      if (existOrganization(infoR.body.organization_code)) {
        infoR
      } else {
        removeTokenInfo(token)
        Resp.notFound("")
      }
    } else {
      infoR
    }
  }

  def getTokenInfoAsync(token: String): Future[Resp[Token_Info_VO]] = {
    val p = Promise[Resp[Token_Info_VO]]()
    RedisProcessor.Async.get(TOKEN_INFO_FLAG + token).onSuccess {
      case existTokenInfoR =>
        if (existTokenInfoR && existTokenInfoR.body != null) {
          p.success(Resp.success(JsonHelper.toObject[Token_Info_VO](existTokenInfoR.body)))
        } else {
          logger.warn("Token NOT exist")
          p.success(Resp.unAuthorized("Token NOT exist"))
        }
    }
    p.future
  }

  def removeTokenInfo(token: String): Resp[Void] = {
    val existTokenInfoR = RedisProcessor.get(TOKEN_INFO_FLAG + token)
    if (existTokenInfoR.body != null) {
      val existTokenInfo = JsonHelper.toObject[Token_Info_VO](existTokenInfoR.body)
      RedisProcessor.del(TOKEN_ID_REL_FLAG + existTokenInfo.login_id)
    }
    RedisProcessor.del(TOKEN_INFO_FLAG + token)
    Resp.success(null)
  }

  def addResource(resourceCode: String): Resp[Void] = {
    RedisProcessor.hset(RESOURCES_FLAG, resourceCode, "")
  }

  def removeResource(resourceCode: String): Resp[Void] = {
    RedisProcessor.hdel(RESOURCES_FLAG, resourceCode)
  }

  def existResource(resourceCode: String): Future[Resp[Boolean]] = {
    val p = Promise[Resp[Boolean]]()
    RedisProcessor.Async.hget(RESOURCES_FLAG, resourceCode).onSuccess {
      case resR =>
        if (resR.body != null) {
          p.success(Resp.success(true))
        } else {
          p.success(Resp.success(false))
        }
    }
    p.future
  }

  def addResourceByRole(roleCode: String, resourceCodes: List[String]): Resp[Void] = {
    RedisProcessor.lmset(RESOURCES_REL_FLAG + roleCode, resourceCodes)
  }

  def removeResourceByRole(roleCode: String): Resp[Void] = {
    RedisProcessor.del(RESOURCES_REL_FLAG + roleCode)
  }

  def existResourceByRoles(roleCodes: List[String], resourceCode: String): Future[Resp[Boolean]] = {
    val p = Promise[Resp[Boolean]]()
    val counter = new AtomicInteger(roleCodes.size)
    val isFind = new AtomicBoolean(false)
    if (roleCodes.isEmpty) {
      p.success(Resp.success(false))
    } else {
      roleCodes.foreach {
        roleCode =>
          RedisProcessor.Async.lget(RESOURCES_REL_FLAG + roleCode).onSuccess {
            case resR =>
              if (resR.body != null && resR.body.contains(resourceCode)) {
                isFind.set(true)
                p.success(Resp.success(true))
              }
              if (counter.decrementAndGet() == 0 && !isFind.get()) {
                p.success(Resp.success(false))
              }
          }
      }
    }
    p.future
  }

  def addActiveAccount(encryption: String, accountCode: String): Unit = {
    RedisProcessor.set(ACTIVE_ACCOUNT_FLAG + encryption.hashCode, accountCode, ServiceAdapter.activeKeepSeconds)
  }

  def getAndRemoveActiveAccount(encryption: String): String = {
    val resp = RedisProcessor.get(ACTIVE_ACCOUNT_FLAG + encryption.hashCode)
    RedisProcessor.del(ACTIVE_ACCOUNT_FLAG + encryption.hashCode)
    if (resp && resp.body != null) {
      resp.body.asInstanceOf[String]
    } else {
      null
    }
  }

  def addActiveNewPassword(encryption: String, accountCode: String, newPassword: String): Unit = {
    RedisProcessor.set(ACTIVE_FIND_PASSWORD_FLAG + encryption.hashCode, accountCode, ServiceAdapter.activeKeepSeconds)
    RedisProcessor.set(ACTIVE_NEW_PASSWORD_FLAG + accountCode, newPassword, ServiceAdapter.activeKeepSeconds)
  }

  def getAndRemoveNewPassword(encryption: String): Resp[(String, String)] = {
    val accountCodeR = RedisProcessor.get(ACTIVE_FIND_PASSWORD_FLAG + encryption.hashCode)
    RedisProcessor.del(ACTIVE_FIND_PASSWORD_FLAG + encryption.hashCode)
    if (accountCodeR && accountCodeR.body != null) {
      val newPasswordR = RedisProcessor.get(ACTIVE_NEW_PASSWORD_FLAG + accountCodeR.body)
      RedisProcessor.del(ACTIVE_NEW_PASSWORD_FLAG + accountCodeR.body)
      if (newPasswordR && newPasswordR.body != null) {
        Resp.success((accountCodeR.body.asInstanceOf[String], newPasswordR.body.asInstanceOf[String]))
      } else {
        newPasswordR
      }
    } else {
      accountCodeR
    }
  }

  def addOrganization(organizationCode: String): Unit = {
    RedisProcessor.hset(ORGANIZATIONS_FLAG, organizationCode, "")
  }

  def removeOrganization(organizationCode: String): Unit = {
    RedisProcessor.hdel(ORGANIZATIONS_FLAG, organizationCode)
  }

  def existOrganizationAsync(organizationCode: String): Future[Resp[Boolean]] = {
    RedisProcessor.Async.hexist(ORGANIZATIONS_FLAG, organizationCode)
  }

  def existOrganization(organizationCode: String): Boolean = {
    RedisProcessor.hexist(ORGANIZATIONS_FLAG, organizationCode).body
  }

  def addLoginErrorTimes(accountLoginIdOrEmailAndOrg: String): Long = {
    RedisProcessor.incr(LOGIN_ERROR_TIMES_FLAG + accountLoginIdOrEmailAndOrg).body
  }

  def removeLoginErrorTimes(accountLoginIdOrEmailAndOrg: String): Unit = {
    RedisProcessor.del(LOGIN_ERROR_TIMES_FLAG + accountLoginIdOrEmailAndOrg)
  }

  def getLoginErrorTimes(accountLoginIdOrEmailAndOrg: String): Long = {
    RedisProcessor.custom().getAtomicLong(LOGIN_ERROR_TIMES_FLAG + accountLoginIdOrEmailAndOrg).get()
  }

  def addCaptcha(accountLoginIdOrEmailAndOrg: String, text: String, filePath: String): Unit = {
    RedisProcessor.hset(LOGIN_CAPTCHA_TEXT_FLAG, accountLoginIdOrEmailAndOrg, text)
    RedisProcessor.hset(LOGIN_CAPTCHA_FILE_FLAG, accountLoginIdOrEmailAndOrg, filePath)
  }

  def removeCaptcha(accountLoginIdOrEmailAndOrg: String): Unit = {
    RedisProcessor.hdel(LOGIN_CAPTCHA_TEXT_FLAG, accountLoginIdOrEmailAndOrg)
    val file = new File(getCaptchaFile(accountLoginIdOrEmailAndOrg))
    if (file.exists()) {
      file.delete()
    }
    RedisProcessor.hdel(LOGIN_CAPTCHA_FILE_FLAG, accountLoginIdOrEmailAndOrg)
  }

  def getCaptchaText(accountLoginIdOrEmailAndOrg: String): String = {
    RedisProcessor.hget(LOGIN_CAPTCHA_TEXT_FLAG, accountLoginIdOrEmailAndOrg, "").body
  }

  def getCaptchaFile(accountLoginIdOrEmailAndOrg: String): String = {
    RedisProcessor.hget(LOGIN_CAPTCHA_FILE_FLAG, accountLoginIdOrEmailAndOrg, "").body
  }

}
