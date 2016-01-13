package com.asto.ez.framework.storage.mongo

import com.asto.ez.framework.EZContext
import com.asto.ez.framework.storage.{Page, StatusModel, StatusStorage}
import com.ecfront.common.Resp
import io.vertx.core.json.JsonObject

import scala.concurrent.Future

trait MongoStatusStorage[M <: StatusModel] extends MongoBaseStorage[M] with StatusStorage[M] {

  override def doGetEnabledByCond(condition: String, parameters: List[Any], context: EZContext): Future[Resp[M]] = {
    getByCond(appendEnabled(new JsonObject(condition)).encode(), parameters, context)
  }

  override def doExistEnabledByCond(condition: String, parameters: List[Any], context: EZContext): Future[Resp[Boolean]] = {
    existByCond(appendEnabled(new JsonObject(condition)).encode(), parameters, context)
  }

  override def doFindEnabled(condition: String, parameters: List[Any], context: EZContext): Future[Resp[List[M]]] = {
    find(appendEnabled(new JsonObject(condition)).encode(), parameters, context)
  }

  override def doPageEnabled(condition: String, parameters: List[Any], pageNumber: Long, pageSize: Int, context: EZContext): Future[Resp[Page[M]]] = {
    page(appendEnabled(new JsonObject(condition)).encode(), parameters, pageNumber, pageSize, context)
  }

  override def doCountEnabled(condition: String, parameters: List[Any], context: EZContext): Future[Resp[Long]] = {
    count(appendEnabled(new JsonObject(condition)).encode(), parameters, context)
  }

  override def doEnableById(id: Any, context: EZContext): Future[Resp[Void]] = {
    updateByCond( s"""{"$$set":{"enable":true}}""", s"""{"_id":"$id"}""", List(id), context)
  }

  override def doDisableById(id: Any, context: EZContext): Future[Resp[Void]] = {
    updateByCond( s"""{"$$set":{"enable":false}}""", s"""{"_id":"$id"}""", List(id), context)
  }

  private def appendEnabled(condition: JsonObject): JsonObject = {
    condition.put(StatusModel.ENABLE_FLAG, true)
  }

}






