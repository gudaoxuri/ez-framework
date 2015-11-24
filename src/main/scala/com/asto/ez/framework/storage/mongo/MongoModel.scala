package com.asto.ez.framework.storage.mongo

import com.asto.ez.framework.EZContext
import com.asto.ez.framework.helper.Page
import com.ecfront.common.{BeanHelper, Resp}

import scala.concurrent.Future

trait MongoModel extends Serializable {

  protected val _modelClazz = this.getClass
  protected val _tableName = _modelClazz.getSimpleName.toLowerCase

  def getTableName = _tableName

  def save(context:EZContext=null): Future[Resp[Void]] = {
    DBExecutor.save(_entityInfo,if(context==null) EZContext.build() else context, BaseModel.getMapValue(this).filter(_._2 != null))
  }

  def update(context:EZContext=null): Future[Resp[Void]] = {
    DBExecutor.update(_entityInfo,if(context==null) EZContext.build() else context, BaseModel.getIdValue(this), BaseModel.getMapValue(this).filter(_._2 != null))
  }

  def saveOrUpdate(context:EZContext=null): Future[Resp[Void]] = {
    DBExecutor.saveOrUpdate(_entityInfo,if(context==null) EZContext.build() else context, BaseModel.getIdValue(this), BaseModel.getMapValue(this).filter(_._2 != null))
  }

  def updateByCond(newValues: String, condition: String, parameters: List[Any],context:EZContext=null): Future[Resp[Void]] = {
    DBExecutor.update(_entityInfo, if(context==null) EZContext.build() else context,newValues, condition, parameters)
  }

  def deleteById(id: Any,context:EZContext=null): Future[Resp[Void]] = {
    DBExecutor.delete(_entityInfo,if(context==null) EZContext.build() else context, id)
  }

  def deleteByCond(condition: String, parameters: List[Any],context:EZContext=null): Future[Resp[Void]] = {
    DBExecutor.delete(_entityInfo,if(context==null) EZContext.build() else context, condition, parameters)
  }

  def getById(id: Any,context:EZContext=null): Future[Resp[this.type]] = {
    DBExecutor.get(_entityInfo,if(context==null) EZContext.build() else context, id)
  }

  def getByCond(condition: String, parameters: List[Any],context:EZContext=null): Future[Resp[this.type ]] = {
    DBExecutor.get(_entityInfo, if(context==null) EZContext.build() else context,condition, parameters)
  }

  def exist(condition: String, parameters: List[Any],context:EZContext=null): Future[Resp[Boolean]] = {
    DBExecutor.exist(_entityInfo,if(context==null) EZContext.build() else context, condition, parameters)
  }

  def find(condition: String = " 1=1 ", parameters: List[Any] = List(),context:EZContext=null): Future[Resp[List[this.type]]] = {
    DBExecutor.find(_entityInfo,if(context==null) EZContext.build() else context, condition, parameters)
  }

  def page(condition: String = " 1=1 ", parameters: List[Any] = List(), pageNumber: Long = 1, pageSize: Int = 10,context:EZContext=null): Future[Resp[Page[this.type]]] = {
    DBExecutor.page(_entityInfo, if(context==null) EZContext.build() else context,condition, parameters, pageNumber, pageSize)
  }

  def count(condition: String = " 1=1 ", parameters: List[Any] = List(),context:EZContext=null): Future[Resp[Long]] = {
    DBExecutor.count(_entityInfo,if(context==null) EZContext.build() else context, condition, parameters)
  }

}

object BaseModel {

  val REL_FLAG = "rel"

  protected def getMapValue(model: BaseModel): Map[String, Any] = {
    //获取对象要持久化字段的值，忽略为null的id字段（由seq控制）
    BeanHelper.findValues(model, model._entityInfo.ignoreFieldNames)
      .filterNot(item => item._1 == model._entityInfo.idFieldName && (item._2 == null || item._2.toString.trim == ""))
  }

  protected def getIdValue(model: BaseModel): Any = {
    getValueByField(model, model._entityInfo.idFieldName)
  }

  protected def getValueByField(model: AnyRef, fieldName: String): Any = {
    BeanHelper.getValue(model, fieldName).orNull
  }

  protected def setValueByField(model: AnyRef, fieldName: String, value: Any): Unit = {
    BeanHelper.setValue(model, fieldName, value)
  }

}
