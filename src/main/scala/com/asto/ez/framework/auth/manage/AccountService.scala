package com.asto.ez.framework.auth.manage

import com.asto.ez.framework.EZContext
import com.asto.ez.framework.auth.EZ_Account
import com.asto.ez.framework.rpc.{HTTP, RPC}
import com.asto.ez.framework.scaffold.SimpleRPCService
import com.ecfront.common.{EncryptHelper, Resp}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@RPC("/auth/manage/account/")
@HTTP
object AccountService extends SimpleRPCService[EZ_Account] {

  override protected def preSave(model: EZ_Account, context: EZContext): Future[Resp[EZ_Account]] = {
    super.preSave(model, context)
    if (model.login_id == null || model.login_id.trim.isEmpty || model.password == null || model.password.trim.isEmpty) {
      Future(Resp.badRequest("Require LoginId and password."))
    } else {
      model.password = packageEncryptPwd(model.login_id, model.password)
      Future(Resp.success(model))
    }
  }

  override protected def preUpdate(model: EZ_Account, context: EZContext): Future[Resp[EZ_Account]] = {
    super.preUpdate(model, context)
    if (model.login_id == null || model.login_id.trim.isEmpty || model.password == null || model.password.trim.isEmpty) {
      Future(Resp.badRequest("Require LoginId and password."))
    } else {
      model.password = packageEncryptPwd(model.login_id, model.password)
      Future(Resp.success(model))
    }
  }

  def packageEncryptPwd(loginId: String, password: String): String = {
    EncryptHelper.encrypt(loginId + password)
  }

}