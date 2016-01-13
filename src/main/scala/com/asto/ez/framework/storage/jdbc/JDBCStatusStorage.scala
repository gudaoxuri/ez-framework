package com.asto.ez.framework.storage.jdbc

import com.asto.ez.framework.EZContext
import com.asto.ez.framework.storage.{StatusStorage, Page, StatusModel}
import com.ecfront.common.Resp

import scala.concurrent.Future

trait JDBCStatusStorage[M <: StatusModel] extends JDBCBaseStorage[M] with StatusStorage[M] {

  override def doGetEnabledByCond(condition: String, parameters: List[Any], context: EZContext): Future[Resp[M]] = {
    getByCond(appendEnabled(condition), parameters, context)
  }

  override def doExistEnabledByCond(condition: String, parameters: List[Any], context: EZContext): Future[Resp[Boolean]] = {
    existByCond(appendEnabled(condition), parameters, context)
  }

  override def doFindEnabled(condition: String, parameters: List[Any], context: EZContext): Future[Resp[List[M]]] = {
    find(appendEnabled(condition), parameters, context)
  }

  override def doPageEnabled(condition: String, parameters: List[Any], pageNumber: Long, pageSize: Int, context: EZContext): Future[Resp[Page[M]]] = {
    page(appendEnabled(condition), parameters, pageNumber, pageSize, context)
  }

  override def doCountEnabled(condition: String, parameters: List[Any], context: EZContext): Future[Resp[Long]] = {
    count(appendEnabled(condition), parameters, context)
  }

  override def doEnableById(id: Any, context: EZContext): Future[Resp[Void]] = {
    updateByCond(" enable = true ", " id = ?", List(id), context)
  }

  override def doDisableById(id: Any, context: EZContext): Future[Resp[Void]] = {
    updateByCond(" enable = false ", " id = ?", List(id), context)
  }


  private def appendEnabled(condition: String): String = {
    condition + " AND enable = true "
  }

}








