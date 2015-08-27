package com.ecfront.ez.framework.service

import com.ecfront.common.{Req, Resp}
import com.ecfront.storage.PageModel

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait FutureService[M <: AnyRef, R <: Req] extends BasicService[M,R] {

  def _getById(id: String, request: Option[R] = None): Future[Resp[M]] = Future {
    _executeGetById(id, request)
  }

  def _getByCondition(condition: String, parameters: Option[List[Any]] = None, request: Option[R] = None): Future[Resp[M]] = Future {
    _executeGetByCondition(condition, parameters, request)
  }

  def _findAll(request: Option[R] = None): Future[Resp[List[M]]] = Future {
    _executeFindAll(request)
  }

  def _findByCondition(condition: String, parameters: Option[List[Any]] = None, request: Option[R] = None): Future[Resp[List[M]]] = Future {
    _executeFindByCondition(condition, parameters, request)
  }

  def _pageAll(pageNumber: Long = 1, pageSize: Long = 10, request: Option[R] = None): Future[Resp[PageModel[M]]] = Future {
    _executePageAll(pageNumber, pageSize, request)
  }

  def _pageByCondition(condition: String, parameters: Option[List[Any]] = None, pageNumber: Long = 1, pageSize: Long = 10, request: Option[R] = None): Future[Resp[PageModel[M]]] = Future {
    _executePageByCondition(condition, parameters, pageNumber, pageSize, request)
  }

  def _saveOrUpdate(model: M, request: Option[R] = None): Future[Resp[String]] = Future {
    _executeSaveOrUpdate(model, request)
  }

  def _save(model: M, request: Option[R] = None): Future[Resp[String]] = Future {
    _executeSave(model, request)
  }

  def _update(id: String, model: M, request: Option[R] = None): Future[Resp[String]] = Future {
    _executeUpdate(id, model, request)
  }

  def _deleteById(id: String, request: Option[R] = None): Future[Resp[String]] = Future {
    _executeDeleteById(id, request)
  }

  def _deleteByCondition(condition: String, parameters: Option[List[Any]] = None, request: Option[R] = None): Future[Resp[List[String]]] = Future {
    _executeDeleteByCondition(condition, parameters, request)
  }

  def _deleteAll(request: Option[R] = None): Future[Resp[List[String]]] = Future {
    _executeDeleteAll(request)
  }

}



