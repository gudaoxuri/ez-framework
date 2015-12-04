package com.asto.ez.framework.storage.mongo

import com.asto.ez.framework.EZContext
import com.asto.ez.framework.storage.mongo.MongoEntityContainer.MongoEntityInfo
import com.asto.ez.framework.storage.mongo.SortEnum.SortEnum
import com.asto.ez.framework.storage.{BaseModel, Page}
import com.ecfront.common.{BeanHelper, JsonHelper, Resp}
import io.vertx.core.json.{JsonArray, JsonObject}

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
    MongoProcessor.save(_tableName, save)
  }

  override def update(context: EZContext = null): Future[Resp[String]] = {
    val update = MongoBaseModel.convertToJsonObject(_entityInfo, this)
    MongoProcessor.update(_tableName, this.id, update)
  }

  override def saveOrUpdate(context: EZContext = null): Future[Resp[String]] = {
    val saveOrUpdate = MongoBaseModel.convertToJsonObject(_entityInfo, this)
    MongoProcessor.saveOrUpdate(_tableName, saveOrUpdate)
  }

  override def updateByCond(newValues: String, condition: String, parameters: List[Any] = List(), context: EZContext = null): Future[Resp[Void]] = {
    MongoProcessor.updateByCond(_tableName, new JsonObject(condition), new JsonObject(newValues))
  }

  override def getById(id: Any, context: EZContext = null): Future[Resp[this.type]] = {
    MongoProcessor.getById(_tableName, id.asInstanceOf[String], _modelClazz).asInstanceOf[Future[Resp[this.type]]]
  }

  override def getByCond(condition: String, parameters: List[Any] = List(), context: EZContext = null): Future[Resp[MongoBaseModel.this.type]] = {
    MongoProcessor.getByCond(_tableName, new JsonObject(condition), _modelClazz).asInstanceOf[Future[Resp[this.type]]]
  }

  override def deleteById(id: Any, context: EZContext = null): Future[Resp[Void]] = {
    MongoProcessor.deleteById(_tableName, id.asInstanceOf[String])
  }

  override def deleteByCond(condition: String, parameters: List[Any] = List(), context: EZContext = null): Future[Resp[Void]] = {
    MongoProcessor.deleteByCond(_tableName, new JsonObject(condition))
  }

  override def count(condition: String = "{}", parameters: List[Any] = List(), context: EZContext = null): Future[Resp[Long]] = {
    MongoProcessor.count(_tableName, new JsonObject(condition))
  }

  override def existById(id: Any, context: EZContext = null): Future[Resp[Boolean]] = {
    MongoProcessor.exist(_tableName, new JsonObject().put("_id", id))
  }

  override def existByCond(condition: String = "{}", parameters: List[Any] = List(), context: EZContext = null): Future[Resp[Boolean]] = {
    MongoProcessor.exist(_tableName, new JsonObject(condition))
  }

  override def find(condition: String = "{}", parameters: List[Any] = List(), context: EZContext = null): Future[Resp[List[this.type]]] = {
    MongoProcessor.find(_tableName, new JsonObject(condition), null, 0, _modelClazz).asInstanceOf[Future[Resp[List[this.type]]]]
  }

  override def page(condition: String = "{}", parameters: List[Any] = List(), pageNumber: Long = 1, pageSize: Int = 10, context: EZContext = null): Future[Resp[Page[this.type]]] = {
    MongoProcessor.page(_tableName, new JsonObject(condition), pageNumber, pageSize, null, _modelClazz).asInstanceOf[Future[Resp[Page[this.type]]]]
  }

  def findWithOpt(condition: String = "{}", sort: Map[String, SortEnum] = null, limit: Int = 0, context: EZContext = null): Future[Resp[List[this.type]]] = {
    MongoProcessor.find(_tableName, new JsonObject(condition), MongoBaseModel.convertSort(sort), limit, _modelClazz).asInstanceOf[Future[Resp[List[this.type]]]]
  }

  def pageWithOpt(condition: String = "{}", pageNumber: Long = 1, pageSize: Int = 10, sort: Map[String, SortEnum] = null, context: EZContext = null): Future[Resp[Page[this.type]]] = {
    MongoProcessor.page(_tableName, new JsonObject(condition), pageNumber, pageSize, MongoBaseModel.convertSort(sort), _modelClazz).asInstanceOf[Future[Resp[Page[this.type]]]]
  }

  def aggregate(query: JsonArray, context: EZContext = null): Future[Resp[JsonArray]] = {
    MongoProcessor.aggregate(_tableName, query)
  }

}

object MongoBaseModel {

  val Id_FLAG = "id"

  protected def convertToJsonObject(entityInfo: MongoEntityInfo, model: MongoBaseModel): JsonObject = {
    new JsonObject(JsonHelper.toJsonString(BeanHelper.findValues(model)))
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


