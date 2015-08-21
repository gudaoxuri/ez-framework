package com.ecfront.ez.framework.service.protocols

import com.ecfront.common.{Req, Resp}
import com.ecfront.ez.framework.service.{BasicService, IdModel, SecureModel}
import com.ecfront.storage.{JDBCStorable, PageModel}
import com.typesafe.scalalogging.slf4j.LazyLogging

trait JDBCService[M <: IdModel, R <: Req] extends BasicService[M, R] with JDBCStorable[M, R] {

  override protected def _doFindAll(request: Option[R]): Resp[List[M]] = {
    _doFindByCondition("1=1", None, request)
  }

  override protected def _doFindByCondition(condition: String, parameters: Option[List[Any]], request: Option[R]): Resp[List[M]] = {
    Resp.success(__findByCondition(_addDefaultSort(condition), parameters, request).orNull)
  }

  override protected def _doGetByCondition(condition: String, parameters: Option[List[Any]], request: Option[R]): Resp[M] = {
    Resp.success(__getByCondition(condition, parameters, request).get)
  }

  override protected def _doPageAll(pageNumber: Long, pageSize: Long, request: Option[R]): Resp[PageModel[M]] = {
    _doPageByCondition("1=1", None, pageNumber, pageSize, request)
  }

  override protected def _doPageByCondition(condition: String, parameters: Option[List[Any]], pageNumber: Long, pageSize: Long, request: Option[R]): Resp[PageModel[M]] = {
    Resp.success(__pageByCondition(_addDefaultSort(condition), parameters, pageNumber, pageSize, request).orNull)
  }

  override protected def _doSave(model: M, request: Option[R]): Resp[String] = {
    Resp.success(__save(model, request).orNull)
  }

  protected def _doSaveWithoutTransaction(model: M, request: Option[R]): Resp[String] = {
    Resp.success(__saveWithoutTransaction(model, request).orNull)
  }

  override protected def _doGetById(id: String, request: Option[R]): Resp[M] = {
    Resp.success(__getById(id, request).get)
  }

  override protected def _doUpdate(id: String, model: M, request: Option[R]): Resp[String] = {
    Resp.success(__update(id, model, request).orNull)
  }

  protected def _doUpdateWithoutTransaction(id: String, model: M, request: Option[R]): Resp[String] = {
    Resp.success(__updateWithoutTransaction(id, model, request).orNull)
  }

  override protected def _doDeleteById(id: String, request: Option[R]): Resp[String] = {
    Resp.success(__deleteById(id, request).orNull)
  }

  protected def _doDeleteByIdWithoutTransaction(id: String, request: Option[R]): Resp[String] = {
    Resp.success(__deleteByIdWithoutTransaction(id, request).orNull)
  }

  override protected def _doDeleteAll(request: Option[R]): Resp[List[String]] = {
    Resp.success(__deleteAll(request).orNull)
  }

  protected def _doDeleteAllWithoutTransaction(request: Option[R]): Resp[List[String]] = {
    Resp.success(__deleteAllWithoutTransaction(request).orNull)
  }

  override protected def _doDeleteByCondition(condition: String, parameters: Option[List[Any]], request: Option[R]): Resp[List[String]] = {
    Resp.success(__deleteByCondition(condition, parameters, request).orNull)
  }

  protected def _doDeleteByConditionWithoutTransaction(condition: String, parameters: Option[List[Any]], request: Option[R]): Resp[List[String]] = {
    Resp.success(__deleteByConditionWithoutTransaction(condition, parameters, request).orNull)
  }

  protected def _addDefaultSort(condition: String): String = {
    if (condition.toLowerCase.indexOf("order by") == -1 && classOf[SecureModel].isAssignableFrom(_modelClazz)) {
      condition + " ORDER BY update_time desc"
    } else {
      condition
    }
  }

}

object JDBCService extends LazyLogging {

  def init(): Unit = {
    var path = this.getClass.getResource("/").getPath
    if (System.getProperties.getProperty("os.name").toUpperCase.indexOf("WINDOWS") != -1) {
      path = path.substring(1)
    }
    JDBCStorable.init(path)
  }

  def close(): Unit = {
  }

}
