package com.ecfront.ez.framework.module.auth

import com.ecfront.common.{Req, Resp}
import com.ecfront.ez.framework.module.keylog.KeyLogService
import com.ecfront.ez.framework.service.SyncService
import com.ecfront.ez.framework.service.protocols.CacheService

object TokenService extends CacheService[EZ_Token_Info, Req] with SyncService[EZ_Token_Info, Req] {

  private val maxIndate = 2592000000L //30天

  override protected def _postGetById(tokenInfo: EZ_Token_Info, preResult: Any, req: Option[Req]): Resp[EZ_Token_Info] = {
    if (tokenInfo == null) {
      Resp.unAuthorized("Token NOT exist.")
    } else {
      if ((tokenInfo.last_login_time + maxIndate) < System.currentTimeMillis()) {
        //过期
        _deleteById(tokenInfo.id, req)
        KeyLogService.unAuthorized(s"Token expired by ${tokenInfo.login_id} , token : ${tokenInfo.id}", req)
        Resp.unAuthorized("Token expired.")
      } else {
        Resp.success(tokenInfo)
      }
    }
  }
}
