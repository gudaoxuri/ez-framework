package com.ecfront.ez.framework.service.auth

import com.ecfront.common.BeanHelper
import com.ecfront.ez.framework.service.rpc.foundation.EZRPCContext
import com.ecfront.ez.framework.service.storage.foundation.EZStorageContext

import scala.beans.BeanProperty

class EZAuthContext extends EZRPCContext {
  @BeanProperty  var token: Option[String] = None
  @BeanProperty  var loginInfo: Option[EZ_Token_Info] = None
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
      EZStorageContext(loginInfo.login_id, loginInfo.organization.getCode)
    } else {
      EZStorageContext()
    }
  }

}
