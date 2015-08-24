package com.ecfront.ez.framework.module.auth

import com.ecfront.common.Resp
import com.ecfront.ez.framework.module.core.EZReq
import com.ecfront.ez.framework.module.keylog.KeyLogService
import com.ecfront.ez.framework.service.protocols.CacheService
import com.ecfront.ez.framework.service.{BasicService, SyncService}

object TokenService extends CacheService[TokenInfo, EZReq] with SyncService[TokenInfo, EZReq] with BasicService {

  private val maxIndate = 2592000000L //30天

  override protected def _postGetById(tokenInfo: TokenInfo, preResult: Any, req: Option[EZReq]): Resp[EZReq] = {
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
