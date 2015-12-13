package com.asto.ez.framework.auth.manage

import com.asto.ez.framework.EZContext
import com.asto.ez.framework.auth.EZ_Resource
import com.asto.ez.framework.rpc.{HTTP, RPC}
import com.asto.ez.framework.scaffold.SimpleRPCService
import com.ecfront.common.Resp

import scala.concurrent.Future

@RPC("/auth/manage/resource/")
@HTTP
object ResourceService extends SimpleRPCService[EZ_Resource] {
  override protected def preSave(model: EZ_Resource, context: EZContext): Future[Resp[EZ_Resource]] = {
    if (model.method == null || model.method.trim.isEmpty || model.uri == null || model.uri.trim.isEmpty) {
      Future(Resp.badRequest("Require【method】and【uri】"))
    } else {
      model.code = model.method + EZ_Resource.SPLIT + model.uri
      Future(Resp.success(model))
    }
  }

  override protected def preUpdate(model: EZ_Resource, context: EZContext): Future[Resp[EZ_Resource]] = {
    if (model.method == null || model.method.trim.isEmpty || model.uri == null || model.uri.trim.isEmpty) {
      Future(Resp.badRequest("Require【method】and【uri】"))
    } else {
      model.code = model.method + EZ_Resource.SPLIT + model.uri
      Future(Resp.success(model))
    }
  }
}