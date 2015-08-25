package com.ecfront.ez.framework.module.auth.manage

import com.ecfront.common.Resp
import com.ecfront.ez.framework.module.auth.Resource
import com.ecfront.ez.framework.module.core.{CommonUtils, EZReq}
import com.ecfront.ez.framework.rpc._
import com.ecfront.ez.framework.service.SyncService
import com.ecfront.ez.framework.service.protocols.JDBCService
import com.ecfront.storage.PageModel

@RPC("/auth/manage/resource/")
@HTTP
object ResourceService extends JDBCService[Resource, EZReq] with SyncService[Resource, EZReq] {

  @POST("")
  def save(parameter: Map[String, String], body: Resource, req: Option[EZReq]): Resp[String] = {
    _save(body, req)
  }

  @PUT(":id/")
  def update(parameter: Map[String, String], body: Resource, req: Option[EZReq]): Resp[String] = {
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
  def page(parameter: Map[String, String], req: Option[EZReq]): Resp[PageModel[Resource]] = {
    val (orderSql, orderParams) = CommonUtils.packageOrder(parameter)
    if (orderSql.nonEmpty) {
      _pageByCondition(orderSql, Some(orderParams), parameter("number").toInt, parameter("size").toInt, req)
    } else {
      _pageAll(parameter("number").toInt, parameter("size").toInt, req)
    }
  }

  @GET("")
  def find(parameter: Map[String, String], req: Option[EZReq]): Resp[List[Resource]] = {
    val (orderSql, orderParams) = CommonUtils.packageOrder(parameter)
    if (orderSql.nonEmpty) {
      _findByCondition(orderSql, Some(orderParams), req)
    } else {
      _findAll(req)
    }
  }

  override protected def _preSave(model: Resource, request: Option[EZReq]): Resp[Any] = {
    if (model.id == null || model.id.trim.isEmpty) {
      Resp.badRequest("Require method and uri.")
    } else {
      Resp.success(model)
    }
  }

}
