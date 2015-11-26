package com.asto.ez.framework.storage.jdbc

import com.asto.ez.framework.EZContext
import com.asto.ez.framework.storage.SecureModel
import com.ecfront.common.Resp

import scala.concurrent.Future

trait JDBCSecureModel extends JDBCBaseModel with SecureModel {

  override def save(context: EZContext): Future[Resp[String]] = {
    wrapSecureSave(context)
    super.save(context)
  }

  override def update(context: EZContext): Future[Resp[String]] = {
    wrapSecureUpdate(context)
    super.update(context)
  }

  override def saveOrUpdate(context: EZContext): Future[Resp[String]] = {
    wrapSecureSave(context)
    super.saveOrUpdate(context)
  }

}
