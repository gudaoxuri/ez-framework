package com.asto.ez.framework.storage.mongo

import com.asto.ez.framework.EZContext
import com.asto.ez.framework.storage.{Page, StatusModel}
import com.ecfront.common.Resp
import io.vertx.core.json.JsonObject

import scala.concurrent.Future

trait MongoStatusModel extends MongoBaseModel with StatusModel {

  override def getEnabledByCond(condition: String = "{}", parameters: List[Any] = List(), context: EZContext = null): Future[Resp[this.type]] = {
    getByCond(appendEnabled(new JsonObject(condition)).encode(), parameters, context)
  }

  override def existEnabled(condition: String = "{}", parameters: List[Any] = List(), context: EZContext = null): Future[Resp[Boolean]] = {
    existByCond(appendEnabled(new JsonObject(condition)).encode(), parameters, context)
  }

  override def findEnabled(condition: String = "{}", parameters: List[Any] = List(), context: EZContext = null): Future[Resp[List[this.type]]] = {
    find(appendEnabled(new JsonObject(condition)).encode(), parameters, context)
  }

  override def pageEnabled(condition: String = "{}", parameters: List[Any] = List(), pageNumber: Long = 1, pageSize: Int = 10, context: EZContext = null): Future[Resp[Page[this.type]]] = {
    page(appendEnabled(new JsonObject(condition)).encode(), parameters, pageNumber, pageSize, context)
  }

  override def countEnabled(condition: String = "{}", parameters: List[Any] = List(), context: EZContext = null): Future[Resp[Long]] = {
    count(appendEnabled(new JsonObject(condition)).encode(), parameters, context)
  }

  override def enableById(id: Any, context: EZContext = null): Future[Resp[Void]] = {
    updateByCond(s"""{"$$set":{"enable":true}}""",s"""{"_id":"$id"}""", List(id), context)
  }

  override def disableById(id: Any, context: EZContext = null): Future[Resp[Void]] = {
    updateByCond(s"""{"$$set":{"enable":false}}""",s"""{"_id":"$id"}""", List(id), context)
  }

  private def appendEnabled(condition: JsonObject): JsonObject = {
    condition.put(StatusModel.ENABLE_FLAG, true)
  }

}








