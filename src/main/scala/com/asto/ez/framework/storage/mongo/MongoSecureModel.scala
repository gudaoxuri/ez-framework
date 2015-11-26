package com.asto.ez.framework.storage.mongo

import com.asto.ez.framework.EZContext
import com.asto.ez.framework.storage.SecureModel
import com.ecfront.common.Resp

import scala.concurrent.Future

trait MongoSecureModel extends MongoBaseModel with SecureModel{

  override def save(context: EZContext = null): Future[Resp[String]] = {
    wrapSecureSave(context)
    super.save(context)
  }

  override def update(context: EZContext = null): Future[Resp[String]] = {
    wrapSecureUpdate(context)
    super.update(context)
  }

  override def saveOrUpdate(context: EZContext = null): Future[Resp[String]] = {
    wrapSecureSave(context)
    super.saveOrUpdate(context)
  }

}
