package com.ecfront.ez.framework.module.auth.manage

import com.ecfront.common.{Req, Resp}
import com.ecfront.ez.framework.module.SimpleRPCService
import com.ecfront.ez.framework.module.auth.{EZ_Resource, LocalCacheContainer}
import com.ecfront.ez.framework.rpc._
import com.ecfront.ez.framework.service.protocols.JDBCService

@RPC("/auth/manage/resource/")
@HTTP
object ResourceService extends SimpleRPCService[EZ_Resource, Req] with JDBCService[EZ_Resource, Req] {

  override protected def _preSave(model: EZ_Resource, request: Option[Req]): Resp[Any] = {
    if (model.id == null || model.id.trim.isEmpty) {
      Resp.badRequest("Require method and uri.")
    } else {
      Resp.success(model)
    }
  }

  override protected def _postSave(result: String, preResult: Any, request: Option[Req]): Resp[String] = {
    val resource = __getById(result).get
    LocalCacheContainer.addResource(resource.id)
    super._postSave(result, preResult, request)
  }

  override protected def _postUpdate(result: String, preResult: Any, request: Option[Req]): Resp[String] = {
    val resource = __getById(result).get
    LocalCacheContainer.addResource(resource.id)
    super._postUpdate(result, preResult, request)
  }

  override protected def _postDeleteById(result: String, preResult: Any, request: Option[Req]): Resp[String] = {
    LocalCacheContainer.removeResource(result)
    super._postDeleteById(result, preResult, request)
  }

  override protected def _postDeleteByCondition(result: List[String], preResult: Any, request: Option[Req]): Resp[List[String]] = {
    result.foreach(LocalCacheContainer.removeResource)
    super._postDeleteByCondition(result, preResult, request)
  }

  override protected def _postDeleteAll(result: List[String], preResult: Any, request: Option[Req]): Resp[List[String]] = {
    LocalCacheContainer.removeAllResource()
    super._postDeleteAll(result, preResult, request)
  }

}
