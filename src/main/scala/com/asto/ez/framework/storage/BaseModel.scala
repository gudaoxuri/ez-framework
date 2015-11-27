package com.asto.ez.framework.storage

import com.asto.ez.framework.EZContext
import com.ecfront.common.{BeanHelper, Ignore, JsonHelper, Resp}

import scala.concurrent.Future

trait BaseModel extends Serializable {

  @Ignore
  protected val _modelClazz = this.getClass
  @Ignore
  protected val _tableName = _modelClazz.getSimpleName.toLowerCase

  def getTableName = _tableName

  def save(context: EZContext = null): Future[Resp[String]]

  def update(context: EZContext = null): Future[Resp[String]]

  def saveOrUpdate(context: EZContext = null): Future[Resp[String]]

  def deleteById(id: Any, context: EZContext = null): Future[Resp[Void]]

  def getById(id: Any, context: EZContext = null): Future[Resp[this.type]]

  def toPersistentJsonString = {
    JsonHelper.toJsonString(BeanHelper.findValues(this))
  }

}

