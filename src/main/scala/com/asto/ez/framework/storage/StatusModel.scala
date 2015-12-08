package com.asto.ez.framework.storage

import com.asto.ez.framework.EZContext
import com.asto.ez.framework.storage.jdbc.Index
import com.ecfront.common.Resp

import scala.beans.BeanProperty
import scala.concurrent.Future

trait StatusModel extends BaseModel {

  @Index
  @BeanProperty var enable: Boolean = _

  def getEnabledByCond(condition: String = "", parameters: List[Any] = List(), context: EZContext = null): Future[Resp[this.type]]

  def existEnabled(condition: String = "", parameters: List[Any] = List(), context: EZContext = null): Future[Resp[Boolean]]

  def findEnabled(condition: String = "", parameters: List[Any] = List(), context: EZContext = null): Future[Resp[List[this.type]]]

  def pageEnabled(condition: String = "", parameters: List[Any] = List(), pageNumber: Long = 1, pageSize: Int = 10, context: EZContext = null): Future[Resp[Page[this.type]]]

  def countEnabled(condition: String = "", parameters: List[Any] = List(), context: EZContext = null): Future[Resp[Long]]

  def enableById(id: Any, context: EZContext = null): Future[Resp[Void]]

  def disableById(id: Any, context: EZContext = null): Future[Resp[Void]]

}

object StatusModel {
  val ENABLE_FLAG = "enable"
}








