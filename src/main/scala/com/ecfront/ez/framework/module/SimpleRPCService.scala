package com.ecfront.ez.framework.module

import com.ecfront.common.{JsonHelper, Req, Resp}
import com.ecfront.ez.framework.module.core.CommonUtils
import com.ecfront.ez.framework.rpc.{DELETE, GET, POST, PUT}
import com.ecfront.ez.framework.service.SyncService
import com.ecfront.ez.framework.storage.{IdModel, PageModel}

trait SimpleRPCService[M <: IdModel, R <: Req] extends SyncService[M, R] {

  @POST("")
  def _rpc_save(parameter: Map[String, String], body: String, req: Option[R]): Resp[String] = {
    _save(JsonHelper.toObject(body, _modelClazz), req)
  }

  @PUT(":id/")
  def _rpc_update(parameter: Map[String, String], body: String, req: Option[R]): Resp[String] = {
    _update(parameter("id"), JsonHelper.toObject(body, _modelClazz), req)
  }

  @DELETE(":id/")
  def _rpc_delete(parameter: Map[String, String], req: Option[R]): Resp[String] = {
    _deleteById(parameter("id"), req)
  }

  @GET(":id/")
  def _rpc_get(parameter: Map[String, String], req: Option[R]): Resp[M] = {
    _getById(parameter("id"), req)
  }

  @GET(":id/enable/")
  def _rpc_enable(parameter: Map[String, String], req: Option[R]): Resp[M] = {
    _enable(parameter("id"), req)
  }

  @GET(":id/disable/")
  def _rpc_disable(parameter: Map[String, String], req: Option[R]): Resp[M] = {
    _disable(parameter("id"), req)
  }

  @GET("page/:pageNumber/:pageSize/")
  def _rpc_page(parameter: Map[String, String], req: Option[R]): Resp[PageModel[M]] = {
    val (sql, params) = CommonUtils.packageSql(parameter)
    if (sql.nonEmpty) {
      _pageByCondition(sql, Some(params), parameter(PageModel.PAGE_NUMBER_FLAG).toInt, parameter(PageModel.PAGE_SIZE_FLAG).toInt, req)
    } else {
      _pageAll(parameter(PageModel.PAGE_NUMBER_FLAG).toInt, parameter(PageModel.PAGE_SIZE_FLAG).toInt, req)
    }
  }

  @GET("")
  def _rpc_find(parameter: Map[String, String], req: Option[R]): Resp[List[M]] = {
    val (sql, params) = CommonUtils.packageSql(parameter)
    if (sql.nonEmpty) {
      _findByCondition(sql, Some(params), req)
    } else {
      _findAll(req)
    }
  }

  @POST("upload/")
  def _rpc_upload(parameter: Map[String, String],body:String, req: Option[R]): Resp[String] = {
    Resp.success(body)
  }

}
