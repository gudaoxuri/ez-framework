package com.asto.ez.framework.storage.jdbc

import com.asto.ez.framework.EZContext
import com.asto.ez.framework.storage.{Page, StatusModel}
import com.ecfront.common.Resp

import scala.concurrent.Future

trait JDBCStatusModel extends JDBCBaseModel with StatusModel {

  override def getEnabledByCond(condition: String = " 1=1 ", parameters: List[Any] = List(), context: EZContext = null): Future[Resp[this.type]] = {
    getByCond(appendEnabled(condition), parameters, context)
  }

  override def existEnabled(condition: String = " 1=1 ", parameters: List[Any] = List(), context: EZContext = null): Future[Resp[Boolean]] = {
    existByCond(appendEnabled(condition), parameters, context)
  }

  override def findEnabled(condition: String = " 1=1 ", parameters: List[Any] = List(), context: EZContext = null): Future[Resp[List[this.type]]] = {
    find(appendEnabled(condition), parameters, context)
  }

  override def pageEnabled(condition: String = " 1=1 ", parameters: List[Any] = List(), pageNumber: Long = 1, pageSize: Int = 10, context: EZContext = null): Future[Resp[Page[this.type]]] = {
    page(appendEnabled(condition), parameters, pageNumber, pageSize, context)
  }

  override def countEnabled(condition: String = " 1=1 ", parameters: List[Any] = List(), context: EZContext = null): Future[Resp[Long]] = {
    count(appendEnabled(condition), parameters, context)
  }

  private def appendEnabled(condition: String): String = {
    condition + " AND enable = true "
  }

}








