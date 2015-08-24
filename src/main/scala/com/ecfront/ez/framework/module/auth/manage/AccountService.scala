package com.ecfront.ez.framework.module.auth.manage

import com.ecfront.common.{EncryptHelper, Resp, StandardCode}
import com.ecfront.ez.framework.module.auth.Account
import com.ecfront.ez.framework.module.core.EZReq
import com.ecfront.ez.framework.service.{SyncService, BasicService}
import com.ecfront.ez.framework.service.protocols.JDBCService
import com.ecfront.lego.core.component.BasicService
import com.ecfront.lego.core.foundation.{IdModelExt, LReq}
import com.ecfront.lego.rbac.foundation.Account
import com.ecfront.service.SyncService
import com.ecfront.service.protocols.JDBCService

object AccountService extends JDBCService[Account, EZReq] with SyncService[Account, EZReq] with BasicService {

  /**
   * ID检查，是否非法
   * 设置主键、密码
   */
  override protected def _preSave(model: Account, request: Option[EZReq]): Resp[Any] = {
    if (model.id == null || model.id.trim.isEmpty) {
      Resp.badRequest("Require LoginId.")
    } else {
        model.password = packageEncryptPwd(model.id, model.password)
        Resp.success(model)
    }
  }

  def packageEncryptPwd(loginId: String, password: String): String = {
    EncryptHelper.encrypt(loginId + password)
  }

}