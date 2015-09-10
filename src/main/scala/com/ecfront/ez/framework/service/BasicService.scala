package com.ecfront.ez.framework.service

import java.lang.reflect.ParameterizedType

import com.ecfront.common.{BeanHelper, Req, Resp}
import com.ecfront.ez.framework.storage.{IdModel, PageModel, SecureModel, StatusModel}
import com.typesafe.scalalogging.slf4j.LazyLogging

trait BasicService[M <: AnyRef, R <: Req] extends LazyLogging {

  protected val _modelClazz = this.getClass.getGenericInterfaces()(0).asInstanceOf[ParameterizedType].getActualTypeArguments()(0).asInstanceOf[Class[M]]

  protected val _isIdModel = classOf[IdModel].isAssignableFrom(_modelClazz)
  protected val _isSecureModel = classOf[SecureModel].isAssignableFrom(_modelClazz)

  logger.info( """Create Service: model: %s""".format(_modelClazz.getSimpleName))

  //=========================Common=========================

  protected def _useAuthType = _AuthType.BY_CREATE_USER

  protected def _convertToView(model: M, request: Option[R]): M = {
    model
  }

  protected def _convertToViews(models: List[M], request: Option[R]): List[M] = {
    models
  }

  //=========================GetByID=========================

  protected def _preGetById(id: String, request: Option[R]): Resp[Any] = {
    Resp.success(null)
  }

  protected def _postGetById(result: M, preResult: Any, request: Option[R]): Resp[M] = {
    Resp.success(result)
  }

  protected def _doGetById(id: String, request: Option[R]): Resp[M]

  protected def _executeGetById(id: String, request: Option[R]): Resp[M] = {
    val preResult = _preGetById(id, request)
    if (preResult) {
      val result = _doGetById(id, request)
      if (result) {
        _postGetById(_convertToView(result.body, request), preResult.body, request)
      } else {
        result
      }
    } else {
      preResult
    }
  }

  //=========================GetByCondition=========================

  protected def _preGetByCondition(condition: String, parameters: Option[List[Any]], request: Option[R]): Resp[Any] = {
    Resp.success(null)
  }

  protected def _postGetByCondition(result: M, preResult: Any, request: Option[R]): Resp[M] = {
    Resp.success(result)
  }

  protected def _doGetByCondition(condition: String, parameters: Option[List[Any]], request: Option[R]): Resp[M]

  protected def _executeGetByCondition(condition: String, parameters: Option[List[Any]], request: Option[R]): Resp[M] = {
    val preResult = _preGetByCondition(condition, parameters, request)
    if (preResult) {
      val result = _doGetByCondition(condition, parameters, request)
      if (result) {
        _postGetByCondition(_convertToView(result.body, request), preResult.body, request)
      } else {
        result
      }
    } else {
      preResult
    }
  }

  //=========================FindAll=========================

  protected def _preFindAll(request: Option[R]): Resp[Any] = {
    Resp.success(null)
  }

  protected def _postFindAll(result: List[M], preResult: Any, request: Option[R]): Resp[List[M]] = {
    Resp.success(result)
  }

  protected def _doFindAll(request: Option[R]): Resp[List[M]]

  protected def _executeFindAll(request: Option[R]): Resp[List[M]] = {
    val preResult = _preFindAll(request)
    if (preResult) {
      val result = _doFindAll(request)
      if (result) {
        _postFindAll(_convertToViews(result.body, request), preResult.body, request)
      } else {
        result
      }
    } else {
      preResult
    }
  }

  //=========================FindAllEnable=========================

  protected def _preFindAllEnable(request: Option[R]): Resp[Any] = {
    Resp.success(null)
  }

  protected def _postFindAllEnable(result: List[M], preResult: Any, request: Option[R]): Resp[List[M]] = {
    Resp.success(result)
  }

  protected def _doFindAllEnable(request: Option[R]): Resp[List[M]]

  protected def _executeFindAllEnable(request: Option[R]): Resp[List[M]] = {
    if (classOf[StatusModel].isAssignableFrom(_modelClazz)) {
      val preResult = _preFindAllEnable(request)
      if (preResult) {
        val result = _doFindAllEnable(request)
        if (result) {
          _postFindAllEnable(_convertToViews(result.body, request), preResult.body, request)
        } else {
          result
        }
      } else {
        preResult
      }
    } else {
      Resp.badRequest("The model not extend [StatusModel]")
    }
  }

  //=========================FindAllDisable=========================

  protected def _preFindAllDisable(request: Option[R]): Resp[Any] = {
    Resp.success(null)
  }

  protected def _postFindAllDisable(result: List[M], preResult: Any, request: Option[R]): Resp[List[M]] = {
    Resp.success(result)
  }

  protected def _doFindAllDisable(request: Option[R]): Resp[List[M]]

  protected def _executeFindAllDisable(request: Option[R]): Resp[List[M]] = {
    if (classOf[StatusModel].isAssignableFrom(_modelClazz)) {
      val preResult = _preFindAllDisable(request)
      if (preResult) {
        val result = _doFindAllDisable(request)
        if (result) {
          _postFindAllDisable(_convertToViews(result.body, request), preResult.body, request)
        } else {
          result
        }
      } else {
        preResult
      }
    } else {
      Resp.badRequest("The model not extend [StatusModel]")
    }
  }

  //=========================FindByCondition=========================

  protected def _preFindByCondition(condition: String, parameters: Option[List[Any]], request: Option[R]): Resp[Any] = {
    Resp.success(null)
  }

  protected def _postFindByCondition(result: List[M], preResult: Any, request: Option[R]): Resp[List[M]] = {
    Resp.success(result)
  }

  protected def _doFindByCondition(condition: String, parameters: Option[List[Any]], request: Option[R]): Resp[List[M]]

  protected def _executeFindByCondition(condition: String, parameters: Option[List[Any]], request: Option[R]): Resp[List[M]] = {
    val preResult = _preFindByCondition(condition, parameters, request)
    if (preResult) {
      val result = _doFindByCondition(condition, parameters, request)
      if (result) {
        _postFindByCondition(_convertToViews(result.body, request), preResult.body, request)
      } else {
        result
      }
    } else {
      preResult
    }
  }

  //=========================PageAll=========================

  protected def _prePageAll(pageNumber: Long, pageSize: Long, request: Option[R]): Resp[Any] = {
    Resp.success(null)
  }

  protected def _postPageAll(result: PageModel[M], preResult: Any, pageNumber: Long, pageSize: Long, request: Option[R]): Resp[PageModel[M]] = {
    Resp.success(result)
  }

  protected def _doPageAll(pageNumber: Long, pageSize: Long, request: Option[R]): Resp[PageModel[M]]

  protected def _executePageAll(pageNumber: Long, pageSize: Long, request: Option[R]): Resp[PageModel[M]] = {
    val preResult = _prePageAll(pageNumber, pageSize, request)
    if (preResult) {
      val result = _doPageAll(pageNumber, pageSize, request)
      if (result) {
        result.body.setResults(_convertToViews(result.body.results, request))
        _postPageAll(result.body, preResult.body, pageNumber, pageSize, request)
      } else {
        result
      }
    } else {
      preResult
    }
  }

  //=========================PageAllEnable=========================

  protected def _prePageAllEnable(pageNumber: Long, pageSize: Long, request: Option[R]): Resp[Any] = {
    Resp.success(null)
  }

  protected def _postPageAllEnable(result: PageModel[M], preResult: Any, pageNumber: Long, pageSize: Long, request: Option[R]): Resp[PageModel[M]] = {
    Resp.success(result)
  }

  protected def _doPageAllEnable(pageNumber: Long, pageSize: Long, request: Option[R]): Resp[PageModel[M]]

  protected def _executePageAllEnable(pageNumber: Long, pageSize: Long, request: Option[R]): Resp[PageModel[M]] = {
    if (classOf[StatusModel].isAssignableFrom(_modelClazz)) {
      val preResult = _prePageAllEnable(pageNumber, pageSize, request)
      if (preResult) {
        val result = _doPageAllEnable(pageNumber, pageSize, request)
        if (result) {
          result.body.setResults(_convertToViews(result.body.results, request))
          _postPageAllEnable(result.body, preResult.body, pageNumber, pageSize, request)
        } else {
          result
        }
      } else {
        preResult
      }
    } else {
      Resp.badRequest("The model not extend [StatusModel]")
    }
  }

  //=========================PageAllDisable=========================

  protected def _prePageAllDisable(pageNumber: Long, pageSize: Long, request: Option[R]): Resp[Any] = {
    Resp.success(null)
  }

  protected def _postPageAllDisable(result: PageModel[M], preResult: Any, pageNumber: Long, pageSize: Long, request: Option[R]): Resp[PageModel[M]] = {
    Resp.success(result)
  }

  protected def _doPageAllDisable(pageNumber: Long, pageSize: Long, request: Option[R]): Resp[PageModel[M]]

  protected def _executePageAllDisable(pageNumber: Long, pageSize: Long, request: Option[R]): Resp[PageModel[M]] = {
    if (classOf[StatusModel].isAssignableFrom(_modelClazz)) {
      val preResult = _prePageAllDisable(pageNumber, pageSize, request)
      if (preResult) {
        val result = _doPageAllDisable(pageNumber, pageSize, request)
        if (result) {
          result.body.setResults(_convertToViews(result.body.results, request))
          _postPageAllDisable(result.body, preResult.body, pageNumber, pageSize, request)
        } else {
          result
        }
      } else {
        preResult
      }
    } else {
      Resp.badRequest("The model not extend [StatusModel]")
    }
  }

  //=========================PageByCondition=========================

  protected def _prePageByCondition(condition: String, parameters: Option[List[Any]], pageNumber: Long, pageSize: Long, request: Option[R]): Resp[Any] = {
    Resp.success(null)
  }

  protected def _postPageByCondition(result: PageModel[M], preResult: Any, pageNumber: Long, pageSize: Long, request: Option[R]): Resp[PageModel[M]] = {
    Resp.success(result)
  }

  protected def _doPageByCondition(condition: String, parameters: Option[List[Any]], pageNumber: Long, pageSize: Long, request: Option[R]): Resp[PageModel[M]]

  protected def _executePageByCondition(condition: String, parameters: Option[List[Any]], pageNumber: Long, pageSize: Long, request: Option[R]): Resp[PageModel[M]] = {
    val preResult = _prePageByCondition(condition, parameters, pageNumber, pageSize, request)
    if (preResult) {
      val result = _doPageByCondition(condition, parameters, pageNumber, pageSize, request)
      if (result) {
        result.body.setResults(_convertToViews(result.body.results, request))
        _postPageByCondition(result.body, preResult.body, pageNumber, pageSize, request)
      } else {
        result
      }
    } else {
      preResult
    }
  }

  //=========================Save=========================

  protected def _preSave(model: M, request: Option[R]): Resp[Any] = {
    Resp.success(null)
  }

  protected def _postSave(result: String, preResult: Any, request: Option[R]): Resp[String] = {
    Resp.success(result)
  }

  protected def _executeSaveOrUpdate(model: M, request: Option[R]): Resp[String] = {
    model match {
      case idModel: IdModel =>
        if (idModel.id != null && idModel.id.trim != "" && _doGetById(idModel.id, request).body != null) {
          _executeUpdate(idModel.id, model, request)
        } else {
          _executeSave(model, request)
        }
      case _ => _executeSave(model, request)
    }
  }

  protected def _doSave(model: M, request: Option[R]): Resp[String]

  protected def _executeSave(model: M, request: Option[R]): Resp[String] = {
    model match {
      case idModel: IdModel =>
        if (idModel.id != null && idModel.id.trim != "" && _doGetById(idModel.id, request).body != null) {
          return Resp.badRequest("Id exist :" + idModel.id)
        }
        idModel match {
          case secureModel: SecureModel =>
            secureModel.create_time = System.currentTimeMillis()
            secureModel.create_user = if (request.isDefined) request.get.login_Id else ""
            secureModel.create_organization = if (request.isDefined) request.get.organization_id else ""
            secureModel.update_time = System.currentTimeMillis()
            secureModel.update_user = if (request.isDefined) request.get.login_Id else ""
            secureModel.update_organization = if (request.isDefined) request.get.organization_id else ""
          case _ =>
        }
      case _ =>
    }
    val preResult = _preSave(model, request)
    if (preResult) {
      val result = _doSave(model, request)
      if (result) {
        _postSave(result.body, preResult.body, request)
      } else {
        result
      }
    } else {
      preResult
    }
  }

  //=========================Update=========================

  protected def _preUpdate(id: String, model: M, request: Option[R]): Resp[Any] = {
    Resp.success(null)
  }

  protected def _postUpdate(result: String, preResult: Any, request: Option[R]): Resp[String] = {
    Resp.success(result)
  }

  protected def _doUpdate(id: String, model: M, request: Option[R]): Resp[String]

  protected def _executeUpdate(id: String, model: M, request: Option[R]): Resp[String] = {
    val getResult = _doGetById(id, request)
    if (getResult) {
      //获取已存储对象的创建时间（因为时间字段是long，不等于null，所以model.create_time=0不会被覆盖）
      val sCreateTime = if (model.isInstanceOf[SecureModel]) getResult.body.asInstanceOf[SecureModel].create_time else 0
      BeanHelper.copyProperties(getResult.body, model)
      val nModel = getResult.body
      nModel match {
        case idModel: IdModel =>
          idModel.id = id
          idModel match {
            case secureModel: SecureModel =>
              secureModel.update_time = System.currentTimeMillis()
              secureModel.update_user = if (request.isDefined) request.get.login_Id else ""
              secureModel.update_organization = if (request.isDefined) request.get.organization_id else ""
              secureModel.create_time = sCreateTime
            case _ =>
          }
        case _ =>
      }
      val preResult = _preUpdate(id, nModel, request)
      if (preResult) {
        val result = _doUpdate(id, nModel, request)
        if (result) {
          _postUpdate(result.body, preResult.body, request)
        } else {
          result
        }
      } else {
        preResult
      }
    } else {
      getResult
    }
  }

  //=========================DeleteById=========================

  protected def _preDeleteById(id: String, request: Option[R]): Resp[Any] = {
    Resp.success(null)
  }

  protected def _postDeleteById(result: String, preResult: Any, request: Option[R]): Resp[String] = {
    Resp.success(result)
  }

  protected def _doDeleteById(id: String, request: Option[R]): Resp[String]

  protected def _executeDeleteById(id: String, request: Option[R]): Resp[String] = {
    val preResult = _preDeleteById(id, request)
    if (preResult) {
      val result = _doDeleteById(id, request)
      if (result) {
        _postDeleteById(result.body, preResult.body, request)
      } else {
        result
      }
    } else {
      preResult
    }
  }

  //=========================DeleteByCondition=========================

  protected def _preDeleteByCondition(condition: String, parameters: Option[List[Any]], request: Option[R]): Resp[Any] = {
    Resp.success(null)
  }

  protected def _postDeleteByCondition(result: List[String], preResult: Any, request: Option[R]): Resp[List[String]] = {
    Resp.success(result)
  }

  protected def _doDeleteByCondition(condition: String, parameters: Option[List[Any]], request: Option[R]): Resp[List[String]]

  protected def _executeDeleteByCondition(condition: String, parameters: Option[List[Any]], request: Option[R]): Resp[List[String]] = {
    val preResult = _preDeleteByCondition(condition, parameters, request)
    if (preResult) {
      val result = _doDeleteByCondition(condition, parameters, request)
      if (result) {
        _postDeleteByCondition(result.body, preResult.body, request)
      } else {
        result
      }
    } else {
      preResult
    }
  }

  //=========================DeleteAll=========================

  protected def _preDeleteAll(request: Option[R]): Resp[Any] = {
    Resp.success(null)
  }

  protected def _postDeleteAll(result: List[String], preResult: Any, request: Option[R]): Resp[List[String]] = {
    Resp.success(result)
  }

  protected def _doDeleteAll(request: Option[R]): Resp[List[String]]

  protected def _executeDeleteAll(request: Option[R]): Resp[List[String]] = {
    val preResult = _preDeleteAll(request)
    if (preResult) {
      val result = _doDeleteAll(request)
      if (result) {
        _postDeleteAll(result.body, preResult.body, request)
      } else {
        result
      }
    } else {
      preResult
    }
  }

}

object _AuthType extends Enumeration {
  type _AuthType = Value
  val BY_CREATE_USER, BY_UPDATE_USER, BY_CREATE_ORGANIZATION, BY_UPDATE_ORGANIZATION = Value
}


