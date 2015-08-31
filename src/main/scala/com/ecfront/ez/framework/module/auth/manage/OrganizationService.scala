package com.ecfront.ez.framework.module.auth.manage

import com.ecfront.common.{Req, Resp}
import com.ecfront.ez.framework.module.auth.Organization
import com.ecfront.ez.framework.module.core.CommonUtils
import com.ecfront.ez.framework.rpc._
import com.ecfront.ez.framework.service.SyncService
import com.ecfront.ez.framework.service.protocols.JDBCService
import com.ecfront.storage.PageModel

@RPC("/auth/manage/organization/")
@HTTP
object OrganizationService extends JDBCService[Organization, Req] with SyncService[Organization, Req] {

  @POST("")
  def save(parameter: Map[String, String], body: Organization, req: Option[Req]): Resp[String] = {
    _save(body, req)
  }

  @PUT(":id/")
  def update(parameter: Map[String, String], body: Organization, req: Option[Req]): Resp[String] = {
    _update(parameter("id"), body, req)
  }

  @DELETE(":id/")
  def delete(parameter: Map[String, String], req: Option[Req]): Resp[String] = {
    _deleteById(parameter("id"), req)
  }

  @GET(":id/")
  def get(parameter: Map[String, String], req: Option[Req]): Resp[Organization] = {
    _getById(parameter("id"), req)
  }

  @GET("page/:number/:size/")
  def page(parameter: Map[String, String], req: Option[Req]): Resp[PageModel[Organization]] = {
    val (orderSql, orderParams) = CommonUtils.packageOrder(parameter)
    if (orderSql.nonEmpty) {
      _pageByCondition(orderSql, Some(orderParams), parameter("number").toInt, parameter("size").toInt, req)
    } else {
      _pageAll(parameter("number").toInt, parameter("size").toInt, req)
    }
  }

  @GET("")
  def find(parameter: Map[String, String], req: Option[Req]): Resp[List[Organization]] = {
    val (orderSql, orderParams) = CommonUtils.packageOrder(parameter)
    if (orderSql.nonEmpty) {
      _findByCondition(orderSql, Some(orderParams), req)
    } else {
      _findAll(req)
    }
  }

}