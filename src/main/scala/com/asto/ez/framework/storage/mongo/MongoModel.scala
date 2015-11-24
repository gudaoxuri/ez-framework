package com.asto.ez.framework.storage.mongo

import com.asto.ez.framework.EZContext
import com.asto.ez.framework.storage.Page
import com.ecfront.common.{JsonHelper, Resp}
import io.vertx.core.json.JsonObject

import scala.concurrent.Future

trait MongoModel extends Serializable {

  protected val _modelClazz = this.getClass
  protected val _tableName = _modelClazz.getSimpleName.toLowerCase

  def getTableName = _tableName

  def save(context: EZContext = null): Future[Resp[String]] = {
    MongoHelper.save(_tableName, new JsonObject(JsonHelper.toJsonString(this)))
  }

  def update(query: JsonObject, update: JsonObject, context: EZContext = null): Future[Resp[Void]] = {
    MongoHelper.update(_tableName, query, update)
  }

  def updateById(id: String, update: JsonObject, context: EZContext = null): Future[Resp[Void]] = {
    MongoHelper.updateById(_tableName, id, update)
  }

  def deleteById(id: String, context: EZContext = null): Future[Resp[Void]] = {
    MongoHelper.deleteById(_tableName, id)
  }

  def delete(query: JsonObject, context: EZContext = null): Future[Resp[Void]] = {
    MongoHelper.delete(_tableName, query)
  }

  def getById(id: String, context: EZContext = null): Future[Resp[this.type]] = {
    MongoHelper.getById(_tableName, id)
  }

  def find(query: JsonObject, sort: JsonObject = null, context: EZContext = null): Future[Resp[List[this.type]]] = {
    MongoHelper.find(_tableName, query, sort)
  }

  def page(query: JsonObject, pageNumber: Long = 1, pageSize: Int = 10, sort: JsonObject = null, context: EZContext = null): Future[Resp[Page[this.type]]] = {
    MongoHelper.page(_tableName, query, pageNumber, pageSize, sort)
  }

  def count(query: JsonObject, context: EZContext = null): Future[Resp[Long]] = {
    MongoHelper.count(_tableName, query)
  }

}

object MongoModel {

}
