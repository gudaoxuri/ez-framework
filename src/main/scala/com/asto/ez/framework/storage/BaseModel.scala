package com.asto.ez.framework.storage

import com.asto.ez.framework.EZContext
import com.ecfront.common.{BeanHelper, Ignore, JsonHelper, Resp}

import scala.concurrent.Future

trait BaseModel extends Serializable {

  @Ignore
  protected def _modelClazz = this.getClass

  def getTableName = _modelClazz.getSimpleName.toLowerCase

  def save(context: EZContext = null): Future[Resp[String]]

  def update(context: EZContext = null): Future[Resp[String]]

  def saveOrUpdate(context: EZContext = null): Future[Resp[String]]

  def deleteById(id: Any, context: EZContext = null): Future[Resp[Void]]

  def getById(id: Any, context: EZContext = null): Future[Resp[this.type]]

  def updateByCond(newValues: String, condition: String, parameters: List[Any] = List(), context: EZContext = null): Future[Resp[Void]]

  def deleteByCond(condition: String, parameters: List[Any] = List(), context: EZContext = null): Future[Resp[Void]]

  def getByCond(condition: String = "", parameters: List[Any] = List(), context: EZContext = null): Future[Resp[this.type]]

  def existById(id: Any, context: EZContext = null): Future[Resp[Boolean]]

  def existByCond(condition: String = "", parameters: List[Any] = List(), context: EZContext = null): Future[Resp[Boolean]]

  def find(condition: String = "", parameters: List[Any] = List(), context: EZContext = null): Future[Resp[List[this.type]]]

  def page(condition: String = "", parameters: List[Any] = List(), pageNumber: Long = 1, pageSize: Int = 10, context: EZContext = null): Future[Resp[Page[this.type]]]

  def count(condition: String = "", parameters: List[Any] = List(), context: EZContext = null): Future[Resp[Long]]

  def toPersistentJsonString = {
    JsonHelper.toJsonString(BeanHelper.findValues(this))
  }

}

