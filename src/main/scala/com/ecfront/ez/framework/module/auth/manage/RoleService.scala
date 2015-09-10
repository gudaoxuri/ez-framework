package com.ecfront.ez.framework.module.auth.manage

import com.ecfront.common.{Req, Resp}
import com.ecfront.ez.framework.module.SimpleRPCService
import com.ecfront.ez.framework.module.auth.{EZ_Role, LocalCacheContainer}
import com.ecfront.ez.framework.rpc._
import com.ecfront.ez.framework.service.protocols.JDBCService

@RPC("/auth/manage/role/")
@HTTP
object RoleService extends SimpleRPCService[EZ_Role, Req] with JDBCService[EZ_Role, Req] {

  override protected def _preSave(model: EZ_Role, request: Option[Req]): Resp[Any] = {
    if (model.id == null || model.id.trim.isEmpty) {
      Resp.badRequest("Require Code.")
    } else {
      Resp.success(model)
    }
  }

  override protected def _postSave(result: String, preResult: Any, request: Option[Req]): Resp[String] = {
    val role = __getById(result).get
    LocalCacheContainer.addRole(role.id, role.resource_ids.keySet)
    super._postSave(result, preResult, request)
  }

  override protected def _postUpdate(result: String, preResult: Any, request: Option[Req]): Resp[String] = {
    val role = __getById(result).get
    LocalCacheContainer.addRole(role.id, role.resource_ids.keySet)
    super._postUpdate(result, preResult, request)
  }

  override protected def _postDeleteById(result: String, preResult: Any, request: Option[Req]): Resp[String] = {
    LocalCacheContainer.removeRole(result)
    super._postDeleteById(result, preResult, request)
  }

  override protected def _postDeleteByCondition(result: List[String], preResult: Any, request: Option[Req]): Resp[List[String]] = {
    result.foreach(LocalCacheContainer.removeRole)
    super._postDeleteByCondition(result, preResult, request)
  }

  override protected def _postDeleteAll(result: List[String], preResult: Any, request: Option[Req]): Resp[List[String]] = {
    LocalCacheContainer.removeAllRole()
    super._postDeleteAll(result, preResult, request)
  }

}
