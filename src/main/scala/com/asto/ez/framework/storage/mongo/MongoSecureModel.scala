package com.asto.ez.framework.storage.mongo

import com.asto.ez.framework.EZContext
import com.asto.ez.framework.storage.SecureModel
import com.ecfront.common.Resp

import scala.async.Async.{async, await}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait MongoSecureModel extends MongoBaseModel with SecureModel {

  override def save(context: EZContext = null): Future[Resp[String]] = async {
    wrapSecureSave(context)
    await(super.save(context))
  }

  override def update(context: EZContext = null): Future[Resp[String]] = async {
    wrapSecureUpdate(context)
    await(super.update(context))
  }

  override def saveOrUpdate(context: EZContext = null): Future[Resp[String]] = async {
    wrapSecureSave(context)
    await(super.saveOrUpdate(context))
  }

}
