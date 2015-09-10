package com.ecfront.ez.framework.module.auth.manage

import com.ecfront.common.{Req, Resp}
import com.ecfront.ez.framework.module.auth.{EZ_Role, LocalCacheContainer}
import com.ecfront.ez.framework.module.core.CommonUtils
import com.ecfront.ez.framework.rpc._
import com.ecfront.ez.framework.service.SyncService
import com.ecfront.ez.framework.service.protocols.JDBCService
import com.ecfront.ez.framework.storage.PageModel

@RPC("/auth/manage/role/")
@HTTP
object RoleService extends JDBCService[EZ_Role, Req] with SyncService[EZ_Role, Req] {

  @POST("")
  def save(parameter: Map[String, String], body: EZ_Role, req: Option[Req]): Resp[String] = {
    _save(body, req)
  }

  @PUT(":id/")
  def update(parameter: Map[String, String], body: EZ_Role, req: Option[Req]): Resp[String] = {
    _update(parameter("id"), body, req)
  }

  @DELETE(":id/")
  def delete(parameter: Map[String, String], req: Option[Req]): Resp[String] = {
    _deleteById(parameter("id"), req)
  }

  @GET(":id/")
  def get(parameter: Map[String, String], req: Option[Req]): Resp[String] = {
    _getById(parameter("id"), req)
  }

  @GET("page/:number/:size/")
  def page(parameter: Map[String, String], req: Option[Req]): Resp[PageModel[EZ_Role]] = {
    val (orderSql, orderParams) = CommonUtils.packageOrder(parameter)
    if (orderSql.nonEmpty) {
      _pageByCondition(orderSql, Some(orderParams), parameter("number").toInt, parameter("size").toInt, req)
    } else {
      _pageAll(parameter("number").toInt, parameter("size").toInt, req)
    }
  }

  @GET("")
  def find(parameter: Map[String, String], req: Option[Req]): Resp[List[EZ_Role]] = {
    val (orderSql, orderParams) = CommonUtils.packageOrder(parameter)
    if (orderSql.nonEmpty) {
      _findByCondition(orderSql, Some(orderParams), req)
    } else {
      _findAll(req)
    }
  }

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
