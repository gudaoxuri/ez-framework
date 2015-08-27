package com.ecfront.ez.framework.service

import java.lang.reflect.ParameterizedType

import com.ecfront.common.{BeanHelper, Req, Resp}
import com.ecfront.storage.PageModel

import scala.collection.mutable.ArrayBuffer

trait SyncVOService[M <: AnyRef, V <: AnyRef, R <: Req] extends BasicService[M, R] {

  protected val _typeArgs = this.getClass.getGenericInterfaces()(1).asInstanceOf[ParameterizedType].getActualTypeArguments
  protected val _voClazz = if (_typeArgs.size == 3) _typeArgs(1).asInstanceOf[Class[V]] else null

  protected def modelToVO(model: M, vo: V): V = vo

  protected def voToModel(vo: V, model: M): M = model

  def _findAll(request: Option[R] = None): Resp[List[V]] = {
    _find(super._executeFindAll(request))
  }

  def _findByCondition(condition: String, parameters: Option[List[Any]] = None, request: Option[R] = None): Resp[List[V]] = {
    _find(super._executeFindByCondition(condition, parameters, request))
  }

  private def _find(result: Resp[List[M]]): Resp[List[V]] = {
    if (_voClazz == null) {
      result.asInstanceOf[Resp[List[V]]]
    } else {
      if (result) {
        val vos = ArrayBuffer[V]()
        result.body.foreach {
          item =>
            val vo = _voClazz.newInstance()
            BeanHelper.copyProperties(vo, item)
            vos += modelToVO(item, vo)
        }
        Resp.success(vos.toList)
      } else {
        result
      }
    }
  }

  def _getByCondition(condition: String, parameters: Option[List[Any]] = None, request: Option[R] = None): Resp[V] = {
    _get(super._executeGetByCondition(condition, parameters, request))
  }

  def _getById(id: String, request: Option[R] = None): Resp[V] = {
    _get(super._executeGetById(id, request))
  }

  private def _get(result: Resp[M]): Resp[V] = {
    if (_voClazz == null) {
      result.asInstanceOf[Resp[V]]
    } else {
      if (result) {
        if (result.body != null) {
          val vo = _voClazz.newInstance()
          BeanHelper.copyProperties(vo, result.body)
          Resp.success(modelToVO(result.body, vo))
        } else {
          result.asInstanceOf[Resp[V]]
        }
      } else {
        result
      }
    }
  }

  def _pageAll(pageNumber: Long = 1, pageSize: Long = 10, request: Option[R] = None): Resp[PageModel[V]] = {
    _page(super._executePageAll(pageNumber, pageSize, request))
  }

  def _pageByCondition(condition: String, parameters: Option[List[Any]] = None, pageNumber: Long = 1, pageSize: Long = 10, request: Option[R] = None): Resp[PageModel[V]] = {
    _page(super._executePageByCondition(condition, parameters, pageNumber, pageSize, request))
  }

  private def _page(result: Resp[PageModel[M]]): Resp[PageModel[V]] = {
    if (_voClazz == null) {
      result.asInstanceOf[Resp[PageModel[V]]]
    } else {
      if (result) {
        val vos = ArrayBuffer[V]()
        result.body.results.foreach {
          item =>
            val vo = _voClazz.newInstance()
            BeanHelper.copyProperties(vo, item)
            vos += modelToVO(item, vo)
        }
        Resp.success(PageModel(result.body.pageNumber, result.body.pageSize, result.body.pageTotal, result.body.recordTotal, vos.toList))
      } else {
        result
      }
    }
  }

  def _saveOrUpdate(vo: V, request: Option[R] = None): Resp[String] = {
    if (_voClazz == null) {
      super._executeSaveOrUpdate(vo.asInstanceOf[M], request)
    } else {
      val model = _modelClazz.newInstance()
      BeanHelper.copyProperties(model, vo)
      super._executeSaveOrUpdate(voToModel(vo, model), request)
    }
  }

  def _save(vo: V, request: Option[R] = None): Resp[String] = {
    if (_voClazz == null) {
      super._executeSave(vo.asInstanceOf[M], request)
    } else {
      val model = _modelClazz.newInstance()
      BeanHelper.copyProperties(model, vo)
      super._executeSave(voToModel(vo, model), request)
    }
  }

  def _update(id: String, vo: V, request: Option[R] = None): Resp[String] = {
    if (_voClazz == null) {
      super._executeUpdate(id, vo.asInstanceOf[M], request)
    } else {
      val model = _modelClazz.newInstance()
      BeanHelper.copyProperties(model, vo)
      super._executeUpdate(id, voToModel(vo, model), request)
    }
  }

  def _deleteById(id: String, request: Option[R] = None): Resp[String] = {
    _executeDeleteById(id, request)
  }

  def _deleteByCondition(condition: String, parameters: Option[List[Any]] = None, request: Option[R] = None): Resp[List[String]] = {
    _executeDeleteByCondition(condition, parameters, request)
  }

  def _deleteAll(request: Option[R] = None): Resp[List[String]] = {
    _executeDeleteAll(request)
  }

}



