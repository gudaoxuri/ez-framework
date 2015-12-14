package com.asto.ez.framework.storage.mongo

import com.asto.ez.framework.EZContext
import com.asto.ez.framework.storage.SecureModel
import com.ecfront.common.Resp

import scala.concurrent.Future

trait MongoSecureModel extends MongoBaseModel with SecureModel {

  override def doSave(context: EZContext): Future[Resp[String]] = {
    wrapSecureSave(context)
    super.doSave(context)
  }

  override def doUpdate(context: EZContext): Future[Resp[String]] = {
    wrapSecureUpdate(context)
    super.doUpdate(context)
  }

  override def doSaveOrUpdate(context: EZContext): Future[Resp[String]] = {
    wrapSecureSave(context)
    super.doSaveOrUpdate(context)
  }

}
