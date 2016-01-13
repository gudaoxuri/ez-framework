package com.asto.ez.framework.storage.mongo

import com.asto.ez.framework.EZContext
import com.asto.ez.framework.storage.{SecureModel, SecureStorage}
import com.ecfront.common.Resp

import scala.concurrent.Future

trait MongoSecureStorage[M <: SecureModel] extends MongoBaseStorage[M] with SecureStorage[M] {

  override def doSave(model: M, context: EZContext): Future[Resp[String]] = {
    wrapSecureSave(model, context)
    super.doSave(model, context)
  }

  override def doUpdate(model: M, context: EZContext): Future[Resp[String]] = {
    wrapSecureUpdate(model, context)
    super.doUpdate(model, context)
  }

  override def doSaveOrUpdate(model: M, context: EZContext): Future[Resp[String]] = {
    wrapSecureSave(model, context)
    super.doSaveOrUpdate(model, context)
  }

}
