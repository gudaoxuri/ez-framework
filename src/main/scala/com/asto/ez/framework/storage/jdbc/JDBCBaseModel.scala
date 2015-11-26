package com.asto.ez.framework.storage.jdbc

import com.asto.ez.framework.EZContext
import com.asto.ez.framework.storage.{BaseModel, Page}
import com.ecfront.common.{BeanHelper, Resp}

import scala.async.Async.{async, await}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait JDBCBaseModel extends BaseModel {

  protected val _entityInfo =
    if (JDBCEntityContainer.CONTAINER.contains(_tableName)) {
      JDBCEntityContainer.CONTAINER(_tableName)
    } else {
      JDBCEntityContainer.initEntity(_modelClazz, _tableName)
      JDBCEntityContainer.CONTAINER(_tableName)
    }

  override def save(context: EZContext = null): Future[Resp[String]] = async {
    await(DBExecutor.save(_entityInfo, if (context == null) EZContext.build() else context, JDBCBaseModel.getMapValue(this).filter(_._2 != null)))
  }

  override def update(context: EZContext = null): Future[Resp[String]] = async {
    await(DBExecutor.update(_entityInfo, if (context == null) EZContext.build() else context, JDBCBaseModel.getIdValue(this), JDBCBaseModel.getMapValue(this).filter(_._2 != null)))
  }

  override def saveOrUpdate(context: EZContext = null): Future[Resp[String]] = async {
    await(DBExecutor.saveOrUpdate(_entityInfo, if (context == null) EZContext.build() else context, JDBCBaseModel.getIdValue(this), JDBCBaseModel.getMapValue(this).filter(_._2 != null)))
  }

  override def deleteById(id: Any, context: EZContext = null): Future[Resp[Void]] = async {
    await(DBExecutor.delete(_entityInfo, if (context == null) EZContext.build() else context, id))
  }

  override def getById(id: Any, context: EZContext = null): Future[Resp[this.type]] = async {
    await(DBExecutor.get(_entityInfo, if (context == null) EZContext.build() else context, id))
  }

  def updateByCond(newValues: String, condition: String, parameters: List[Any], context: EZContext = null): Future[Resp[Void]] = async {
    await(DBExecutor.update(_entityInfo, if (context == null) EZContext.build() else context, newValues, condition, parameters))
  }

  def deleteByCond(condition: String, parameters: List[Any], context: EZContext = null): Future[Resp[Void]] = async {
    await(DBExecutor.delete(_entityInfo, if (context == null) EZContext.build() else context, condition, parameters))
  }

  def getByCond(condition: String, parameters: List[Any], context: EZContext = null): Future[Resp[this.type]] = async {
    await(DBExecutor.get(_entityInfo, if (context == null) EZContext.build() else context, condition, parameters))
  }

  def exist(condition: String, parameters: List[Any], context: EZContext = null): Future[Resp[Boolean]] = async {
    await(DBExecutor.exist(_entityInfo, if (context == null) EZContext.build() else context, condition, parameters))
  }

  def find(condition: String = " 1=1 ", parameters: List[Any] = List(), context: EZContext = null): Future[Resp[List[this.type]]] = async {
    await(DBExecutor.find(_entityInfo, if (context == null) EZContext.build() else context, condition, parameters))
  }

  def page(condition: String = " 1=1 ", parameters: List[Any] = List(), pageNumber: Long = 1, pageSize: Int = 10, context: EZContext = null): Future[Resp[Page[this.type]]] = async {
    await(DBExecutor.page(_entityInfo, if (context == null) EZContext.build() else context, condition, parameters, pageNumber, pageSize))
  }

  def count(condition: String = " 1=1 ", parameters: List[Any] = List(), context: EZContext = null): Future[Resp[Long]] = async {
    await(DBExecutor.count(_entityInfo, if (context == null) EZContext.build() else context, condition, parameters))
  }

}

object JDBCBaseModel {

  val REL_FLAG = "rel"

  protected def getMapValue(model: JDBCBaseModel): Map[String, Any] = {
    //获取对象要持久化字段的值，忽略为null的id字段（由seq控制）
    BeanHelper.findValues(model, model._entityInfo.ignoreFieldNames)
      .filterNot(item => item._1 == model._entityInfo.idFieldName && (item._2 == null || item._2.toString.trim == ""))
  }

  protected def getIdValue(model: JDBCBaseModel): Any = {
    getValueByField(model, model._entityInfo.idFieldName)
  }

  protected def getValueByField(model: AnyRef, fieldName: String): Any = {
    BeanHelper.getValue(model, fieldName).orNull
  }

  protected def setValueByField(model: AnyRef, fieldName: String, value: Any): Unit = {
    BeanHelper.setValue(model, fieldName, value)
  }

}
