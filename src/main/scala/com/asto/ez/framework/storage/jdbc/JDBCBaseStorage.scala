package com.asto.ez.framework.storage.jdbc

import com.asto.ez.framework.EZContext
import com.asto.ez.framework.storage.{BaseModel, BaseStorage, Page}
import com.ecfront.common.{BeanHelper, Resp}

import scala.concurrent.Future

trait JDBCBaseStorage[M <: BaseModel] extends BaseStorage[M] {

  protected val _entityInfo =
    if (JDBCEntityContainer.CONTAINER.contains(tableName)) {
      JDBCEntityContainer.CONTAINER(tableName)
    } else {
      JDBCEntityContainer.buildingEntityInfo(_modelClazz, tableName)
      JDBCEntityContainer.CONTAINER(tableName)
    }

  override def doSave(model: M, context: EZContext): Future[Resp[String]] = {
    DBExecutor.save(_entityInfo, if (context == null) EZContext.build() else context, getMapValue(model).filter(_._2 != null))
  }

  override def doUpdate(model: M, context: EZContext): Future[Resp[String]] = {
    DBExecutor.update(_entityInfo, if (context == null) EZContext.build() else context, getIdValue(model), getMapValue(model).filter(_._2 != null))
  }

  override def doSaveOrUpdate(model: M, context: EZContext): Future[Resp[String]] = {
    DBExecutor.saveOrUpdate(_entityInfo, if (context == null) EZContext.build() else context, getIdValue(model), getMapValue(model).filter(_._2 != null))
  }

  override def doDeleteById(id: Any, context: EZContext): Future[Resp[Void]] = {
    DBExecutor.delete(_entityInfo, if (context == null) EZContext.build() else context, id)
  }

  override def doGetById(id: Any, context: EZContext): Future[Resp[M]] = {
    DBExecutor.get(_entityInfo, if (context == null) EZContext.build() else context, id)
  }

  override def doUpdateByCond(newValues: String, condition: String, parameters: List[Any], context: EZContext): Future[Resp[Void]] = {
    DBExecutor.update(_entityInfo, if (context == null) EZContext.build() else context, newValues, condition, parameters)
  }

  override def doDeleteByCond(condition: String, parameters: List[Any], context: EZContext): Future[Resp[Void]] = {
    DBExecutor.delete(_entityInfo, if (context == null) EZContext.build() else context, condition, parameters)
  }

  override def doGetByCond(condition: String, parameters: List[Any], context: EZContext): Future[Resp[M]] = {
    DBExecutor.get(_entityInfo, if (context == null) EZContext.build() else context, condition, parameters)
  }

  override def doExistById(id: Any, context: EZContext): Future[Resp[Boolean]] = {
    DBExecutor.existById(_entityInfo, if (context == null) EZContext.build() else context, id)
  }

  override def doExistByCond(condition: String, parameters: List[Any], context: EZContext): Future[Resp[Boolean]] = {
    DBExecutor.existByCond(_entityInfo, if (context == null) EZContext.build() else context, condition, parameters)
  }

  override def doFind(condition: String, parameters: List[Any], context: EZContext): Future[Resp[List[M]]] = {
    DBExecutor.find(_entityInfo, if (context == null) EZContext.build() else context, condition, parameters)
  }

  override def doPage(condition: String, parameters: List[Any], pageNumber: Long, pageSize: Int, context: EZContext): Future[Resp[Page[M]]] = {
    DBExecutor.page(_entityInfo, if (context == null) EZContext.build() else context, condition, parameters, pageNumber, pageSize)
  }

  override def doCount(condition: String, parameters: List[Any], context: EZContext): Future[Resp[Long]] = {
    DBExecutor.count(_entityInfo, if (context == null) EZContext.build() else context, condition, parameters)
  }

  protected def getMapValue(model: BaseModel): Map[String, Any] = {
    //获取对象要持久化字段的值，忽略为null的id字段（由seq控制）
    BeanHelper.findValues(model, _entityInfo.ignoreFieldNames)
      .filterNot(item => item._1 == _entityInfo.idFieldName && (item._2 == null || item._2.toString.trim == ""))
  }

  protected def getIdValue(model: BaseModel): Any = {
    if (_entityInfo.idFieldName == BaseModel.Id_FLAG) {
      model.id
    } else {
      getValueByField(model, _entityInfo.idFieldName)
    }
  }

  protected def getValueByField(model: AnyRef, fieldName: String): Any = {
    BeanHelper.getValue(model, fieldName).orNull
  }

  protected def setValueByField(model: AnyRef, fieldName: String, value: Any): Unit = {
    BeanHelper.setValue(model, fieldName, value)
  }

}

