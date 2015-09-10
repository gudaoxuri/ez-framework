package com.ecfront.ez.framework.module.auth.manage

import com.ecfront.common.{EncryptHelper, Req, Resp}
import com.ecfront.ez.framework.module.SimpleRPCService
import com.ecfront.ez.framework.module.auth.EZ_Account
import com.ecfront.ez.framework.rpc._
import com.ecfront.ez.framework.service.protocols.JDBCService

@RPC("/auth/manage/account/")
@HTTP
object AccountService extends SimpleRPCService[EZ_Account, Req] with JDBCService[EZ_Account, Req] {

  /**
   * ID检查，是否非法
   * 设置主键、密码
   */
  override protected def _preSave(model: EZ_Account, request: Option[Req]): Resp[Any] = {
    if (model.id == null || model.id.trim.isEmpty) {
      Resp.badRequest("Require LoginId.")
    } else {
      model.password = packageEncryptPwd(model.id, model.password)
      Resp.success(model)
    }
  }

  override protected def _preUpdate(id: String, model: EZ_Account, request: Option[Req]): Resp[Any] = {
    model.password = packageEncryptPwd(model.id, model.password)
    Resp.success(model)
  }

  def packageEncryptPwd(loginId: String, password: String): String = {
    EncryptHelper.encrypt(loginId + password)
  }

}