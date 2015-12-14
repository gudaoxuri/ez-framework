package com.asto.ez.framework.storage.mongo

import com.asto.ez.framework.EZContext
import com.asto.ez.framework.storage.mongo.SortEnum.SortEnum
import com.asto.ez.framework.storage.{BaseModel, Page}
import com.ecfront.common.{BeanHelper, JsonHelper, Resp}
import io.vertx.core.json.{JsonArray, JsonObject}

import scala.beans.BeanProperty
import scala.concurrent.Future

trait MongoBaseModel extends BaseModel {

  protected def _entityInfo =
    if (MongoEntityContainer.CONTAINER.contains(getTableName)) {
      MongoEntityContainer.CONTAINER(getTableName)
    } else {
      MongoEntityContainer.buildingEntityInfo(_modelClazz, getTableName)
      MongoEntityContainer.CONTAINER(getTableName)
    }

  @BeanProperty var id: String = _

  override def doSave(context: EZContext): Future[Resp[String]] = {
    val save = MongoBaseModel.convertToJsonObject(_entityInfo, this)
    MongoExecutor.save(_entityInfo, getTableName, save)
  }

  override def doUpdate(context: EZContext): Future[Resp[String]] = {
    val update = MongoBaseModel.convertToJsonObject(_entityInfo, this)
    MongoExecutor.update(_entityInfo, getTableName, this.id, update)
  }

  override def doSaveOrUpdate(context: EZContext): Future[Resp[String]] = {
    val saveOrUpdate = MongoBaseModel.convertToJsonObject(_entityInfo, this)
    MongoExecutor.saveOrUpdate(_entityInfo, getTableName, this.id, saveOrUpdate)
  }

  override def doUpdateByCond(newValues: String, condition: String, parameters: List[Any], context: EZContext): Future[Resp[Void]] = {
    MongoProcessor.updateByCond(getTableName, new JsonObject(condition), new JsonObject(newValues))
  }

  override def doGetById(id: Any, context: EZContext): Future[Resp[this.type]] = {
    MongoProcessor.getById(getTableName, id.asInstanceOf[String], _modelClazz).asInstanceOf[Future[Resp[this.type]]]
  }

  override def doGetByCond(condition: String, parameters: List[Any], context: EZContext): Future[Resp[MongoBaseModel.this.type]] = {
    MongoProcessor.getByCond(getTableName, new JsonObject(condition), _modelClazz).asInstanceOf[Future[Resp[this.type]]]
  }

  override def doDeleteById(id: Any, context: EZContext): Future[Resp[Void]] = {
    MongoProcessor.deleteById(getTableName, id.asInstanceOf[String])
  }

  override def doDeleteByCond(condition: String, parameters: List[Any], context: EZContext): Future[Resp[Void]] = {
    MongoProcessor.deleteByCond(getTableName, new JsonObject(condition))
  }

  override def doCount(condition: String, parameters: List[Any], context: EZContext): Future[Resp[Long]] = {
    MongoProcessor.count(getTableName, new JsonObject(condition))
  }

  override def doExistById(id: Any, context: EZContext): Future[Resp[Boolean]] = {
    MongoProcessor.exist(getTableName, new JsonObject().put("_id", id))
  }

  override def doExistByCond(condition: String, parameters: List[Any], context: EZContext): Future[Resp[Boolean]] = {
    MongoProcessor.exist(getTableName, new JsonObject(condition))
  }

  override def doFind(condition: String, parameters: List[Any], context: EZContext): Future[Resp[List[this.type]]] = {
    MongoProcessor.find(getTableName, new JsonObject(condition), null, 0, _modelClazz).asInstanceOf[Future[Resp[List[this.type]]]]
  }

  override def doPage(condition: String, parameters: List[Any], pageNumber: Long, pageSize: Int, context: EZContext): Future[Resp[Page[this.type]]] = {
    MongoProcessor.page(getTableName, new JsonObject(condition), pageNumber, pageSize, null, _modelClazz).asInstanceOf[Future[Resp[Page[this.type]]]]
  }

  def findWithOpt(condition: String = "{}", sort: Map[String, SortEnum], limit: Int = 0, context: EZContext = null): Future[Resp[List[this.type]]] = {
    MongoProcessor.find(getTableName, new JsonObject(condition), MongoBaseModel.convertSort(sort), limit, _modelClazz).asInstanceOf[Future[Resp[List[this.type]]]]
  }

  def pageWithOpt(condition: String = "{}", pageNumber: Long = 1, pageSize: Int = 10, sort: Map[String, SortEnum] = Map(), context: EZContext = null): Future[Resp[Page[this.type]]] = {
    MongoProcessor.page(getTableName, new JsonObject(condition), pageNumber, pageSize, MongoBaseModel.convertSort(sort), _modelClazz).asInstanceOf[Future[Resp[Page[this.type]]]]
  }

  def aggregate(query: JsonArray, context: EZContext = null): Future[Resp[JsonArray]] = {
    MongoProcessor.aggregate(getTableName, query)
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


