package com.asto.ez.framework.storage.jdbc

import com.asto.ez.framework.EZContext
import com.asto.ez.framework.storage.{BaseModel, Page}
import com.ecfront.common.{BeanHelper, Resp}

import scala.concurrent.Future

trait JDBCBaseModel extends BaseModel {

  protected def _entityInfo =
    if (JDBCEntityContainer.CONTAINER.contains(getTableName)) {
      JDBCEntityContainer.CONTAINER(getTableName)
    } else {
      JDBCEntityContainer.buildingEntityInfo(_modelClazz, getTableName)
      JDBCEntityContainer.CONTAINER(getTableName)
    }

  override def doSave(context: EZContext): Future[Resp[String]] = {
    DBExecutor.save(_entityInfo, if (context == null) EZContext.build() else context, JDBCBaseModel.getMapValue(this).filter(_._2 != null))
  }

  override def doUpdate(context: EZContext): Future[Resp[String]] = {
    DBExecutor.update(_entityInfo, if (context == null) EZContext.build() else context, JDBCBaseModel.getIdValue(this), JDBCBaseModel.getMapValue(this).filter(_._2 != null))
  }

  override def doSaveOrUpdate(context: EZContext): Future[Resp[String]] = {
    DBExecutor.saveOrUpdate(_entityInfo, if (context == null) EZContext.build() else context, JDBCBaseModel.getIdValue(this), JDBCBaseModel.getMapValue(this).filter(_._2 != null))
  }

  override def doDeleteById(id: Any, context: EZContext): Future[Resp[Void]] = {
    DBExecutor.delete(_entityInfo, if (context == null) EZContext.build() else context, id)
  }

  override def doGetById(id: Any, context: EZContext): Future[Resp[this.type]] = {
    DBExecutor.get(_entityInfo, if (context == null) EZContext.build() else context, id)
  }

  override def doUpdateByCond(newValues: String, condition: String, parameters: List[Any], context: EZContext): Future[Resp[Void]] = {
    DBExecutor.update(_entityInfo, if (context == null) EZContext.build() else context, newValues, condition, parameters)
  }

  override def doDeleteByCond(condition: String, parameters: List[Any], context: EZContext): Future[Resp[Void]] = {
    DBExecutor.delete(_entityInfo, if (context == null) EZContext.build() else context, condition, parameters)
  }

  override def doGetByCond(condition: String, parameters: List[Any], context: EZContext): Future[Resp[this.type]] = {
    DBExecutor.get(_entityInfo, if (context == null) EZContext.build() else context, condition, parameters)
  }

  override def doExistById(id: Any, context: EZContext): Future[Resp[Boolean]] = {
    DBExecutor.existById(_entityInfo, if (context == null) EZContext.build() else context, id)
  }

  override def doExistByCond(condition: String, parameters: List[Any], context: EZContext): Future[Resp[Boolean]] = {
    DBExecutor.existByCond(_entityInfo, if (context == null) EZContext.build() else context, condition, parameters)
  }

  override def doFind(condition: String, parameters: List[Any], context: EZContext): Future[Resp[List[this.type]]] = {
    DBExecutor.find(_entityInfo, if (context == null) EZContext.build() else context, condition, parameters)
  }

  override def doPage(condition: String, parameters: List[Any], pageNumber: Long, pageSize: Int, context: EZContext): Future[Resp[Page[this.type]]] = {
    DBExecutor.page(_entityInfo, if (context == null) EZContext.build() else context, condition, parameters, pageNumber, pageSize)
  }

  override def doCount(condition: String, parameters: List[Any], context: EZContext): Future[Resp[Long]] = {
    DBExecutor.count(_entityInfo, if (context == null) EZContext.build() else context, condition, parameters)
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
