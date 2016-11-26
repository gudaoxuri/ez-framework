package com.ecfront.ez.framework.service.jdbc

import java.lang.reflect.ParameterizedType

import com.ecfront.common.{BeanHelper, Resp}
import com.ecfront.ez.framework.core.EZ
import com.ecfront.ez.framework.core.i18n.I18NProcessor._
import com.ecfront.ez.framework.core.logger.Logging

import scala.beans.BeanProperty
import scala.reflect.runtime._

/**
  * 实体基类，所有实体都应继承此类
  */
abstract class BaseModel extends Serializable {

  @Id("seq")
  @BeanProperty var id: String = _

}

object BaseModel {

  val Id_FLAG = "id"
  val SPLIT = EZ.eb.ADDRESS_SPLIT_FLAG
  // 默认系统管理员角色
  val SYSTEM_ROLE_FLAG = "system"
  // 组织管理员角色
  val ORG_ADMIN_ROLE_FLAG = "org_admin"
  // 默认普通用户角色
  val USER_ROLE_FLAG = "user"
  val SYSTEM_ACCOUNT_LOGIN_ID = "sysadmin"
  val ORG_ADMIN_ACCOUNT_LOGIN_ID = "admin"

}

/**
  * 基础持久化类，所有实体持久化都应继承此类
  *
  * @tparam M 实体类型
  */
trait BaseStorage[M <: BaseModel] extends Logging {

  protected val _runtimeMirror = universe.runtimeMirror(getClass.getClassLoader)

  protected val _modelClazz = this.getClass.getGenericInterfaces()(0)
    .asInstanceOf[ParameterizedType].getActualTypeArguments()(0).asInstanceOf[Class[M]]

  // 表名
  lazy val tableName = _modelClazz.getSimpleName.toLowerCase

  protected var _entityInfo =
    if (!EntityContainer.CONTAINER.contains(tableName)) {
      EntityContainer.buildingEntityInfo(_modelClazz, tableName)
    } else {
      EntityContainer.CONTAINER(tableName)
    }

  def filterByModel(model: M): Resp[M] = Resp.success(model)

  def filterById(id: Any): Resp[(String, List[Any])] = Resp.success(null)

  def filterByUUID(uuid: String): Resp[(String, List[Any])] = Resp.success(null)

  def filterByCond(condition: String, parameters: List[Any]): Resp[(String, List[Any])] = Resp.success((condition, parameters))

  /**
    * 持久化前检查
    *
    * @param model      实体对象
    * @param entityInfo 实体信息
    * @param isUpdate   是否是更新操作
    * @tparam E 实体类型
    * @return 是否通过
    */
  protected def storageCheck[E <: EntityInfo](model: M, entityInfo: E, isUpdate: Boolean): Resp[Void] = {
    if (entityInfo.requireFieldNames.nonEmpty) {
      if (!isUpdate) {
        // 必填项检查
        val errorFields = entityInfo.requireFieldNames.filter(BeanHelper.getValue(model, _).get == null).map {
          requireField =>
            if (entityInfo.fieldDesc.contains(requireField)) {
              entityInfo.fieldDesc(requireField)._1.x
            } else {
              requireField.x
            }
        }
        if (errorFields.nonEmpty) {
          logger.warn(errorFields.mkString("[", ",", "]") + " not null")
          Resp.badRequest(errorFields.mkString("[", ",", "]") + " not null")
        } else {
          Resp.success(null)
        }
      } else {
        Resp.success(null)
      }
    } else {
      Resp.success(null)
    }
  }

  /**
    * 保存前处理
    *
    * @param model 实体对象
    * @return 是否允许保存
    */
  def preSave(model: M): Resp[M] = Resp.success(model)

  /**
    * 保存后处理
    *
    * @param saveResult 保存后的实体对象
    * @param preResult  保存前的实体对象
    * @return 处理后的实体对象
    */
  def postSave(saveResult: M, preResult: M): Resp[M] = Resp.success(saveResult)

  /**
    * 保存
    *
    * @param model 实体对象
    * @return 保存后的实体对象
    */
  def save(model: M, skipFilter: Boolean = false): Resp[M] = {
    val _model = _modelClazz.newInstance()
    BeanHelper.copyProperties(_model, model)
    val preR = preSave(_model)
    if (preR) {
      val filterR =
        if (!skipFilter) {
          filterByModel(preR.body)
        } else {
          Resp.success(preR.body)
        }
      if (filterR) {
        val doR = doSave(filterR.body)
        if (doR) {
          postSave(doR.body, filterR.body)
        } else {
          doR
        }
      } else {
        filterR
      }
    } else {
      preR
    }
  }

  /**
    * 保存实现方法
    *
    * @param model 实体对象
    * @return 保存后的实体对象
    */
  protected def doSave(model: M): Resp[M] = {
    val requireResp = storageCheck(model, _entityInfo, isUpdate = false)
    if (requireResp) {
      JDBCExecutor.save(_entityInfo, getMapValue(model).filter(i => i._2 != null
        && !_entityInfo.nowBySaveFieldNames.contains(i._1)
        && !_entityInfo.nowByUpdateFieldNames.contains(i._1)
      ), _modelClazz)
    } else {
      requireResp
    }
  }

  /**
    * 更新前处理
    *
    * @param model 实体对象
    * @return 是否允许更新
    */
  def preUpdate(model: M): Resp[M] = Resp.success(model)

  /**
    * 更新后处理
    *
    * @param updateResult 更新后的实体对象
    * @param preResult    更新前的实体对象
    * @return 处理后的实体对象
    */
  def postUpdate(updateResult: M, preResult: M): Resp[M] = Resp.success(updateResult)

  /**
    * 更新
    *
    * @param model 实体对象
    * @return 更新后的实体对象
    */
  def update(model: M, skipFilter: Boolean = false): Resp[M] = {
    val _model = _modelClazz.newInstance()
    BeanHelper.copyProperties(_model, model)
    if (_model.id == null) {
      _model.id = doGetByUUID(getUUIDValue(_model)).body.id
    }
    val preR = preUpdate(_model)
    if (preR) {
      val filterR =
        if (!skipFilter) {
          filterByModel(preR.body)
        } else {
          Resp.success(preR.body)
        }
      if (filterR) {
        val doR = doUpdate(filterR.body)
        if (doR) {
          postUpdate(doR.body, filterR.body)
        } else {
          doR
        }
      } else {
        filterR
      }
    } else {
      preR
    }
  }

  /**
    * 更新实现方法
    *
    * @param model 实体对象
    * @return 更新后的实体对象
    */
  protected def doUpdate(model: M): Resp[M] = {
    val requireResp = storageCheck(model, _entityInfo, isUpdate = true)
    if (requireResp) {
      JDBCExecutor.update(_entityInfo, getIdValue(model),
        getMapValue(model).filter(i => i._2 != null
          && !_entityInfo.nowBySaveFieldNames.contains(i._1)
          && !_entityInfo.nowByUpdateFieldNames.contains(i._1)
        ), _modelClazz)
    } else {
      requireResp
    }
  }

  /**
    * 保存或更新前处理
    *
    * @param model 实体对象
    * @return 是否允许保存或更新
    */
  def preSaveOrUpdate(model: M): Resp[M] = Resp.success(model)

  /**
    * 保存或更新后处理
    *
    * @param saveOrUpdateResult 保存或更新后的实体对象
    * @param preResult          保存或更新前的实体对象
    * @return 处理后的实体对象
    */
  def postSaveOrUpdate(saveOrUpdateResult: M, preResult: M): Resp[M] = Resp.success(saveOrUpdateResult)

  /**
    * 保存或更新
    *
    * @param model 实体对象
    * @return 保存或更新后的实体对象
    */
  def saveOrUpdate(model: M, skipFilter: Boolean = false): Resp[M] = {
    val _model = _modelClazz.newInstance()
    BeanHelper.copyProperties(_model, model)
    if (_model.id != null) {
      _model.id = _model.id.trim
    }
    val preR = preSaveOrUpdate(_model)
    if (preR) {
      val filterR =
        if (!skipFilter) {
          filterByModel(preR.body)
        } else {
          Resp.success(preR.body)
        }
      if (filterR) {
        val doR = doSaveOrUpdate(filterR.body)
        if (doR) {
          postSaveOrUpdate(doR.body, filterR.body)
        } else {
          doR
        }
      } else {
        filterR
      }
    } else {
      preR
    }
  }

  /**
    * 保存或更新实现方法
    *
    * @param model 实体对象
    * @return 保存或更新后的实体对象
    */
  protected def doSaveOrUpdate(model: M): Resp[M] = {
    val requireResp = storageCheck(model, _entityInfo, model.id != null && model.id.nonEmpty)
    if (requireResp) {
      JDBCExecutor.saveOrUpdate(_entityInfo, getIdValue(model),
        getMapValue(model).filter(i => i._2 != null
          && !_entityInfo.nowBySaveFieldNames.contains(i._1)
          && !_entityInfo.nowByUpdateFieldNames.contains(i._1)
        ), _modelClazz)
    } else {
      requireResp
    }
  }

  /**
    * 更新前处理
    *
    * @param newValues  新值，SQL (相当于SET中的条件)
    * @param condition  条件，SQL (相当于Where中的条件)
    * @param parameters 参数
    * @return 是否允许更新
    */
  def preUpdateByCond(
                       newValues: String, condition: String, parameters: List[Any]): Resp[(String, String, List[Any])] =
  Resp.success((newValues, condition, parameters))

  /**
    * 更新后处理
    *
    * @param newValues  新值，SQL (相当于SET中的条件)
    * @param condition  条件，SQL (相当于Where中的条件)
    * @param parameters 参数
    * @return 是否成功
    */
  def postUpdateByCond(newValues: String, condition: String, parameters: List[Any]): Resp[Void] = Resp.success(null)

  /**
    * 更新
    *
    * @param newValues  新值，SQL (相当于SET中的条件)
    * @param condition  条件，SQL (相当于Where中的条件)
    * @param parameters 参数
    * @return 是否成功
    */
  def updateByCond(newValues: String, condition: String, parameters: List[Any] = List()): Resp[Void] = {
    val preR = preUpdateByCond(newValues, condition, parameters)
    if (preR) {
      val filterR = filterByCond(preR.body._2, preR.body._3)
      if (filterR) {
        val doR = doUpdateByCond(preR.body._1, filterR.body._1, filterR.body._2)
        if (doR) {
          postUpdateByCond(preR.body._1, filterR.body._1, filterR.body._2)
        } else {
          doR
        }
      } else {
        filterR
      }
    } else {
      preR
    }
  }

  /**
    * 更新实现方法
    *
    * @param newValues  新值，SQL (相当于SET中的条件)
    * @param condition  条件，SQL (相当于Where中的条件)
    * @param parameters 参数
    * @return 是否成功
    */
  protected def doUpdateByCond(newValues: String, condition: String, parameters: List[Any]): Resp[Void] = {
    JDBCProcessor.update(
      s"UPDATE $tableName Set $newValues WHERE ${packageCondition(condition)}",
      parameters
    )
  }

  /**
    * 删除前处理
    *
    * @param id 主键
    * @return 是否允许删除
    */
  def preDeleteById(id: Any): Resp[Any] = Resp.success(id)

  /**
    * 删除后处理
    *
    * @param id 主键
    * @return 是否成功
    */
  def postDeleteById(id: Any): Resp[Void] = Resp.success(null)

  /**
    * 删除
    *
    * @param id 主键
    * @return 是否成功
    */
  def deleteById(id: Any): Resp[Void] = {
    if (id == null) {
      logger.warn("【id】not null")
      Resp.badRequest("【id】not null")
    } else {
      val preR = preDeleteById(id)
      if (preR) {
        val filterR = filterById(id)
        if (filterR) {
          val doR = if (filterR.body == null) doDeleteById(preR.body) else doDeleteByCond(filterR.body._1, filterR.body._2)
          if (doR) {
            postDeleteById(preR.body)
          } else {
            doR
          }
        } else {
          filterR
        }
      } else {
        preR
      }
    }
  }

  /**
    * 删除实现方法
    *
    * @param id 主键
    * @return 是否成功
    */
  protected def doDeleteById(id: Any): Resp[Void] = {
    JDBCProcessor.update(
      s"DELETE FROM $tableName WHERE ${_entityInfo.idFieldName} = ? ",
      List(id)
    )
  }

  /**
    * 删除前处理
    *
    * @param uuid 主键
    * @return 是否允许删除
    */
  def preDeleteByUUID(uuid: String): Resp[String] = Resp.success(uuid)

  /**
    * 删除后处理
    *
    * @param uuid 主键
    * @return 是否成功
    */
  def postDeleteByUUID(uuid: String): Resp[Void] = Resp.success(null)

  /**
    * 删除
    *
    * @param uuid 主键
    * @return 是否成功
    */
  def deleteByUUID(uuid: String): Resp[Void] = {
    if (uuid == null) {
      logger.warn("【uuid】not null")
      Resp.badRequest("【uuid】not null")
    } else {
      val preR = preDeleteByUUID(uuid)
      if (preR) {
        val filterR = filterByUUID(uuid)
        if (filterR) {
          val doR = if (filterR.body == null) doDeleteByUUID(preR.body) else doDeleteByCond(filterR.body._1, filterR.body._2)
          if (doR) {
            postDeleteByUUID(preR.body)
          } else {
            doR
          }
        } else {
          filterR
        }
      } else {
        preR
      }
    }
  }

  /**
    * 删除实现方法
    *
    * @param uuid 主键
    * @return 是否成功
    */
  protected def doDeleteByUUID(uuid: String): Resp[Void] = {
    JDBCProcessor.update(
      s"DELETE FROM $tableName WHERE ${_entityInfo.uuidFieldName} = ? ",
      List(uuid)
    )
  }

  /**
    * 删除前处理
    *
    * @param condition  条件，SQL (相当于Where中的条件)
    * @param parameters 参数
    * @return 是否允许删除
    */
  def preDeleteByCond(
                       condition: String, parameters: List[Any]): Resp[(String, List[Any])] =
  Resp.success((condition, parameters))

  /**
    * 删除后处理
    *
    * @param condition  条件，SQL (相当于Where中的条件)
    * @param parameters 参数
    * @return 是否成功
    */
  def postDeleteByCond(condition: String, parameters: List[Any]): Resp[Void] = Resp.success(null)

  /**
    * 删除
    *
    * @param condition  条件，SQL (相当于Where中的条件)
    * @param parameters 参数
    * @return 是否成功
    */
  def deleteByCond(condition: String, parameters: List[Any] = List()): Resp[Void] = {
    if (condition == null) {
      logger.warn("【condition】not null")
      Resp.badRequest("【condition】not null")
    } else {
      val preR = preDeleteByCond(condition, parameters)
      if (preR) {
        val filterR = filterByCond(preR.body._1, preR.body._2)
        if (filterR) {
          val doR = doDeleteByCond(filterR.body._1, filterR.body._2)
          if (doR) {
            postDeleteByCond(filterR.body._1, filterR.body._2)
          } else {
            doR
          }
        } else {
          filterR
        }
      } else {
        preR
      }
    }
  }

  /**
    * 删除实现方法
    *
    * @param condition  条件，SQL (相当于Where中的条件)
    * @param parameters 参数
    * @return 是否成功
    */
  protected def doDeleteByCond(condition: String, parameters: List[Any]): Resp[Void] = {
    JDBCProcessor.update(
      s"DELETE FROM $tableName WHERE ${packageCondition(condition)} ",
      parameters
    )
  }

  /**
    * 获取一条记录前处理
    *
    * @param id 主键
    * @return 是否允许获取这条记录
    */
  def preGetById(id: Any): Resp[Any] = Resp.success(id)

  /**
    * 获取一条记录后处理
    *
    * @param id        主键
    * @param getResult 获取到的记录
    * @return 处理后的记录
    */
  def postGetById(id: Any, getResult: M): Resp[M] = Resp.success(getResult)

  /**
    * 获取一条记录
    *
    * @param id 主键
    * @return 获取到的记录
    */
  def getById(id: Any): Resp[M] = {
    if (id == null) {
      logger.warn("【id】not null")
      Resp.badRequest("【id】not null")
    } else {
      val preR = preGetById(id)
      if (preR) {
        val filterR = filterById(id)
        if (filterR) {
          val doR = if (filterR.body == null) doGetById(preR.body) else doGetByCond(filterR.body._1, filterR.body._2)
          if (doR) {
            postGetById(preR.body, doR.body)
          } else {
            doR
          }
        } else {
          filterR
        }
      } else {
        preR
      }
    }
  }

  /**
    * 获取一条记录实现方法
    *
    * @param id 主键
    * @return 获取到的记录
    */
  protected def doGetById(id: Any): Resp[M] = {
    JDBCProcessor.get(
      s"SELECT * FROM $tableName WHERE ${_entityInfo.idFieldName}  = ? ",
      List(id),
      _modelClazz
    )
  }


  /**
    * 获取一条记录前处理
    *
    * @param uuid 主键
    * @return 是否允许获取这条记录
    */
  def preGetByUUID(uuid: String): Resp[String] = Resp.success(uuid)

  /**
    * 获取一条记录后处理
    *
    * @param uuid      主键
    * @param getResult 获取到的记录
    * @return 处理后的记录
    */
  def postGetByUUID(uuid: String, getResult: M): Resp[M] = Resp.success(getResult)

  /**
    * 获取一条记录
    *
    * @param uuid 主键
    * @return 获取到的记录
    */
  def getByUUID(uuid: String): Resp[M] = {
    if (uuid == null) {
      logger.warn("【uuid】not null")
      Resp.badRequest("【uuid】not null")
    } else {
      val preR = preGetByUUID(uuid)
      if (preR) {
        val filterR = filterByUUID(uuid)
        if (filterR) {
          val doR = if (filterR.body == null) doGetByUUID(preR.body) else doGetByCond(filterR.body._1, filterR.body._2)
          if (doR) {
            postGetByUUID(preR.body, doR.body)
          } else {
            doR
          }
        } else {
          filterR
        }
      } else {
        preR
      }
    }
  }

  /**
    * 获取一条记录实现方法
    *
    * @param uuid 主键
    * @return 获取到的记录
    */
  protected def doGetByUUID(uuid: String): Resp[M] = {
    JDBCProcessor.get(
      s"SELECT * FROM $tableName WHERE ${_entityInfo.uuidFieldName}  = ? ",
      List(uuid),
      _modelClazz
    )
  }

  /**
    * 获取一条记录前处理
    *
    * @param condition  条件，SQL (相当于Where中的条件)
    * @param parameters 参数
    * @return 是否允许获取这条记录
    */
  def preGetByCond(condition: String,
                   parameters: List[Any]): Resp[(String, List[Any])] =
  Resp.success((condition, parameters))

  /**
    * 获取一条记录后处理
    *
    * @param condition  条件，SQL (相当于Where中的条件)
    * @param parameters 参数
    * @param getResult  获取到的记录
    * @return 处理后的记录
    */
  def postGetByCond(condition: String, parameters: List[Any], getResult: M): Resp[M] = Resp.success(getResult)

  /**
    * 获取一条记录
    *
    * @param condition  条件，SQL (相当于Where中的条件)
    * @param parameters 参数
    * @return 获取到的记录
    */
  def getByCond(condition: String, parameters: List[Any] = List()): Resp[M] = {
    if (condition == null) {
      logger.warn("【condition】not null")
      Resp.badRequest("【condition】not null")
    } else {
      val preR = preGetByCond(condition, parameters)
      if (preR) {
        val filterR = filterByCond(preR.body._1, preR.body._2)
        if (filterR) {
          val doR = doGetByCond(filterR.body._1, filterR.body._2)
          if (doR) {
            postGetByCond(filterR.body._1, filterR.body._2, doR.body)
          } else {
            doR
          }
        } else {
          filterR
        }
      } else {
        preR
      }
    }
  }

  /**
    * 获取一条记录实现方法
    *
    * @param condition  条件，SQL (相当于Where中的条件)
    * @param parameters 参数
    * @return 获取到的记录
    */
  protected def doGetByCond(condition: String, parameters: List[Any]): Resp[M] = {
    JDBCProcessor.get(
      s"SELECT * FROM $tableName WHERE ${packageCondition(condition)} ",
      parameters,
      _modelClazz
    )
  }

  /**
    * 判断是否存在前处理
    *
    * @param id 主键
    * @return 是否允许判断是否存在
    */
  def preExistById(id: Any): Resp[Any] = Resp.success(id)

  /**
    * 判断是否存在后处理
    *
    * @param id          主键
    * @param existResult 是否存在
    * @return 处理后的结果
    */
  def postExistById(id: Any, existResult: Boolean): Resp[Boolean] = Resp.success(existResult)

  /**
    * 判断是否存在
    *
    * @param id 主键
    * @return 是否存在
    */
  def existById(id: Any): Resp[Boolean] = {
    if (id == null) {
      logger.warn("【id】not null")
      Resp.badRequest("【id】not null")
    } else {
      val preR = preExistById(id)
      if (preR) {
        val filterR = filterById(id)
        if (filterR) {
          val doR = if (filterR.body == null) doExistById(preR.body) else doExistByCond(filterR.body._1, filterR.body._2)
          if (doR) {
            postExistById(preR.body, doR.body)
          } else {
            doR
          }
        } else {
          filterR
        }
      } else {
        preR
      }
    }
  }

  /**
    * 判断是否存在实现方法
    *
    * @param id 主键
    * @return 是否存在
    */
  protected def doExistById(id: Any): Resp[Boolean] = {
    JDBCProcessor.exist(
      s"SELECT 1 FROM $tableName WHERE ${_entityInfo.idFieldName}  = ? ",
      List(id)
    )
  }

  /**
    * 判断是否存在前处理
    *
    * @param uuid 主键
    * @return 是否允许判断是否存在
    */
  def preExistByUUID(uuid: String): Resp[String] = Resp.success(uuid)

  /**
    * 判断是否存在后处理
    *
    * @param uuid        主键
    * @param existResult 是否存在
    * @return 处理后的结果
    */
  def postExistByUUID(uuid: String, existResult: Boolean): Resp[Boolean] = Resp.success(existResult)

  /**
    * 判断是否存在
    *
    * @param uuid 主键
    * @return 是否存在
    */
  def existByUUID(uuid: String): Resp[Boolean] = {
    if (uuid == null) {
      logger.warn("【uuid】not null")
      Resp.badRequest("【uuid】not null")
    } else {
      val preR = preExistByUUID(uuid)
      if (preR) {
        val filterR = filterByUUID(uuid)
        if (filterR) {
          val doR = if (filterR.body == null) doExistByUUID(preR.body) else doExistByCond(filterR.body._1, filterR.body._2)
          if (doR) {
            postExistByUUID(preR.body, doR.body)
          } else {
            doR
          }
        } else {
          filterR
        }
      } else {
        preR
      }
    }
  }

  /**
    * 判断是否存在实现方法
    *
    * @param uuid 主键
    * @return 是否存在
    */
  protected def doExistByUUID(uuid: String): Resp[Boolean] = {
    JDBCProcessor.exist(
      s"SELECT 1 FROM $tableName WHERE ${_entityInfo.uuidFieldName}  = ? ",
      List(uuid)
    )
  }

  /**
    * 判断是否存在前处理
    *
    * @param condition  条件，SQL (相当于Where中的条件)
    * @param parameters 参数
    * @return 是否允许判断是否存在
    */
  def preExistByCond(condition: String, parameters: List[Any]): Resp[(String, List[Any])] =
  Resp.success((condition, parameters))

  /**
    * 判断是否存在后处理
    *
    * @param condition   条件，SQL (相当于Where中的条件)
    * @param parameters  参数
    * @param existResult 是否存在
    * @return 处理后的结果
    */
  def postExistByCond(condition: String, parameters: List[Any],
                      existResult: Boolean): Resp[Boolean] =
  Resp.success(existResult)

  /**
    * 判断是否存在
    *
    * @param condition  条件，SQL (相当于Where中的条件)
    * @param parameters 参数
    * @return 是否存在
    */
  def existByCond(condition: String, parameters: List[Any] = List()): Resp[Boolean] = {
    if (condition == null) {
      logger.warn("【condition】not null")
      Resp.badRequest("【condition】not null")
    } else {
      val preR = preExistByCond(condition, parameters)
      if (preR) {
        val filterR = filterByCond(preR.body._1, preR.body._2)
        if (filterR) {
          val doR = doExistByCond(filterR.body._1, filterR.body._2)
          if (doR) {
            postExistByCond(filterR.body._1, filterR.body._2, doR.body)
          } else {
            doR
          }
        } else {
          filterR
        }
      } else {
        preR
      }
    }
  }

  /**
    * 判断是否存在实现方法
    *
    * @param condition  条件，SQL (相当于Where中的条件)
    * @param parameters 参数
    * @return 是否存在
    */
  protected def doExistByCond(condition: String, parameters: List[Any]): Resp[Boolean] = {
    JDBCProcessor.exist(
      s"SELECT 1 FROM $tableName WHERE ${packageCondition(condition)} ",
      parameters
    )
  }

  /**
    * 查找前处理
    *
    * @param condition  条件，SQL (相当于Where中的条件)
    * @param parameters 参数
    * @return 是否允许查找
    */
  def preFind(condition: String, parameters: List[Any]): Resp[(String, List[Any])] = Resp.success((condition, parameters))

  /**
    * 查找后处理
    *
    * @param condition  条件，SQL (相当于Where中的条件)
    * @param parameters 参数
    * @param findResult 是否存在
    * @return 处理后的结果
    */
  def postFind(condition: String, parameters: List[Any], findResult: List[M]): Resp[List[M]] = Resp.success(findResult)

  /**
    * 查找
    *
    * @param condition  条件，SQL (相当于Where中的条件)
    * @param parameters 参数
    * @return 查找结果
    */
  def find(condition: String, parameters: List[Any] = List()): Resp[List[M]] = {
    val preR = preFind(condition, parameters)
    if (preR) {
      val filterR = filterByCond(preR.body._1, preR.body._2)
      if (filterR) {
        val doR = doFind(filterR.body._1, filterR.body._2)
        if (doR) {
          postFind(filterR.body._1, filterR.body._2, doR.body)
        } else {
          doR
        }
      } else {
        filterR
      }
    } else {
      preR
    }
  }

  /**
    * 查找实现方法
    *
    * @param condition  条件，SQL (相当于Where中的条件)
    * @param parameters 参数
    * @return 查找结果
    */
  protected def doFind(condition: String, parameters: List[Any]): Resp[List[M]] = {
    JDBCProcessor.find(
      s"SELECT * FROM $tableName WHERE ${packageCondition(condition)}",
      parameters,
      _modelClazz
    )
  }

  /**
    * 分页前处理
    *
    * @param condition  条件，SQL (相当于Where中的条件)
    * @param parameters 参数
    * @param pageNumber 当前页，从1开始
    * @param pageSize   每页条数
    * @return 是否允许分页
    */
  def prePage(condition: String, parameters: List[Any], pageNumber: Long, pageSize: Int): Resp[(String, List[Any])] = Resp.success((condition, parameters))

  /**
    * 分页后处理
    *
    * @param condition  条件，SQL (相当于Where中的条件)
    * @param parameters 参数
    * @param pageNumber 当前页，从1开始
    * @param pageSize   每页条数
    * @param pageResult 是否存在
    * @return 处理后的结果
    */
  def postPage(condition: String, parameters: List[Any], pageNumber: Long, pageSize: Int,
               pageResult: Page[M]): Resp[Page[M]] = Resp.success(pageResult)

  /**
    * 分页
    *
    * @param condition  条件，SQL (相当于Where中的条件)
    * @param parameters 参数
    * @param pageNumber 当前页，从1开始
    * @param pageSize   每页条数
    * @return 分页结果
    */
  def page(condition: String, parameters: List[Any] = List(), pageNumber: Long = 1, pageSize: Int = 10): Resp[Page[M]] = {
    val preR = prePage(condition, parameters, pageNumber, pageSize)
    if (preR) {
      val filterR = filterByCond(preR.body._1, preR.body._2)
      if (filterR) {
        val doR = doPage(filterR.body._1, filterR.body._2, pageNumber, pageSize)
        if (doR) {
          postPage(filterR.body._1, filterR.body._2, pageNumber, pageSize, doR.body)
        } else {
          doR
        }
      } else {
        filterR
      }
    } else {
      preR
    }
  }

  /**
    * 分页实现方法
    *
    * @param condition  条件，SQL (相当于Where中的条件)
    * @param parameters 参数
    * @param pageNumber 当前页，从1开始
    * @param pageSize   每页条数
    * @return 分页结果
    */
  protected def doPage(condition: String, parameters: List[Any], pageNumber: Long, pageSize: Int): Resp[Page[M]] = {
    JDBCProcessor.page(
      s"SELECT * FROM $tableName WHERE ${packageCondition(condition)} ",
      parameters,
      pageNumber, pageSize,
      _modelClazz
    )
  }

  /**
    * 计数前处理
    *
    * @param condition  条件，SQL (相当于Where中的条件)
    * @param parameters 参数
    * @return 是否允许计数
    */
  def preCount(condition: String, parameters: List[Any]): Resp[(String, List[Any])] = Resp.success((condition, parameters))

  /**
    * 计数后处理
    *
    * @param condition   条件，SQL (相当于Where中的条件)
    * @param parameters  参数
    * @param countResult 是否存在
    * @return 处理后的结果
    */
  def postCount(condition: String, parameters: List[Any], countResult: Long): Resp[Long] = Resp.success(countResult)

  /**
    * 计数
    *
    * @param condition  条件，SQL (相当于Where中的条件)
    * @param parameters 参数
    * @return 条数
    */
  def count(condition: String, parameters: List[Any] = List()): Resp[Long] = {
    val preR = preCount(condition, parameters)
    if (preR) {
      val filterR = filterByCond(preR.body._1, preR.body._2)
      if (filterR) {
        val doR = doCount(filterR.body._1, filterR.body._2)
        if (doR) {
          postCount(filterR.body._1, filterR.body._2, doR.body)
        } else {
          doR
        }
      } else {
        filterR
      }
    } else {
      preR
    }
  }

  /**
    * 计数实现方法
    *
    * @param condition  条件，SQL (相当于Where中的条件)
    * @param parameters 参数
    * @return 条数
    */
  protected def doCount(condition: String, parameters: List[Any]): Resp[Long] = {
    JDBCProcessor.count(
      s"SELECT 1 FROM $tableName WHERE ${packageCondition(condition)} ",
      parameters
    )
  }


  protected def packageCondition(condition: String): String = {
    if (condition == null || condition.trim == "") {
      "1=1"
    } else {
      condition
    }
  }

  protected def getMapValue(model: BaseModel): Map[String, Any] = {
    // 获取对象要持久化字段的值，忽略为null的id字段（由seq控制）
    BeanHelper.findValues(model, _entityInfo.ignoreFieldNames).toMap
      .filterNot(item => item._1 == _entityInfo.idFieldName && (item._2 == null || item._2.toString.trim == ""))
  }

  protected def getIdValue(model: BaseModel): Any = {
    if (_entityInfo.idFieldName == BaseModel.Id_FLAG) {
      model.id
    } else {
      getValueByField(model, _entityInfo.idFieldName)
    }
  }

  def setIdValue(idValue: String, model: BaseModel): Unit = {
    setValueByField(model, _entityInfo.idFieldName, idValue)
  }

  protected def getUUIDValue(model: BaseModel): String = {
    getValueByField(model, _entityInfo.uuidFieldName).asInstanceOf[String]
  }

  def setUUIDValue(uuidValue: String, model: BaseModel): Unit = {
    setValueByField(model, _entityInfo.uuidFieldName, uuidValue)
  }

  protected def getValueByField(model: AnyRef, fieldName: String): Any = {
    BeanHelper.getValue(model, fieldName).orNull
  }

  protected def setValueByField(model: AnyRef, fieldName: String, value: Any): Unit = {
    BeanHelper.setValue(model, fieldName, value)
  }

}