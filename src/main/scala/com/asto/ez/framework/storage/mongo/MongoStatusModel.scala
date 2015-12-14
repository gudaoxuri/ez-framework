package com.asto.ez.framework.storage.mongo

import com.asto.ez.framework.EZContext
import com.asto.ez.framework.storage.{Page, StatusModel}
import com.ecfront.common.Resp
import io.vertx.core.json.JsonObject

import scala.concurrent.Future

trait MongoStatusModel extends MongoBaseModel with StatusModel {

  override def doGetEnabledByCond(condition: String, parameters: List[Any], context: EZContext): Future[Resp[this.type]] = {
    getByCond(appendEnabled(new JsonObject(condition)).encode(), parameters, context)
  }

  override def doExistEnabledByCond(condition: String, parameters: List[Any], context: EZContext): Future[Resp[Boolean]] = {
    existByCond(appendEnabled(new JsonObject(condition)).encode(), parameters, context)
  }

  override def doFindEnabled(condition: String, parameters: List[Any], context: EZContext): Future[Resp[List[this.type]]] = {
    find(appendEnabled(new JsonObject(condition)).encode(), parameters, context)
  }

  override def doPageEnabled(condition: String, parameters: List[Any], pageNumber: Long, pageSize: Int, context: EZContext): Future[Resp[Page[this.type]]] = {
    page(appendEnabled(new JsonObject(condition)).encode(), parameters, pageNumber, pageSize, context)
  }

  override def doCountEnabled(condition: String, parameters: List[Any], context: EZContext): Future[Resp[Long]] = {
    count(appendEnabled(new JsonObject(condition)).encode(), parameters, context)
  }

  override def doEnableById(id: Any, context: EZContext): Future[Resp[Void]] = {
    updateByCond(s"""{"$$set":{"enable":true}}""",s"""{"_id":"$id"}""", List(id), context)
  }

  override def doDisableById(id: Any, context: EZContext): Future[Resp[Void]] = {
    updateByCond(s"""{"$$set":{"enable":false}}""",s"""{"_id":"$id"}""", List(id), context)
  }

  private def appendEnabled(condition: JsonObject): JsonObject = {
    condition.put(StatusModel.ENABLE_FLAG, true)
  }

}








