package com.ecfront.ez.framework.module.auth.manage

import com.ecfront.common.Resp
import com.ecfront.ez.framework.module.auth.Role
import com.ecfront.ez.framework.module.core.{CommonUtils, EZReq}
import com.ecfront.ez.framework.rpc._
import com.ecfront.ez.framework.service.SyncService
import com.ecfront.ez.framework.service.protocols.JDBCService
import com.ecfront.storage.PageModel

@RPC("/auth/manage/role/")
@HTTP
object RoleService extends JDBCService[Role, EZReq] with SyncService[Role, EZReq] {

  @POST("")
  def save(parameter: Map[String, String], body: Role, req: Option[EZReq]): Resp[String] = {
    _save(body, req)
  }

  @PUT(":id/")
  def update(parameter: Map[String, String], body: Role, req: Option[EZReq]): Resp[String] = {
    _update(parameter("id"), body, req)
  }

  @DELETE(":id/")
  def delete(parameter: Map[String, String], req: Option[EZReq]): Resp[String] = {
    _deleteById(parameter("id"), req)
  }

  @GET(":id/")
  def get(parameter: Map[String, String], req: Option[EZReq]): Resp[String] = {
    _getById(parameter("id"), req)
  }

  @GET("page/:number/:size/")
  def page(parameter: Map[String, String], req: Option[EZReq]): Resp[PageModel[Role]] = {
    val (orderSql, orderParams) = CommonUtils.packageOrder(parameter)
    if (orderSql.nonEmpty) {
      _pageByCondition(orderSql, Some(orderParams), parameter("number").toInt, parameter("size").toInt, req)
    } else {
      _pageAll(parameter("number").toInt, parameter("size").toInt, req)
    }
  }

  @GET("")
  def find(parameter: Map[String, String], req: Option[EZReq]): Resp[List[Role]] = {
    val (orderSql, orderParams) = CommonUtils.packageOrder(parameter)
    if (orderSql.nonEmpty) {
      _findByCondition(orderSql, Some(orderParams), req)
    } else {
      _findAll(req)
    }
  }

  override protected def _preSave(model: Role, request: Option[EZReq]): Resp[Any] = {
    if (model.id == null || model.id.trim.isEmpty) {
      Resp.badRequest("Require Code.")
    } else {
      Resp.success(model)
    }
  }

}
