package com.ecfront.ez.framework.module.auth.manage

import com.ecfront.common.{Req, Resp}
import com.ecfront.ez.framework.module.auth.{EZ_Resource, LocalCacheContainer}
import com.ecfront.ez.framework.module.core.CommonUtils
import com.ecfront.ez.framework.rpc._
import com.ecfront.ez.framework.service.SyncService
import com.ecfront.ez.framework.service.protocols.JDBCService
import com.ecfront.storage.PageModel

@RPC("/auth/manage/resource/")
@HTTP
object ResourceService extends JDBCService[EZ_Resource, Req] with SyncService[EZ_Resource, Req] {

  @POST("")
  def save(parameter: Map[String, String], body: EZ_Resource, req: Option[Req]): Resp[String] = {
    _save(body, req)
  }

  @PUT(":id/")
  def update(parameter: Map[String, String], body: EZ_Resource, req: Option[Req]): Resp[String] = {
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
  def page(parameter: Map[String, String], req: Option[Req]): Resp[PageModel[EZ_Resource]] = {
    val (orderSql, orderParams) = CommonUtils.packageOrder(parameter)
    if (orderSql.nonEmpty) {
      _pageByCondition(orderSql, Some(orderParams), parameter("number").toInt, parameter("size").toInt, req)
    } else {
      _pageAll(parameter("number").toInt, parameter("size").toInt, req)
    }
  }

  @GET("")
  def find(parameter: Map[String, String], req: Option[Req]): Resp[List[EZ_Resource]] = {
    val (orderSql, orderParams) = CommonUtils.packageOrder(parameter)
    if (orderSql.nonEmpty) {
      _findByCondition(orderSql, Some(orderParams), req)
    } else {
      _findAll(req)
    }
  }

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
