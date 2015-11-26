package com.asto.ez.framework.storage.jdbc

import com.asto.ez.framework.EZContext
import com.asto.ez.framework.storage.{Page, StatusModel}
import com.ecfront.common.Resp

import scala.concurrent.Future

trait JDBCStatusModel extends JDBCBaseModel with StatusModel {

  def getEnabledByCond(condition: String, parameters: List[Any], context: EZContext = null): Future[Resp[this.type]] = {
    DBExecutor.get(_entityInfo, if (context == null) EZContext.build() else context, appendEnabled(condition), parameters)
  }

  def existEnabled(condition: String, parameters: List[Any], context: EZContext = null): Future[Resp[Boolean]] = {
    DBExecutor.exist(_entityInfo, if (context == null) EZContext.build() else context, appendEnabled(condition), parameters)
  }

  def findEnabled(condition: String = " 1=1 ", parameters: List[Any] = List(), context: EZContext = null): Future[Resp[List[this.type]]] = {
    DBExecutor.find(_entityInfo, if (context == null) EZContext.build() else context, appendEnabled(condition), parameters)
  }

  def pageEnabled(condition: String = " 1=1 ", parameters: List[Any] = List(), pageNumber: Long = 1, pageSize: Int = 10, context: EZContext = null): Future[Resp[Page[this.type]]] = {
    DBExecutor.page(_entityInfo, if (context == null) EZContext.build() else context, appendEnabled(condition), parameters, pageNumber, pageSize)
  }

  def countEnabled(condition: String = " 1=1 ", parameters: List[Any] = List(), context: EZContext = null): Future[Resp[Long]] = {
    DBExecutor.count(_entityInfo, if (context == null) EZContext.build() else context, appendEnabled(condition), parameters)
  }

  private def appendEnabled(condition: String): String = {
    condition + " AND enable = true "
  }

}








