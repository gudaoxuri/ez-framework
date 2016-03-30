package com.ecfront.ez.framework.service.storage.foundation

import java.lang.reflect.ParameterizedType

import com.ecfront.common.{BeanHelper, JsonHelper, Resp}
import com.typesafe.scalalogging.slf4j.LazyLogging

import scala.beans.BeanProperty
import scala.reflect.runtime._

/**
  * 实体基类，所有实体都应继承此类
  */
trait BaseModel extends Serializable {

  @Id("seq")
  @BeanProperty var id: String = _

}

object BaseModel {

  val Id_FLAG = "id"
  val SPLIT = "@"

}

/**
  * 基础持久化类，所有实体持久化都应继承此类
  *
  * @tparam M 实体类型
  */
trait BaseStorage[M <: BaseModel] extends LazyLogging {

  protected val _runtimeMirror = universe.runtimeMirror(getClass.getClassLoader)

  protected val _modelClazz = this.getClass.getGenericInterfaces()(0)
    .asInstanceOf[ParameterizedType].getActualTypeArguments()(0).asInstanceOf[Class[M]]

  // 表名
  var tableName = _modelClazz.getSimpleName.toLowerCase

  def customTableName(newName: String): Unit

  /**
    * 持久化前检查
    *
    * @param model      实体对象
    * @param entityInfo 实体信息
    * @param isUpdate   是否是更新操作
    * @tparam E 实体类型
    * @return 是否通过
    */
  protected def storageCheck[E <: BaseEntityInfo](model: M, entityInfo: E, isUpdate: Boolean): Resp[Void] = {
    if (entityInfo.requireFieldNames.nonEmpty) {
      if (!isUpdate) {
        // 必填项检查
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
    } else {
      Resp.success(null)
    }
  }

  def convertToEntity(obj: Any): M = {
    JsonHelper.toObject(obj, _modelClazz)
  }

  /**
    * 保存前处理
    *
    * @param model   实体对象
    * @param context 上下文
    * @return 是否允许保存
    */
  def preSave(model: M, context: EZStorageContext): Resp[M] = Resp.success(model)

  /**
    * 保存后处理
    *
    * @param saveResult 保存后的实体对象
    * @param preResult  保存前的实体对象
    * @param context    上下文
    * @return 处理后的实体对象
    */
  def postSave(saveResult: M, preResult: M, context: EZStorageContext): Resp[M] = Resp.success(saveResult)

  /**
    * 保存
    *
    * @param model   实体对象
    * @param context 上下文
    * @return 保存后的实体对象
    */
  def save(model: M, context: EZStorageContext = EZStorageContext()): Resp[M] = {
    val preR = preSave(model, context)
    if (preR) {
      val doR = doSave(preR.body, context)
      if (doR) {
        postSave(doR.body, preR.body, context)
      } else {
        doR
      }
    } else {
      preR
    }
  }

  /**
    * 保存实现方法
    *
    * @param model   实体对象
    * @param context 上下文
    * @return 保存后的实体对象
    */
  def doSave(model: M, context: EZStorageContext): Resp[M]

  /**
    * 更新前处理
    *
    * @param model   实体对象
    * @param context 上下文
    * @return 是否允许更新
    */
  def preUpdate(model: M, context: EZStorageContext): Resp[M] = Resp.success(model)

  /**
    * 更新后处理
    *
    * @param updateResult 更新后的实体对象
    * @param preResult    更新前的实体对象
    * @param context      上下文
    * @return 处理后的实体对象
    */
  def postUpdate(updateResult: M, preResult: M, context: EZStorageContext): Resp[M] = Resp.success(updateResult)

  /**
    * 更新
    *
    * @param model   实体对象
    * @param context 上下文
    * @return 更新后的实体对象
    */
  def update(model: M, context: EZStorageContext = EZStorageContext()): Resp[M] = {
    model.id = model.id.trim
    val preR = preUpdate(model, context)
    if (preR) {
      val doR = doUpdate(preR.body, context)
      if (doR) {
        postUpdate(doR.body, preR.body, context)
      } else {
        doR
      }
    } else {
      preR
    }
  }

  /**
    * 更新实现方法
    *
    * @param model   实体对象
    * @param context 上下文
    * @return 更新后的实体对象
    */
  def doUpdate(model: M, context: EZStorageContext): Resp[M]

  /**
    * 保存或更新前处理
    *
    * @param model   实体对象
    * @param context 上下文
    * @return 是否允许保存或更新
    */
  def preSaveOrUpdate(model: M, context: EZStorageContext): Resp[M] = Resp.success(model)

  /**
    * 保存或更新后处理
    *
    * @param saveOrUpdateResult 保存或更新后的实体对象
    * @param preResult          保存或更新前的实体对象
    * @param context            上下文
    * @return 处理后的实体对象
    */
  def postSaveOrUpdate(saveOrUpdateResult: M, preResult: M, context: EZStorageContext): Resp[M] = Resp.success(saveOrUpdateResult)

  /**
    * 保存或更新
    *
    * @param model   实体对象
    * @param context 上下文
    * @return 保存或更新后的实体对象
    */
  def saveOrUpdate(model: M, context: EZStorageContext = EZStorageContext()): Resp[M] = {
    if (model.id != null) {
      model.id = model.id.trim
    }
    val preR = preSaveOrUpdate(model, context)
    if (preR) {
      val doR = doSaveOrUpdate(preR.body, context)
      if (doR) {
        postSaveOrUpdate(doR.body, preR.body, context)
      } else {
        doR
      }
    } else {
      preR
    }
  }

  /**
    * 保存或更新实现方法
    *
    * @param model   实体对象
    * @param context 上下文
    * @return 保存或更新后的实体对象
    */
  def doSaveOrUpdate(model: M, context: EZStorageContext): Resp[M]

  /**
    * 更新前处理
    *
    * @param newValues  新值，SQL (相当于SET中的条件)或Json
    * @param condition  条件，SQL (相当于Where中的条件)或Json
    * @param parameters 参数 ，Mongo不需要
    * @param context    上下文
    * @return 是否允许更新
    */
  def preUpdateByCond(
                       newValues: String, condition: String,
                       parameters: List[Any],
                       context: EZStorageContext): Resp[(String, String, List[Any])] =
    Resp.success((newValues, condition, parameters))

  /**
    * 更新后处理
    *
    * @param newValues  新值，SQL (相当于SET中的条件)或Json
    * @param condition  条件，SQL (相当于Where中的条件)或Json
    * @param parameters 参数 ，Mongo不需要
    * @param context    上下文
    * @return 是否成功
    */
  def postUpdateByCond(newValues: String, condition: String, parameters: List[Any], context: EZStorageContext): Resp[Void] = Resp.success(null)

  /**
    * 更新
    *
    * @param newValues  新值，SQL (相当于SET中的条件)或Json
    * @param condition  条件，SQL (相当于Where中的条件)或Json
    * @param parameters 参数 ，Mongo不需要
    * @param context    上下文
    * @return 是否成功
    */
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

  /**
    * 更新实现方法
    *
    * @param newValues  新值，SQL (相当于SET中的条件)或Json
    * @param condition  条件，SQL (相当于Where中的条件)或Json
    * @param parameters 参数 ，Mongo不需要
    * @param context    上下文
    * @return 是否成功
    */
  def doUpdateByCond(newValues: String, condition: String, parameters: List[Any], context: EZStorageContext): Resp[Void]

  /**
    * 删除前处理
    *
    * @param id      主键
    * @param context 上下文
    * @return 是否允许删除
    */
  def preDeleteById(id: Any, context: EZStorageContext): Resp[Any] = Resp.success(id)

  /**
    * 删除后处理
    *
    * @param id      主键
    * @param context 上下文
    * @return 是否成功
    */
  def postDeleteById(id: Any, context: EZStorageContext): Resp[Void] = Resp.success(null)

  /**
    * 删除
    *
    * @param id      主键
    * @param context 上下文
    * @return 是否成功
    */
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

  /**
    * 删除实现方法
    *
    * @param id      主键
    * @param context 上下文
    * @return 是否成功
    */
  def doDeleteById(id: Any, context: EZStorageContext): Resp[Void]

  /**
    * 删除前处理
    *
    * @param condition  条件，SQL (相当于Where中的条件)或Json
    * @param parameters 参数 ，Mongo不需要
    * @param context    上下文
    * @return 是否允许删除
    */
  def preDeleteByCond(
                       condition: String, parameters: List[Any],
                       context: EZStorageContext): Resp[(String, List[Any])] =
    Resp.success((condition, parameters))

  /**
    * 删除后处理
    *
    * @param condition  条件，SQL (相当于Where中的条件)或Json
    * @param parameters 参数 ，Mongo不需要
    * @param context    上下文
    * @return 是否成功
    */
  def postDeleteByCond(condition: String, parameters: List[Any], context: EZStorageContext): Resp[Void] = Resp.success(null)

  /**
    * 删除
    *
    * @param condition  条件，SQL (相当于Where中的条件)或Json
    * @param parameters 参数 ，Mongo不需要
    * @param context    上下文
    * @return 是否成功
    */
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

  /**
    * 删除实现方法
    *
    * @param condition  条件，SQL (相当于Where中的条件)或Json
    * @param parameters 参数 ，Mongo不需要
    * @param context    上下文
    * @return 是否成功
    */
  def doDeleteByCond(condition: String, parameters: List[Any], context: EZStorageContext): Resp[Void]

  /**
    * 获取一条记录前处理
    *
    * @param id      主键
    * @param context 上下文
    * @return 是否允许获取这条记录
    */
  def preGetById(id: Any, context: EZStorageContext): Resp[Any] = Resp.success(id)

  /**
    * 获取一条记录后处理
    *
    * @param id        主键
    * @param getResult 获取到的记录
    * @param context   上下文
    * @return 处理后的记录
    */
  def postGetById(id: Any, getResult: M, context: EZStorageContext): Resp[M] = Resp.success(getResult)

  /**
    * 获取一条记录
    *
    * @param id      主键
    * @param context 上下文
    * @return 获取到的记录
    */
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

  /**
    * 获取一条记录实现方法
    *
    * @param id      主键
    * @param context 上下文
    * @return 获取到的记录
    */
  def doGetById(id: Any, context: EZStorageContext): Resp[M]

  /**
    * 获取一条记录前处理
    *
    * @param condition  条件，SQL (相当于Where中的条件)或Json
    * @param parameters 参数 ，Mongo不需要
    * @param context    上下文
    * @return 是否允许获取这条记录
    */
  def preGetByCond(condition: String,
                   parameters: List[Any], context: EZStorageContext): Resp[(String, List[Any])] =
    Resp.success((condition, parameters))

  /**
    * 获取一条记录后处理
    *
    * @param condition  条件，SQL (相当于Where中的条件)或Json
    * @param parameters 参数 ，Mongo不需要
    * @param getResult  获取到的记录
    * @param context    上下文
    * @return 处理后的记录
    */
  def postGetByCond(condition: String, parameters: List[Any], getResult: M, context: EZStorageContext): Resp[M] = Resp.success(getResult)

  /**
    * 获取一条记录
    *
    * @param condition  条件，SQL (相当于Where中的条件)或Json
    * @param parameters 参数 ，Mongo不需要
    * @param context    上下文
    * @return 获取到的记录
    */
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

  /**
    * 获取一条记录实现方法
    *
    * @param condition  条件，SQL (相当于Where中的条件)或Json
    * @param parameters 参数 ，Mongo不需要
    * @param context    上下文
    * @return 获取到的记录
    */
  def doGetByCond(condition: String, parameters: List[Any], context: EZStorageContext): Resp[M]

  /**
    * 判断是否存在前处理
    *
    * @param id      主键
    * @param context 上下文
    * @return 是否允许判断是否存在
    */
  def preExistById(id: Any, context: EZStorageContext): Resp[Any] = Resp.success(id)

  /**
    * 判断是否存在后处理
    *
    * @param id          主键
    * @param existResult 是否存在
    * @param context     上下文
    * @return 处理后的结果
    */
  def postExistById(id: Any, existResult: Boolean, context: EZStorageContext): Resp[Boolean] = Resp.success(existResult)

  /**
    * 判断是否存在
    *
    * @param id      主键
    * @param context 上下文
    * @return 是否存在
    */
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

  /**
    * 判断是否存在实现方法
    *
    * @param id      主键
    * @param context 上下文
    * @return 是否存在
    */
  def doExistById(id: Any, context: EZStorageContext = null): Resp[Boolean]

  /**
    * 判断是否存在前处理
    *
    * @param condition  条件，SQL (相当于Where中的条件)或Json
    * @param parameters 参数 ，Mongo不需要
    * @param context    上下文
    * @return 是否允许判断是否存在
    */
  def preExistByCond(condition: String, parameters: List[Any],
                     context: EZStorageContext): Resp[(String, List[Any])] =
    Resp.success((condition, parameters))

  /**
    * 判断是否存在后处理
    *
    * @param condition   条件，SQL (相当于Where中的条件)或Json
    * @param parameters  参数 ，Mongo不需要
    * @param existResult 是否存在
    * @param context     上下文
    * @return 处理后的结果
    */
  def postExistByCond(condition: String, parameters: List[Any],
                      existResult: Boolean, context: EZStorageContext): Resp[Boolean] =
    Resp.success(existResult)

  /**
    * 判断是否存在
    *
    * @param condition  条件，SQL (相当于Where中的条件)或Json
    * @param parameters 参数 ，Mongo不需要
    * @param context    上下文
    * @return 是否存在
    */
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

  /**
    * 判断是否存在实现方法
    *
    * @param condition  条件，SQL (相当于Where中的条件)或Json
    * @param parameters 参数 ，Mongo不需要
    * @param context    上下文
    * @return 是否存在
    */
  def doExistByCond(condition: String, parameters: List[Any], context: EZStorageContext): Resp[Boolean]

  /**
    * 查找前处理
    *
    * @param condition  条件，SQL (相当于Where中的条件)或Json
    * @param parameters 参数 ，Mongo不需要
    * @param context    上下文
    * @return 是否允许查找
    */
  def preFind(condition: String, parameters: List[Any], context: EZStorageContext): Resp[(String, List[Any])] = Resp.success((condition, parameters))

  /**
    * 查找后处理
    *
    * @param condition  条件，SQL (相当于Where中的条件)或Json
    * @param parameters 参数 ，Mongo不需要
    * @param findResult 是否存在
    * @param context    上下文
    * @return 处理后的结果
    */
  def postFind(condition: String, parameters: List[Any], findResult: List[M], context: EZStorageContext): Resp[List[M]] = Resp.success(findResult)

  /**
    * 查找
    *
    * @param condition  条件，SQL (相当于Where中的条件)或Json
    * @param parameters 参数 ，Mongo不需要
    * @param context    上下文
    * @return 查找结果
    */
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

  /**
    * 查找实现方法
    *
    * @param condition  条件，SQL (相当于Where中的条件)或Json
    * @param parameters 参数 ，Mongo不需要
    * @param context    上下文
    * @return 查找结果
    */
  def doFind(condition: String, parameters: List[Any], context: EZStorageContext): Resp[List[M]]

  /**
    * 分页前处理
    *
    * @param condition  条件，SQL (相当于Where中的条件)或Json
    * @param parameters 参数 ，Mongo不需要
    * @param pageNumber 当前页，从1开始
    * @param pageSize   每页条数
    * @param context    上下文
    * @return 是否允许分页
    */
  def prePage(condition: String, parameters: List[Any], pageNumber: Long, pageSize: Int,
              context: EZStorageContext): Resp[(String, List[Any])] = Resp.success((condition, parameters))

  /**
    * 分页后处理
    *
    * @param condition  条件，SQL (相当于Where中的条件)或Json
    * @param parameters 参数 ，Mongo不需要
    * @param pageNumber 当前页，从1开始
    * @param pageSize   每页条数
    * @param pageResult 是否存在
    * @param context    上下文
    * @return 处理后的结果
    */
  def postPage(condition: String, parameters: List[Any], pageNumber: Long, pageSize: Int,
               pageResult: Page[M], context: EZStorageContext): Resp[Page[M]] = Resp.success(pageResult)

  /**
    * 分页
    *
    * @param condition  条件，SQL (相当于Where中的条件)或Json
    * @param parameters 参数 ，Mongo不需要
    * @param pageNumber 当前页，从1开始
    * @param pageSize   每页条数
    * @param context    上下文
    * @return 分页结果
    */
  def page(condition: String, parameters: List[Any] = List(), pageNumber: Long = 1, pageSize: Int = 10,
           context: EZStorageContext = EZStorageContext()): Resp[Page[M]] = {
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

  /**
    * 分页实现方法
    *
    * @param condition  条件，SQL (相当于Where中的条件)或Json
    * @param parameters 参数 ，Mongo不需要
    * @param pageNumber 当前页，从1开始
    * @param pageSize   每页条数
    * @param context    上下文
    * @return 分页结果
    */
  def doPage(condition: String, parameters: List[Any], pageNumber: Long, pageSize: Int, context: EZStorageContext): Resp[Page[M]]

  /**
    * 计数前处理
    *
    * @param condition  条件，SQL (相当于Where中的条件)或Json
    * @param parameters 参数 ，Mongo不需要
    * @param context    上下文
    * @return 是否允许计数
    */
  def preCount(condition: String, parameters: List[Any], context: EZStorageContext): Resp[(String, List[Any])] = Resp.success((condition, parameters))

  /**
    * 计数后处理
    *
    * @param condition   条件，SQL (相当于Where中的条件)或Json
    * @param parameters  参数 ，Mongo不需要
    * @param countResult 是否存在
    * @param context     上下文
    * @return 处理后的结果
    */
  def postCount(condition: String, parameters: List[Any], countResult: Long, context: EZStorageContext): Resp[Long] = Resp.success(countResult)

  /**
    * 计数
    *
    * @param condition  条件，SQL (相当于Where中的条件)或Json
    * @param parameters 参数 ，Mongo不需要
    * @param context    上下文
    * @return 条数
    */
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

  /**
    * 计数实现方法
    *
    * @param condition  条件，SQL (相当于Where中的条件)或Json
    * @param parameters 参数 ，Mongo不需要
    * @param context    上下文
    * @return 条数
    */
  def doCount(condition: String, parameters: List[Any], context: EZStorageContext): Resp[Long]

}


/**
  * 基础持久化类的实现适配类
  *
  * @tparam M 实体类型
  * @tparam O 实现类，如jdbc或mongo方式
  */
trait BaseStorageAdapter[M <: BaseModel, O <: BaseStorage[M]] extends BaseStorage[M] {

  // 持久化对象
  protected val storageObj: O

  override def customTableName(newName: String): Unit = {
    storageObj.customTableName(newName)
  }

  /**
    * 保存前处理
    *
    * @param model   实体对象
    * @param context 上下文
    * @return 是否允许保存
    */
  override def preSave(model: M, context: EZStorageContext): Resp[M] = storageObj.preSave(model, context)

  /**
    * 保存后处理
    *
    * @param saveResult 保存后的实体对象
    * @param preResult  保存前的实体对象
    * @param context    上下文
    * @return 处理后的实体对象
    */
  override def postSave(saveResult: M, preResult: M, context: EZStorageContext): Resp[M] = storageObj.postSave(saveResult, preResult, context)

  /**
    * 保存
    *
    * @param model   实体对象
    * @param context 上下文
    * @return 保存后的实体对象
    */
  override def save(model: M, context: EZStorageContext): Resp[M] = storageObj.save(model, context)

  /**
    * 更新前处理
    *
    * @param model   实体对象
    * @param context 上下文
    * @return 是否允许更新
    */
  override def preUpdate(model: M, context: EZStorageContext): Resp[M] = storageObj.preUpdate(model, context)

  /**
    * 更新后处理
    *
    * @param updateResult 更新后的实体对象
    * @param preResult    更新前的实体对象
    * @param context      上下文
    * @return 处理后的实体对象
    */
  override def postUpdate(updateResult: M, preResult: M, context: EZStorageContext): Resp[M] = storageObj.postUpdate(updateResult, preResult, context)

  /**
    * 更新
    *
    * @param model   实体对象
    * @param context 上下文
    * @return 更新后的实体对象
    */
  override def update(model: M, context: EZStorageContext): Resp[M] = storageObj.update(model, context)

  /**
    * 保存或更新前处理
    *
    * @param model   实体对象
    * @param context 上下文
    * @return 是否允许保存或更新
    */
  override def preSaveOrUpdate(model: M, context: EZStorageContext): Resp[M] = storageObj.preSaveOrUpdate(model, context)

  /**
    * 保存或更新后处理
    *
    * @param saveOrUpdateResult 保存或更新后的实体对象
    * @param preResult          保存或更新前的实体对象
    * @param context            上下文
    * @return 处理后的实体对象
    */
  override def postSaveOrUpdate(saveOrUpdateResult: M, preResult: M, context: EZStorageContext): Resp[M] =
    storageObj.postSaveOrUpdate(saveOrUpdateResult, preResult, context)

  /**
    * 保存或更新
    *
    * @param model   实体对象
    * @param context 上下文
    * @return 保存或更新后的实体对象
    */
  override def saveOrUpdate(model: M, context: EZStorageContext): Resp[M] = storageObj.saveOrUpdate(model, context)

  /**
    * 更新前处理
    *
    * @param newValues  新值，SQL (相当于SET中的条件)或Json
    * @param condition  条件，SQL (相当于Where中的条件)或Json
    * @param parameters 参数 ，Mongo不需要
    * @param context    上下文
    * @return 是否允许更新
    */
  override def preUpdateByCond(
                                newValues: String, condition: String,
                                parameters: List[Any], context: EZStorageContext): Resp[(String, String, List[Any])] =
    storageObj.preUpdateByCond(newValues, condition, parameters, context)

  /**
    * 更新后处理
    *
    * @param newValues  新值，SQL (相当于SET中的条件)或Json
    * @param condition  条件，SQL (相当于Where中的条件)或Json
    * @param parameters 参数 ，Mongo不需要
    * @param context    上下文
    * @return 是否成功
    */
  override def postUpdateByCond(newValues: String, condition:
  String, parameters: List[Any], context: EZStorageContext): Resp[Void] =
    storageObj.postUpdateByCond(newValues, condition, parameters, context)

  /**
    * 更新
    *
    * @param newValues  新值，SQL (相当于SET中的条件)或Json
    * @param condition  条件，SQL (相当于Where中的条件)或Json
    * @param parameters 参数 ，Mongo不需要
    * @param context    上下文
    * @return 是否成功
    */
  override def updateByCond(newValues: String, condition: String,
                            parameters: List[Any], context: EZStorageContext): Resp[Void] =
    storageObj.updateByCond(newValues, condition, parameters, context)

  /**
    * 删除前处理
    *
    * @param id      主键
    * @param context 上下文
    * @return 是否允许删除
    */
  override def preDeleteById(id: Any, context: EZStorageContext): Resp[Any] = storageObj.preDeleteById(id, context)

  /**
    * 删除后处理
    *
    * @param id      主键
    * @param context 上下文
    * @return 是否成功
    */
  override def postDeleteById(id: Any, context: EZStorageContext): Resp[Void] = storageObj.postDeleteById(id, context)

  /**
    * 删除
    *
    * @param id      主键
    * @param context 上下文
    * @return 是否成功
    */
  override def deleteById(id: Any, context: EZStorageContext): Resp[Void] = storageObj.deleteById(id, context)

  /**
    * 删除前处理
    *
    * @param condition  条件，SQL (相当于Where中的条件)或Json
    * @param parameters 参数 ，Mongo不需要
    * @param context    上下文
    * @return 是否允许删除
    */
  override def preDeleteByCond(condition: String, parameters: List[Any],
                               context: EZStorageContext): Resp[(String, List[Any])] =
    storageObj.preDeleteByCond(condition, parameters, context)

  /**
    * 删除后处理
    *
    * @param condition  条件，SQL (相当于Where中的条件)或Json
    * @param parameters 参数 ，Mongo不需要
    * @param context    上下文
    * @return 是否成功
    */
  override def postDeleteByCond(condition: String, parameters: List[Any],
                                context: EZStorageContext): Resp[Void] =
    storageObj.postDeleteByCond(condition, parameters, context)

  /**
    * 删除
    *
    * @param condition  条件，SQL (相当于Where中的条件)或Json
    * @param parameters 参数 ，Mongo不需要
    * @param context    上下文
    * @return 是否成功
    */
  override def deleteByCond(condition: String, parameters: List[Any],
                            context: EZStorageContext): Resp[Void] =
    storageObj.deleteByCond(condition, parameters, context)

  /**
    * 获取一条记录前处理
    *
    * @param id      主键
    * @param context 上下文
    * @return 是否允许获取这条记录
    */
  override def preGetById(id: Any, context: EZStorageContext): Resp[Any] = storageObj.preGetById(id, context)

  /**
    * 获取一条记录后处理
    *
    * @param id        主键
    * @param getResult 获取到的记录
    * @param context   上下文
    * @return 处理后的记录
    */
  override def postGetById(id: Any, getResult: M, context: EZStorageContext): Resp[M] = storageObj.postGetById(id, getResult, context)

  /**
    * 获取一条记录
    *
    * @param id      主键
    * @param context 上下文
    * @return 获取到的记录
    */
  override def getById(id: Any, context: EZStorageContext): Resp[M] = storageObj.getById(id, context)

  /**
    * 获取一条记录前处理
    *
    * @param condition  条件，SQL (相当于Where中的条件)或Json
    * @param parameters 参数 ，Mongo不需要
    * @param context    上下文
    * @return 是否允许获取这条记录
    */
  override def preGetByCond(condition: String, parameters: List[Any],
                            context: EZStorageContext): Resp[(String, List[Any])] =
    storageObj.preGetByCond(condition, parameters, context)

  /**
    * 获取一条记录后处理
    *
    * @param condition  条件，SQL (相当于Where中的条件)或Json
    * @param parameters 参数 ，Mongo不需要
    * @param getResult  获取到的记录
    * @param context    上下文
    * @return 处理后的记录
    */
  override def postGetByCond(condition: String, parameters: List[Any],
                             getResult: M, context: EZStorageContext): Resp[M] =
    storageObj.postGetByCond(condition, parameters, getResult, context)

  /**
    * 获取一条记录
    *
    * @param condition  条件，SQL (相当于Where中的条件)或Json
    * @param parameters 参数 ，Mongo不需要
    * @param context    上下文
    * @return 获取到的记录
    */
  override def getByCond(condition: String, parameters: List[Any],
                         context: EZStorageContext): Resp[M] = storageObj.getByCond(condition, parameters, context)

  /**
    * 判断是否存在前处理
    *
    * @param id      主键
    * @param context 上下文
    * @return 是否允许判断是否存在
    */
  override def preExistById(id: Any, context: EZStorageContext): Resp[Any] = storageObj.preExistById(id, context)

  /**
    * 判断是否存在后处理
    *
    * @param id          主键
    * @param existResult 是否存在
    * @param context     上下文
    * @return 处理后的结果
    */
  override def postExistById(id: Any, existResult: Boolean,
                             context: EZStorageContext): Resp[Boolean] =
    storageObj.postExistById(id, existResult, context)

  /**
    * 判断是否存在
    *
    * @param id      主键
    * @param context 上下文
    * @return 是否存在
    */
  override def existById(id: Any, context: EZStorageContext): Resp[Boolean] = storageObj.existById(id, context)

  /**
    * 判断是否存在前处理
    *
    * @param condition  条件，SQL (相当于Where中的条件)或Json
    * @param parameters 参数 ，Mongo不需要
    * @param context    上下文
    * @return 是否允许判断是否存在
    */
  override def preExistByCond(condition: String, parameters: List[Any],
                              context: EZStorageContext): Resp[(String, List[Any])] =
    storageObj.preExistByCond(condition, parameters, context)

  /**
    * 判断是否存在后处理
    *
    * @param condition   条件，SQL (相当于Where中的条件)或Json
    * @param parameters  参数 ，Mongo不需要
    * @param existResult 是否存在
    * @param context     上下文
    * @return 处理后的结果
    */
  override def postExistByCond(condition: String, parameters: List[Any],
                               existResult: Boolean, context: EZStorageContext): Resp[Boolean] =
    storageObj.postExistByCond(condition, parameters, existResult, context)

  /**
    * 判断是否存在
    *
    * @param condition  条件，SQL (相当于Where中的条件)或Json
    * @param parameters 参数 ，Mongo不需要
    * @param context    上下文
    * @return 是否存在
    */
  override def existByCond(condition: String, parameters: List[Any],
                           context: EZStorageContext): Resp[Boolean] =
    storageObj.existByCond(condition, parameters, context)

  /**
    * 查找前处理
    *
    * @param condition  条件，SQL (相当于Where中的条件)或Json
    * @param parameters 参数 ，Mongo不需要
    * @param context    上下文
    * @return 是否允许查找
    */
  override def preFind(condition: String, parameters: List[Any],
                       context: EZStorageContext): Resp[(String, List[Any])] =
    storageObj.preFind(condition, parameters, context)

  /**
    * 查找后处理
    *
    * @param condition  条件，SQL (相当于Where中的条件)或Json
    * @param parameters 参数 ，Mongo不需要
    * @param findResult 是否存在
    * @param context    上下文
    * @return 处理后的结果
    */
  override def postFind(condition: String, parameters: List[Any],
                        findResult: List[M], context: EZStorageContext): Resp[List[M]] =
    storageObj.postFind(condition, parameters, findResult, context)

  /**
    * 查找
    *
    * @param condition  条件，SQL (相当于Where中的条件)或Json
    * @param parameters 参数 ，Mongo不需要
    * @param context    上下文
    * @return 查找结果
    */
  override def find(condition: String, parameters: List[Any],
                    context: EZStorageContext): Resp[List[M]] =
    storageObj.find(condition, parameters, context)

  /**
    * 分页前处理
    *
    * @param condition  条件，SQL (相当于Where中的条件)或Json
    * @param parameters 参数 ，Mongo不需要
    * @param pageNumber 当前页，从1开始
    * @param pageSize   每页条数
    * @param context    上下文
    * @return 是否允许分页
    */
  override def prePage(condition: String, parameters: List[Any],
                       pageNumber: Long, pageSize: Int, context: EZStorageContext): Resp[(String, List[Any])] =
    storageObj.prePage(condition, parameters, pageNumber, pageSize, context)

  /**
    * 分页后处理
    *
    * @param condition  条件，SQL (相当于Where中的条件)或Json
    * @param parameters 参数 ，Mongo不需要
    * @param pageNumber 当前页，从1开始
    * @param pageSize   每页条数
    * @param pageResult 是否存在
    * @param context    上下文
    * @return 处理后的结果
    */
  override def postPage(condition: String, parameters: List[Any],
                        pageNumber: Long, pageSize: Int, pageResult: Page[M], context: EZStorageContext): Resp[Page[M]] =
    storageObj.postPage(condition, parameters, pageNumber, pageSize, pageResult, context)

  /**
    * 分页
    *
    * @param condition  条件，SQL (相当于Where中的条件)或Json
    * @param parameters 参数 ，Mongo不需要
    * @param pageNumber 当前页，从1开始
    * @param pageSize   每页条数
    * @param context    上下文
    * @return 分页结果
    */
  override def page(condition: String, parameters: List[Any],
                    pageNumber: Long, pageSize: Int, context: EZStorageContext): Resp[Page[M]] =
    storageObj.page(condition, parameters, pageNumber, pageSize, context)

  /**
    * 计数前处理
    *
    * @param condition  条件，SQL (相当于Where中的条件)或Json
    * @param parameters 参数 ，Mongo不需要
    * @param context    上下文
    * @return 是否允许计数
    */
  override def preCount(condition: String, parameters: List[Any],
                        context: EZStorageContext): Resp[(String, List[Any])] =
    storageObj.preCount(condition, parameters, context)

  /**
    * 计数后处理
    *
    * @param condition   条件，SQL (相当于Where中的条件)或Json
    * @param parameters  参数 ，Mongo不需要
    * @param countResult 是否存在
    * @param context     上下文
    * @return 处理后的结果
    */
  override def postCount(condition: String, parameters: List[Any],
                         countResult: Long, context: EZStorageContext): Resp[Long] =
    storageObj.postCount(condition, parameters, countResult, context)

  /**
    * 计数
    *
    * @param condition  条件，SQL (相当于Where中的条件)或Json
    * @param parameters 参数 ，Mongo不需要
    * @param context    上下文
    * @return 条数
    */
  override def count(condition: String, parameters: List[Any],
                     context: EZStorageContext): Resp[Long] = storageObj.count(condition, parameters, context)

  /**
    * 保存实现方法
    *
    * @param model   实体对象
    * @param context 上下文
    * @return 保存后的实体对象
    */
  override def doSave(model: M, context: EZStorageContext): Resp[M] =
    storageObj.doSave(model, context)

  /**
    * 更新实现方法
    *
    * @param newValues  新值，SQL (相当于SET中的条件)或Json
    * @param condition  条件，SQL (相当于Where中的条件)或Json
    * @param parameters 参数 ，Mongo不需要
    * @param context    上下文
    * @return 是否成功
    */
  override def doUpdateByCond(newValues: String, condition: String,
                              parameters: List[Any], context: EZStorageContext): Resp[Void] =
    storageObj.doUpdateByCond(newValues, condition, parameters, context)

  /**
    * 删除实现方法
    *
    * @param condition  条件，SQL (相当于Where中的条件)或Json
    * @param parameters 参数 ，Mongo不需要
    * @param context    上下文
    * @return 是否成功
    */
  override def doDeleteByCond(condition: String, parameters: List[Any],
                              context: EZStorageContext): Resp[Void] =
    storageObj.doDeleteByCond(condition, parameters, context)

  /**
    * 计数实现方法
    *
    * @param condition  条件，SQL (相当于Where中的条件)或Json
    * @param parameters 参数 ，Mongo不需要
    * @param context    上下文
    * @return 条数
    */
  override def doCount(condition: String, parameters: List[Any],
                       context: EZStorageContext): Resp[Long] =
    storageObj.doCount(condition, parameters, context)

  /**
    * 判断是否存在实现方法
    *
    * @param id      主键
    * @param context 上下文
    * @return 是否存在
    */
  override def doExistById(id: Any, context: EZStorageContext): Resp[Boolean] =
    storageObj.doExistById(id, context)

  /**
    * 更新实现方法
    *
    * @param model   实体对象
    * @param context 上下文
    * @return 更新后的实体对象
    */
  override def doUpdate(model: M, context: EZStorageContext): Resp[M] =
    storageObj.doUpdate(model, context)

  /**
    * 保存或更新实现方法
    *
    * @param model   实体对象
    * @param context 上下文
    * @return 保存或更新后的实体对象
    */
  override def doSaveOrUpdate(model: M, context: EZStorageContext): Resp[M] =
    storageObj.doSaveOrUpdate(model, context)

  /**
    * 删除实现方法
    *
    * @param id      主键
    * @param context 上下文
    * @return 是否成功
    */
  override def doDeleteById(id: Any, context: EZStorageContext): Resp[Void] =
    storageObj.doDeleteById(id, context)

  /**
    * 获取一条记录实现方法
    *
    * @param condition  条件，SQL (相当于Where中的条件)或Json
    * @param parameters 参数 ，Mongo不需要
    * @param context    上下文
    * @return 获取到的记录
    */
  override def doGetByCond(condition: String, parameters: List[Any],
                           context: EZStorageContext): Resp[M] =
    storageObj.doGetByCond(condition, parameters, context)

  /**
    * 分页实现方法
    *
    * @param condition  条件，SQL (相当于Where中的条件)或Json
    * @param parameters 参数 ，Mongo不需要
    * @param pageNumber 当前页，从1开始
    * @param pageSize   每页条数
    * @param context    上下文
    * @return 分页结果
    */
  override def doPage(condition: String, parameters: List[Any],
                      pageNumber: Long, pageSize: Int, context: EZStorageContext): Resp[Page[M]] =
    storageObj.doPage(condition, parameters, pageNumber, pageSize, context)

  /**
    * 查找实现方法
    *
    * @param condition  条件，SQL (相当于Where中的条件)或Json
    * @param parameters 参数 ，Mongo不需要
    * @param context    上下文
    * @return 查找结果
    */
  override def doFind(condition: String, parameters: List[Any],
                      context: EZStorageContext): Resp[List[M]] =
    storageObj.doFind(condition, parameters, context)

  /**
    * 判断是否存在实现方法
    *
    * @param condition  条件，SQL (相当于Where中的条件)或Json
    * @param parameters 参数 ，Mongo不需要
    * @param context    上下文
    * @return 是否存在
    */
  override def doExistByCond(condition: String, parameters: List[Any],
                             context: EZStorageContext): Resp[Boolean] =
    storageObj.doExistByCond(condition, parameters, context)

  /**
    * 获取一条记录实现方法
    *
    * @param id      主键
    * @param context 上下文
    * @return 获取到的记录
    */
  override def doGetById(id: Any, context: EZStorageContext): Resp[M] = storageObj.doGetById(id, context)
}



