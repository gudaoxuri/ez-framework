package com.asto.ez.framework.storage.mongo

import com.asto.ez.framework.EZContext
import com.asto.ez.framework.storage.mongo.SortEnum.SortEnum
import com.asto.ez.framework.storage.{BaseModel, BaseStorage, Page}
import com.ecfront.common.{BeanHelper, JsonHelper, Resp}
import io.vertx.core.json.{JsonArray, JsonObject}

import scala.beans.BeanProperty
import scala.concurrent.Future

trait MongoBaseModel extends BaseModel {

  @BeanProperty var id: String = _

}

object MongoBaseModel {

  val Id_FLAG = "id"

}

trait MongoBaseStorage[M <: MongoBaseModel] extends BaseStorage[M] {

  protected val _entityInfo =
    if (MongoEntityContainer.CONTAINER.contains(tableName)) {
      MongoEntityContainer.CONTAINER(tableName)
    } else {
      MongoEntityContainer.buildingEntityInfo(_modelClazz, tableName)
      MongoEntityContainer.CONTAINER(tableName)
    }

  override def doSave(model: M, context: EZContext): Future[Resp[String]] = {
    val save = convertToJsonObject(_entityInfo, model)
    MongoExecutor.save(_entityInfo, tableName, save)
  }

  override def doUpdate(model: M, context: EZContext): Future[Resp[String]] = {
    val update = convertToJsonObject(_entityInfo, model)
    MongoExecutor.update(_entityInfo, tableName, model.id, update)
  }

  override def doSaveOrUpdate(model: M, context: EZContext): Future[Resp[String]] = {
    val saveOrUpdate = convertToJsonObject(_entityInfo, model)
    MongoExecutor.saveOrUpdate(_entityInfo, tableName, model.id, saveOrUpdate)
  }

  override def doUpdateByCond(newValues: String, condition: String, parameters: List[Any], context: EZContext): Future[Resp[Void]] = {
    MongoProcessor.updateByCond(tableName, new JsonObject(condition), new JsonObject(newValues))
  }

  override def doGetById(id: Any, context: EZContext): Future[Resp[M]] = {
    MongoProcessor.getById(tableName, id.asInstanceOf[String], _modelClazz)
  }

  override def doGetByCond(condition: String, parameters: List[Any], context: EZContext): Future[Resp[M]] = {
    MongoProcessor.getByCond(tableName, new JsonObject(condition), _modelClazz)
  }

  override def doDeleteById(id: Any, context: EZContext): Future[Resp[Void]] = {
    MongoProcessor.deleteById(tableName, id.asInstanceOf[String])
  }

  override def doDeleteByCond(condition: String, parameters: List[Any], context: EZContext): Future[Resp[Void]] = {
    MongoProcessor.deleteByCond(tableName, new JsonObject(condition))
  }

  override def doCount(condition: String, parameters: List[Any], context: EZContext): Future[Resp[Long]] = {
    MongoProcessor.count(tableName, new JsonObject(condition))
  }

  override def doExistById(id: Any, context: EZContext): Future[Resp[Boolean]] = {
    MongoProcessor.exist(tableName, new JsonObject().put("_id", id))
  }

  override def doExistByCond(condition: String, parameters: List[Any], context: EZContext): Future[Resp[Boolean]] = {
    MongoProcessor.exist(tableName, new JsonObject(condition))
  }

  override def doFind(condition: String, parameters: List[Any], context: EZContext): Future[Resp[List[M]]] = {
    MongoProcessor.find(tableName, new JsonObject(condition), null, 0, _modelClazz)
  }

  override def doPage(condition: String, parameters: List[Any], pageNumber: Long, pageSize: Int, context: EZContext): Future[Resp[Page[M]]] = {
    MongoProcessor.page(tableName, new JsonObject(condition), pageNumber, pageSize, null, _modelClazz)
  }

  def findWithOpt(condition: String = "{}", sort: Map[String, SortEnum], limit: Int = 0, context: EZContext = null): Future[Resp[List[M]]] = {
    MongoProcessor.find(tableName, new JsonObject(condition), convertSort(sort), limit, _modelClazz)
  }

  def pageWithOpt(condition: String = "{}", pageNumber: Long = 1, pageSize: Int = 10, sort: Map[String, SortEnum] = Map(), context: EZContext = null): Future[Resp[Page[M]]] = {
    MongoProcessor.page(tableName, new JsonObject(condition), pageNumber, pageSize, convertSort(sort), _modelClazz)
  }

  def aggregate(query: JsonArray, context: EZContext = null): Future[Resp[JsonArray]] = {
    MongoProcessor.aggregate(tableName, query)
  }

  protected def convertToJsonObject(entityInfo: MongoEntityInfo, model: M): JsonObject = {
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


