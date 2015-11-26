package com.asto.ez.framework.storage.mongo

import com.asto.ez.framework.EZContext
import com.asto.ez.framework.storage.mongo.MongoEntityContainer.MongoEntityInfo
import com.asto.ez.framework.storage.mongo.SortEnum.SortEnum
import com.asto.ez.framework.storage.{BaseModel, Page}
import com.ecfront.common.{BeanHelper, JsonHelper, Resp}
import io.vertx.core.json.JsonObject

import scala.beans.BeanProperty
import scala.concurrent.Future

trait MongoBaseModel extends BaseModel {

  @BeanProperty var id: String = _

  protected val _entityInfo =
    if (MongoEntityContainer.CONTAINER.contains(_tableName)) {
      MongoEntityContainer.CONTAINER(_tableName)
    } else {
      MongoEntityContainer.initEntity(_modelClazz, _tableName)
      MongoEntityContainer.CONTAINER(_tableName)
    }

  override def save(context: EZContext = null): Future[Resp[String]] = {
    val save = MongoBaseModel.convertToJsonObject(_entityInfo, this)
    MongoHelper.save(_tableName, save)
  }

  override def update(context: EZContext = null): Future[Resp[String]] = {
    val update = MongoBaseModel.convertToJsonObject(_entityInfo, this)
    MongoHelper.update(_tableName, this.id, update)
  }

  override def saveOrUpdate(context: EZContext): Future[Resp[String]] = {
    val saveOrUpdate = MongoBaseModel.convertToJsonObject(_entityInfo, this)
    MongoHelper.saveOrUpdate(_tableName, saveOrUpdate)
  }

  override def getById(id: Any, context: EZContext = null): Future[Resp[this.type]] = {
    MongoHelper.getById(_tableName, id.asInstanceOf[String], _modelClazz).asInstanceOf[Future[Resp[this.type]]]
  }

  override def deleteById(id: Any, context: EZContext = null): Future[Resp[Void]] = {
    MongoHelper.deleteById(_tableName, id.asInstanceOf[String])
  }

  def updateByCond(query: JsonObject, update: JsonObject, context: EZContext = null): Future[Resp[Void]] = {
    MongoHelper.updateByCond(_tableName, query, update)
  }

  def deleteByCond(query: JsonObject = new JsonObject(), context: EZContext = null): Future[Resp[Void]] = {
    MongoHelper.deleteByCond(_tableName, query)
  }

  def find(query: JsonObject = new JsonObject(), sort: Map[String, SortEnum] = null, context: EZContext = null): Future[Resp[List[this.type]]] = {
    MongoHelper.find(_tableName, query, MongoBaseModel.convertSort(sort), _modelClazz).asInstanceOf[Future[Resp[List[this.type]]]]
  }

  def page(query: JsonObject = new JsonObject(), pageNumber: Long = 1, pageSize: Int = 10, sort: Map[String, SortEnum] = null, context: EZContext = null): Future[Resp[Page[this.type]]] = {
    MongoHelper.page(_tableName, query, pageNumber, pageSize, MongoBaseModel.convertSort(sort), _modelClazz).asInstanceOf[Future[Resp[Page[this.type]]]]
  }

  def count(query: JsonObject = new JsonObject(), context: EZContext = null): Future[Resp[Long]] = {
    MongoHelper.count(_tableName, query)
  }

}

object MongoBaseModel {

  val Id_FLAG = "id"

  protected def convertToJsonObject(entityInfo: MongoEntityInfo, model: MongoBaseModel): JsonObject = {
    new JsonObject(JsonHelper.toJsonString(BeanHelper.findValues(model, entityInfo.persistentFields)))
  }

  protected def convertSort(sort: Map[String, SortEnum]): JsonObject = {
    if (sort == null || sort.isEmpty)
      null
    else {
      val s = new JsonObject()
      sort.foreach(i => s.put(i._1, i._2.id))
      s
    }
  }

}

object SortEnum extends Enumeration {
  type SortEnum = Value
  val DESC = Value(-1)
  val ASC = Value(1)
}


