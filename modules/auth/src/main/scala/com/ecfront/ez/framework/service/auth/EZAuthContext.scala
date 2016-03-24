package com.ecfront.ez.framework.service.auth

import com.ecfront.common.BeanHelper
import com.ecfront.ez.framework.service.rpc.foundation.EZRPCContext
import com.ecfront.ez.framework.service.storage.foundation.EZStorageContext

import scala.beans.BeanProperty
import scala.language.implicitConversions

/**
  * RPC上下文，带权限信息
  */
class EZAuthContext extends EZRPCContext {
  // 请求token
  @BeanProperty var token: Option[String] = None
  // 登录信息
  @BeanProperty var loginInfo: Option[Token_Info_VO] = None
}

object EZAuthContext {

  implicit def toAuthContext(rpcContext: EZRPCContext): EZAuthContext = {
    val auth = new EZAuthContext()
    BeanHelper.copyProperties(auth, rpcContext)
    auth
  }

  implicit def toStorageContext(authContext: EZAuthContext): EZStorageContext = {
    if (authContext.loginInfo.isDefined) {
      val loginInfo = authContext.loginInfo.get
      EZStorageContext(loginInfo.login_id, loginInfo.organization_code)
    } else {
      EZStorageContext()
    }
  }

}
