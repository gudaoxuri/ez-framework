package com.ecfront.ez.framework.service.auth

import java.io.File
import java.util.Date
import java.util.concurrent.locks.ReentrantLock

import com.ecfront.common.JsonHelper
import com.ecfront.ez.framework.core.EZ
import com.ecfront.ez.framework.core.rpc.{OptInfo, RPCProcessor}
import com.ecfront.ez.framework.service.auth.model._
import com.typesafe.scalalogging.slf4j.LazyLogging

object CacheManager extends LazyLogging {

  object Token {

    private val TOKEN_INFO_FLAG = RPCProcessor.TOKEN_INFO_FLAG
    // Token Id 关联 key : ez:auth:token:id:rel:<code> value : <token Id>
    private val TOKEN_ID_REL_FLAG = "ez:auth:token:id:rel:"

    val tokenLock = new ReentrantLock()

    def getToken(accountCode: String): String = {
      EZ.cache.get(TOKEN_ID_REL_FLAG + accountCode)
    }

    def getTokenInfo(token: String): OptInfo = {
      JsonHelper.toObject[OptInfo](EZ.cache.get(TOKEN_INFO_FLAG + token))
    }

    def removeTokenByAccountCode(accountCode: String): Unit = {
      val token = getToken(accountCode)
      if (token != null) {
        removeToken(token)
      }
    }

    def removeToken(token: String): Unit = {
      val tokenInfo = getTokenInfo(token)
      if (tokenInfo != null) {
        EZ.cache.del(TOKEN_ID_REL_FLAG + tokenInfo.accountCode)
        EZ.cache.del(TOKEN_INFO_FLAG + token)
      }
    }

    def addToken(account: EZ_Account, org: EZ_Organization): OptInfo = {
      // 加锁，避免在多线程下`TOKEN_ID_REL_FLAG + account.code`竞争问题
      tokenLock.lock()
      removeToken(account.code)
      val newTokenInfo = OptInfo(
        EZ.createUUID,
        account.code,
        account.login_id,
        account.name,
        account.email,
        account.image,
        account.organization_code,
        org.name,
        org.category,
        account.role_codes,
        new Date(),
        account.ext_id,
        account.ext_info
      )
      EZ.cache.set(TOKEN_ID_REL_FLAG + account.code, newTokenInfo.token, ServiceAdapter.loginKeepSeconds)
      EZ.cache.set(TOKEN_INFO_FLAG + newTokenInfo.token, JsonHelper.toJsonString(newTokenInfo), ServiceAdapter.loginKeepSeconds)
      tokenLock.unlock()
      newTokenInfo
    }

    def updateTokenInfo(account: EZ_Account): Unit = {
      val token = getToken(account.code)
      if (token != null) {
        val oldTokenInfo = getTokenInfo(token)
        if (oldTokenInfo == null) {
          // 在某些情况下（如缓存被清空）可能存在原token信息不存在，此时要求重新登录
          removeToken(token)
        } else {
          val newTokenInfo = OptInfo(
            token,
            account.code,
            account.login_id,
            account.name,
            account.email,
            account.image,
            oldTokenInfo.organizationCode,
            oldTokenInfo.organizationName,
            oldTokenInfo.organizationCategory,
            account.role_codes,
            oldTokenInfo.lastLoginTime,
            account.ext_id,
            account.ext_info
          )
          EZ.cache.set(TOKEN_INFO_FLAG + newTokenInfo.token, JsonHelper.toJsonString(newTokenInfo), ServiceAdapter.loginKeepSeconds)
        }
      }
    }

  }

  object RBAC {

    def initOrganization(org: EZ_Organization): Unit = {
      Initiator.initOrganization(org)
    }


    def addOrganization(org: EZ_Organization): Unit = {
      EZ.eb.pubReq(ServiceAdapter.EB_ORG_ADD_FLAG, org)
    }

    def removeOrganization(code: String): Unit = {
      EZ.eb.pubReq(ServiceAdapter.EB_ORG_REMOVE_FLAG, Map("code" -> code))
    }

    def addResource(res: EZ_Resource): Unit = {
      EZ.eb.pubReq(ServiceAdapter.EB_RESOURCE_ADD_FLAG, res)
    }

    def removeResource(code: String): Unit = {
      EZ.eb.pubReq(ServiceAdapter.EB_RESOURCE_REMOVE_FLAG, Map("code" -> code))
    }

    def addRole(role: EZ_Role): Unit = {
      EZ.eb.pubReq(ServiceAdapter.EB_ROLE_ADD_FLAG, role)
    }

    def removeRole(code: String): Unit = {
      EZ.eb.pubReq(ServiceAdapter.EB_ROLE_REMOVE_FLAG, Map("code" -> code))
    }

  }

  object Login {
    // 连续登录错误次数
    private val LOGIN_ERROR_TIMES_FLAG = "ez:auth:login:error:times:"
    // 登录验证码的字符
    private val LOGIN_CAPTCHA_TEXT_FLAG = "ez:auth:login:captcha:text"
    // 登录验证码的文件路径
    private val LOGIN_CAPTCHA_FILE_FLAG = "ez:auth:login:captcha:file"

    def addLoginErrorTimes(accountLoginIdOrEmailAndOrg: String): Long = {
      EZ.cache.incr(LOGIN_ERROR_TIMES_FLAG + accountLoginIdOrEmailAndOrg)
    }

    def removeLoginErrorTimes(accountLoginIdOrEmailAndOrg: String): Unit = {
      EZ.cache.del(LOGIN_ERROR_TIMES_FLAG + accountLoginIdOrEmailAndOrg)
    }

    def getLoginErrorTimes(accountLoginIdOrEmailAndOrg: String): Long = {
      EZ.cache.incr(LOGIN_ERROR_TIMES_FLAG + accountLoginIdOrEmailAndOrg, 0)
    }

    def addCaptcha(accountLoginIdOrEmailAndOrg: String, text: String, filePath: String): Unit = {
      EZ.cache.hset(LOGIN_CAPTCHA_TEXT_FLAG, accountLoginIdOrEmailAndOrg, text)
      EZ.cache.hset(LOGIN_CAPTCHA_FILE_FLAG, accountLoginIdOrEmailAndOrg, filePath)
    }

    def removeCaptcha(accountLoginIdOrEmailAndOrg: String): Unit = {
      EZ.cache.hdel(LOGIN_CAPTCHA_TEXT_FLAG, accountLoginIdOrEmailAndOrg)
      val file = getCaptchaFile(accountLoginIdOrEmailAndOrg)
      if (file != null && new File(file).exists()) {
        new File(file).delete()
      }
      EZ.cache.hdel(LOGIN_CAPTCHA_FILE_FLAG, accountLoginIdOrEmailAndOrg)
    }

    def getCaptchaText(accountLoginIdOrEmailAndOrg: String): String = {
      EZ.cache.hget(LOGIN_CAPTCHA_TEXT_FLAG, accountLoginIdOrEmailAndOrg)
    }

    def getCaptchaFile(accountLoginIdOrEmailAndOrg: String): String = {
      EZ.cache.hget(LOGIN_CAPTCHA_FILE_FLAG, accountLoginIdOrEmailAndOrg)
    }

  }

}
