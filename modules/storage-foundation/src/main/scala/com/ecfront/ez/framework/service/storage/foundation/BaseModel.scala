package com.ecfront.ez.framework.service.storage.foundation

import java.lang.reflect.ParameterizedType

import com.ecfront.common.{BeanHelper, Resp}
import com.typesafe.scalalogging.slf4j.LazyLogging
import scala.beans.BeanProperty

trait BaseModel extends Serializable {

  @Id("seq")
  @BeanProperty var id: String = _

}

object BaseModel {

  val Id_FLAG = "id"
  val SPLIT = "@"

}

trait BaseStorage[M <: BaseModel] extends LazyLogging {

  protected val _modelClazz = this.getClass.getGenericInterfaces()(0).asInstanceOf[ParameterizedType].getActualTypeArguments()(0).asInstanceOf[Class[M]]

  val tableName = _modelClazz.getSimpleName.toLowerCase

  protected def storageCheck[E <: BaseEntityInfo](model: M, entityInfo: E): Resp[Void] = {
    if (entityInfo.requireFieldNames.nonEmpty) {
      val errorFields = entityInfo.requireFieldNames.filter(BeanHelper.getValue(model, _).get == null).map {
        requireField =>
          if (entityInfo.fieldLabel.contains(requireField)) {
            entityInfo.fieldLabel(requireField)
          } else {
            requireField
          }
      }
      if (errorFields.nonEmpty) {
        Resp.badRequest(errorFields.mkString("[", ",", "]") + " not null")
      } else {
        Resp.success(null)
      }
    } else {
      Resp.success(null)
    }
  }

  protected def preSave(model: M, context: EZStorageContext): Resp[M] = Resp.success(model)

  protected def postSave(doResult: M, context: EZStorageContext): Resp[M] = Resp.success(doResult)

  def save(model: M, context: EZStorageContext = EZStorageContext()): Resp[M] = {
    val preR = preSave(model, context)
    if (preR) {
      val doR = doSave(preR.body, context)
      if (doR) {
        postSave(doR.body, context)
      } else {
        doR
      }
    } else {
      preR
    }
  }

  protected def doSave(model: M, context: EZStorageContext): Resp[M]

  protected def preUpdate(model: M, context: EZStorageContext): Resp[M] = Resp.success(model)

  protected def postUpdate(doResult: M, context: EZStorageContext): Resp[M] = Resp.success(doResult)

  def update(model: M, context: EZStorageContext = EZStorageContext()): Resp[M] = {
    val preR = preUpdate(model, context)
    if (preR) {
      val doR = doUpdate(preR.body, context)
      if (doR) {
        postUpdate(doR.body, context)
      } else {
        doR
      }
    } else {
      preR
    }
  }

  protected def doUpdate(model: M, context: EZStorageContext): Resp[M]

  protected def preSaveOrUpdate(model: M, context: EZStorageContext): Resp[M] = Resp.success(model)

  protected def postSaveOrUpdate(doResult: M, context: EZStorageContext): Resp[M] = Resp.success(doResult)

  def saveOrUpdate(model: M, context: EZStorageContext = EZStorageContext()): Resp[M] = {
    val preR = preSaveOrUpdate(model, context)
    if (preR) {
      val doR = doSaveOrUpdate(preR.body, context)
      if (doR) {
        postSaveOrUpdate(doR.body, context)
      } else {
        doR
      }
    } else {
      preR
    }
  }

  protected def doSaveOrUpdate(model: M, context: EZStorageContext): Resp[M]

  protected def preUpdateByCond(newValues: String, condition: String, parameters: List[Any], context: EZStorageContext): Resp[(String, String, List[Any])] = Resp.success((newValues, condition, parameters))

  protected def postUpdateByCond(newValues: String, condition: String, parameters: List[Any], context: EZStorageContext): Resp[Void] = Resp.success(null)

  def updateByCond(newValues: String, condition: String, parameters: List[Any] = List(), context: EZStorageContext = EZStorageContext()): Resp[Void] = {
    val preR = preUpdateByCond(newValues, condition, parameters, context)
    if (preR) {
      val doR = doUpdateByCond(preR.body._1, preR.body._2, preR.body._3, context)
      if (doR) {
        postUpdateByCond(preR.body._1, preR.body._2, preR.body._3, context)
      } else {
        doR
      }
    } else {
      preR
    }
  }

  protected def doUpdateByCond(newValues: String, condition: String, parameters: List[Any], context: EZStorageContext): Resp[Void]

  protected def preDeleteById(id: Any, context: EZStorageContext): Resp[Any] = Resp.success(id)

  protected def postDeleteById(id: Any, context: EZStorageContext): Resp[Void] = Resp.success(null)

  def deleteById(id: Any, context: EZStorageContext = EZStorageContext()): Resp[Void] = {
    if (id == null) {
      Resp.badRequest("【id】not null")
    } else {
      val preR = preDeleteById(id, context)
      if (preR) {
        val doR = doDeleteById(preR.body, context)
        if (doR) {
          postDeleteById(preR.body, context)
        } else {
          doR
        }
      } else {
        preR
      }
    }
  }

  protected def doDeleteById(id: Any, context: EZStorageContext): Resp[Void]

  protected def preDeleteByCond(condition: String, parameters: List[Any], context: EZStorageContext): Resp[(String, List[Any])] = Resp.success((condition, parameters))

  protected def postDeleteByCond(condition: String, parameters: List[Any], context: EZStorageContext): Resp[Void] = Resp.success(null)

  def deleteByCond(condition: String, parameters: List[Any] = List(), context: EZStorageContext = EZStorageContext()): Resp[Void] = {
    if (condition == null) {
      Resp.badRequest("【condition】not null")
    } else {
      val preR = preDeleteByCond(condition, parameters, context)
      if (preR) {
        val doR = doDeleteByCond(preR.body._1, preR.body._2, context)
        if (doR) {
          postDeleteByCond(preR.body._1, preR.body._2, context)
        } else {
          doR
        }
      } else {
        preR
      }
    }
  }

  protected def doDeleteByCond(condition: String, parameters: List[Any], context: EZStorageContext): Resp[Void]

  protected def preGetById(id: Any, context: EZStorageContext): Resp[Any] = Resp.success(id)

  protected def postGetById(id: Any, doResult: M, context: EZStorageContext): Resp[M] = Resp.success(doResult)

  def getById(id: Any, context: EZStorageContext = EZStorageContext()): Resp[M] = {
    if (id == null) {
      Resp.badRequest("【id】not null")
    } else {
      val preR = preGetById(id, context)
      if (preR) {
        val doR = doGetById(preR.body, context)
        if (doR) {
          postGetById(preR.body, doR.body, context)
        } else {
          doR
        }
      } else {
        preR
      }
    }
  }

  protected def doGetById(id: Any, context: EZStorageContext): Resp[M]

  protected def preGetByCond(condition: String, parameters: List[Any], context: EZStorageContext): Resp[(String, List[Any])] = Resp.success((condition, parameters))

  protected def postGetByCond(condition: String, parameters: List[Any], doResult: M, context: EZStorageContext): Resp[M] = Resp.success(doResult)

  def getByCond(condition: String, parameters: List[Any] = List(), context: EZStorageContext = EZStorageContext()): Resp[M] = {
    if (condition == null) {
      Resp.badRequest("【condition】not null")
    } else {
      val preR = preGetByCond(condition, parameters, context)
      if (preR) {
        val doR = doGetByCond(preR.body._1, preR.body._2, context)
        if (doR) {
          postGetByCond(preR.body._1, preR.body._2, doR.body, context)
        } else {
          doR
        }
      } else {
        preR
      }
    }
  }

  protected def doGetByCond(condition: String, parameters: List[Any], context: EZStorageContext): Resp[M]

  protected def preExistById(id: Any, context: EZStorageContext): Resp[Any] = Resp.success(id)

  protected def postExistById(id: Any, doResult: Boolean, context: EZStorageContext): Resp[Boolean] = Resp.success(doResult)

  def existById(id: Any, context: EZStorageContext = EZStorageContext()): Resp[Boolean] = {
    if (id == null) {
      Resp.badRequest("【id】not null")
    } else {
      val preR = preExistById(id, context)
      if (preR) {
        val doR = doExistById(preR.body, context)
        if (doR) {
          postExistById(preR.body, doR.body, context)
        } else {
          doR
        }
      } else {
        preR
      }
    }
  }

  protected def doExistById(id: Any, context: EZStorageContext = null): Resp[Boolean]

  protected def preExistByCond(condition: String, parameters: List[Any], context: EZStorageContext): Resp[(String, List[Any])] = Resp.success((condition, parameters))

  protected def postExistByCond(condition: String, parameters: List[Any], doResult: Boolean, context: EZStorageContext): Resp[Boolean] = Resp.success(doResult)

  def existByCond(condition: String, parameters: List[Any] = List(), context: EZStorageContext = EZStorageContext()): Resp[Boolean] = {
    if (condition == null) {
      Resp.badRequest("【condition】not null")
    } else {
      val preR = preExistByCond(condition, parameters, context)
      if (preR) {
        val doR = doExistByCond(preR.body._1, preR.body._2, context)
        if (doR) {
          postExistByCond(preR.body._1, preR.body._2, doR.body, context)
        } else {
          doR
        }
      } else {
        preR
      }
    }
  }

  protected def doExistByCond(condition: String, parameters: List[Any], context: EZStorageContext): Resp[Boolean]

  protected def preFind(condition: String, parameters: List[Any], context: EZStorageContext): Resp[(String, List[Any])] = Resp.success((condition, parameters))

  protected def postFind(condition: String, parameters: List[Any], doResult: List[M], context: EZStorageContext): Resp[List[M]] = Resp.success(doResult)

  def find(condition: String, parameters: List[Any] = List(), context: EZStorageContext = EZStorageContext()): Resp[List[M]] = {
    val preR = preFind(condition, parameters, context)
    if (preR) {
      val doR = doFind(preR.body._1, preR.body._2, context)
      if (doR) {
        postFind(preR.body._1, preR.body._2, doR.body, context)
      } else {
        doR
      }
    } else {
      preR
    }
  }

  protected def doFind(condition: String, parameters: List[Any], context: EZStorageContext): Resp[List[M]]

  protected def prePage(condition: String, parameters: List[Any], pageNumber: Long, pageSize: Int, context: EZStorageContext): Resp[(String, List[Any])] = Resp.success((condition, parameters))

  protected def postPage(condition: String, parameters: List[Any], pageNumber: Long, pageSize: Int, doResult: Page[M], context: EZStorageContext): Resp[Page[M]] = Resp.success(doResult)

  def page(condition: String, parameters: List[Any] = List(), pageNumber: Long = 1, pageSize: Int = 10, context: EZStorageContext = EZStorageContext()): Resp[Page[M]] = {
    val preR = prePage(condition, parameters, pageNumber, pageSize, context)
    if (preR) {
      val doR = doPage(preR.body._1, preR.body._2, pageNumber, pageSize, context)
      if (doR) {
        postPage(preR.body._1, preR.body._2, pageNumber, pageSize, doR.body, context)
      } else {
        doR
      }
    } else {
      preR
    }
  }

  protected def doPage(condition: String, parameters: List[Any], pageNumber: Long, pageSize: Int, context: EZStorageContext): Resp[Page[M]]

  protected def preCount(condition: String, parameters: List[Any], context: EZStorageContext): Resp[(String, List[Any])] = Resp.success((condition, parameters))

  protected def postCount(condition: String, parameters: List[Any], doResult: Long, context: EZStorageContext): Resp[Long] = Resp.success(doResult)

  def count(condition: String, parameters: List[Any] = List(), context: EZStorageContext = EZStorageContext()): Resp[Long] = {
    val preR = preCount(condition, parameters, context)
    if (preR) {
      val doR = doCount(preR.body._1, preR.body._2, context)
      if (doR) {
        postCount(preR.body._1, preR.body._2, doR.body, context)
      } else {
        doR
      }
    } else {
      preR
    }
  }

  protected def doCount(condition: String, parameters: List[Any], context: EZStorageContext): Resp[Long]

}



