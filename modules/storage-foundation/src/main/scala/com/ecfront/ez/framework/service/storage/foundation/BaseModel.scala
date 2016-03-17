package com.ecfront.ez.framework.service.storage.foundation

import java.lang.reflect.ParameterizedType

import com.ecfront.common.{BeanHelper, Resp}
import com.typesafe.scalalogging.slf4j.LazyLogging

import scala.beans.BeanProperty

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

  protected val _modelClazz = this.getClass.getGenericInterfaces()(0)
    .asInstanceOf[ParameterizedType].getActualTypeArguments()(0).asInstanceOf[Class[M]]

  // 表名
  val tableName = _modelClazz.getSimpleName.toLowerCase

  /**
    * 持久化前检查
    *
    * @param model      实体对象
    * @param entityInfo 实体信息
    * @tparam E 实体类型
    * @return 是否通过
    */
  protected def storageCheck[E <: BaseEntityInfo](model: M, entityInfo: E): Resp[Void] = {
    if (entityInfo.requireFieldNames.nonEmpty) {
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
  }

  /**
    * 保存前处理
    *
    * @param model   实体对象
    * @param context 上下文
    * @return 是否允许保存
    */
  protected def preSave(model: M, context: EZStorageContext): Resp[M] = Resp.success(model)

  /**
    * 保存后处理
    *
    * @param saveResult 保存后的实体对象
    * @param context    上下文
    * @return 处理后的实体对象
    */
  protected def postSave(saveResult: M, context: EZStorageContext): Resp[M] = Resp.success(saveResult)

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
        postSave(doR.body, context)
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
  protected def doSave(model: M, context: EZStorageContext): Resp[M]

  /**
    * 更新前处理
    *
    * @param model   实体对象
    * @param context 上下文
    * @return 是否允许更新
    */
  protected def preUpdate(model: M, context: EZStorageContext): Resp[M] = Resp.success(model)

  /**
    * 更新后处理
    *
    * @param updateResult 更新后的实体对象
    * @param context      上下文
    * @return 处理后的实体对象
    */
  protected def postUpdate(updateResult: M, context: EZStorageContext): Resp[M] = Resp.success(updateResult)

  /**
    * 更新
    *
    * @param model   实体对象
    * @param context 上下文
    * @return 更新后的实体对象
    */
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

  /**
    * 更新实现方法
    *
    * @param model   实体对象
    * @param context 上下文
    * @return 更新后的实体对象
    */
  protected def doUpdate(model: M, context: EZStorageContext): Resp[M]

  /**
    * 保存或更新前处理
    *
    * @param model   实体对象
    * @param context 上下文
    * @return 是否允许保存或更新
    */
  protected def preSaveOrUpdate(model: M, context: EZStorageContext): Resp[M] = Resp.success(model)

  /**
    * 保存或更新后处理
    *
    * @param saveOrUpdateResult 保存或更新后的实体对象
    * @param context            上下文
    * @return 处理后的实体对象
    */
  protected def postSaveOrUpdate(saveOrUpdateResult: M, context: EZStorageContext): Resp[M] = Resp.success(saveOrUpdateResult)

  /**
    * 保存或更新
    *
    * @param model   实体对象
    * @param context 上下文
    * @return 保存或更新后的实体对象
    */
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

  /**
    * 保存或更新实现方法
    *
    * @param model   实体对象
    * @param context 上下文
    * @return 保存或更新后的实体对象
    */
  protected def doSaveOrUpdate(model: M, context: EZStorageContext): Resp[M]

  /**
    * 更新前处理
    *
    * @param newValues  新值，SQL (相当于SET中的条件)或Json
    * @param condition  条件，SQL (相当于Where中的条件)或Json
    * @param parameters 参数 ，Mongo不需要
    * @param context    上下文
    * @return 是否允许更新
    */
  protected def preUpdateByCond(
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
  protected def postUpdateByCond(newValues: String, condition: String, parameters: List[Any], context: EZStorageContext): Resp[Void] = Resp.success(null)

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
  protected def doUpdateByCond(newValues: String, condition: String, parameters: List[Any], context: EZStorageContext): Resp[Void]

  /**
    * 删除前处理
    *
    * @param id      主键
    * @param context 上下文
    * @return 是否允许删除
    */
  protected def preDeleteById(id: Any, context: EZStorageContext): Resp[Any] = Resp.success(id)

  /**
    * 删除后处理
    *
    * @param id      主键
    * @param context 上下文
    * @return 是否成功
    */
  protected def postDeleteById(id: Any, context: EZStorageContext): Resp[Void] = Resp.success(null)

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
  protected def doDeleteById(id: Any, context: EZStorageContext): Resp[Void]

  /**
    * 删除前处理
    *
    * @param condition  条件，SQL (相当于Where中的条件)或Json
    * @param parameters 参数 ，Mongo不需要
    * @param context    上下文
    * @return 是否允许删除
    */
  protected def preDeleteByCond(
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
  protected def postDeleteByCond(condition: String, parameters: List[Any], context: EZStorageContext): Resp[Void] = Resp.success(null)

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
  protected def doDeleteByCond(condition: String, parameters: List[Any], context: EZStorageContext): Resp[Void]

  /**
    * 获取一条记录前处理
    *
    * @param id      主键
    * @param context 上下文
    * @return 是否允许获取这条记录
    */
  protected def preGetById(id: Any, context: EZStorageContext): Resp[Any] = Resp.success(id)

  /**
    * 获取一条记录后处理
    *
    * @param id        主键
    * @param getResult 获取到的记录
    * @param context   上下文
    * @return 处理后的记录
    */
  protected def postGetById(id: Any, getResult: M, context: EZStorageContext): Resp[M] = Resp.success(getResult)

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
  protected def doGetById(id: Any, context: EZStorageContext): Resp[M]

  /**
    * 获取一条记录前处理
    *
    * @param condition  条件，SQL (相当于Where中的条件)或Json
    * @param parameters 参数 ，Mongo不需要
    * @param context    上下文
    * @return 是否允许获取这条记录
    */
  protected def preGetByCond(condition: String,
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
  protected def postGetByCond(condition: String, parameters: List[Any], getResult: M, context: EZStorageContext): Resp[M] = Resp.success(getResult)

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
  protected def doGetByCond(condition: String, parameters: List[Any], context: EZStorageContext): Resp[M]

  /**
    * 判断是否存在前处理
    *
    * @param id      主键
    * @param context 上下文
    * @return 是否允许判断是否存在
    */
  protected def preExistById(id: Any, context: EZStorageContext): Resp[Any] = Resp.success(id)

  /**
    * 判断是否存在后处理
    *
    * @param id          主键
    * @param existResult 是否存在
    * @param context     上下文
    * @return 处理后的结果
    */
  protected def postExistById(id: Any, existResult: Boolean, context: EZStorageContext): Resp[Boolean] = Resp.success(existResult)

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
  protected def doExistById(id: Any, context: EZStorageContext = null): Resp[Boolean]

  /**
    * 判断是否存在前处理
    *
    * @param condition  条件，SQL (相当于Where中的条件)或Json
    * @param parameters 参数 ，Mongo不需要
    * @param context    上下文
    * @return 是否允许判断是否存在
    */
  protected def preExistByCond(condition: String, parameters: List[Any],
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
  protected def postExistByCond(condition: String, parameters: List[Any],
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
  protected def doExistByCond(condition: String, parameters: List[Any], context: EZStorageContext): Resp[Boolean]

  /**
    * 查找前处理
    *
    * @param condition  条件，SQL (相当于Where中的条件)或Json
    * @param parameters 参数 ，Mongo不需要
    * @param context    上下文
    * @return 是否允许查找
    */
  protected def preFind(condition: String, parameters: List[Any], context: EZStorageContext): Resp[(String, List[Any])] = Resp.success((condition, parameters))

  /**
    * 查找后处理
    *
    * @param condition  条件，SQL (相当于Where中的条件)或Json
    * @param parameters 参数 ，Mongo不需要
    * @param findResult 是否存在
    * @param context    上下文
    * @return 处理后的结果
    */
  protected def postFind(condition: String, parameters: List[Any], findResult: List[M], context: EZStorageContext): Resp[List[M]] = Resp.success(findResult)

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
  protected def doFind(condition: String, parameters: List[Any], context: EZStorageContext): Resp[List[M]]

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
  protected def prePage(condition: String, parameters: List[Any], pageNumber: Long, pageSize: Int,
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
  protected def postPage(condition: String, parameters: List[Any], pageNumber: Long, pageSize: Int,
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
  protected def doPage(condition: String, parameters: List[Any], pageNumber: Long, pageSize: Int, context: EZStorageContext): Resp[Page[M]]

  /**
    * 计数前处理
    *
    * @param condition  条件，SQL (相当于Where中的条件)或Json
    * @param parameters 参数 ，Mongo不需要
    * @param context    上下文
    * @return 是否允许计数
    */
  protected def preCount(condition: String, parameters: List[Any], context: EZStorageContext): Resp[(String, List[Any])] = Resp.success((condition, parameters))

  /**
    * 计数后处理
    *
    * @param condition   条件，SQL (相当于Where中的条件)或Json
    * @param parameters  参数 ，Mongo不需要
    * @param countResult 是否存在
    * @param context     上下文
    * @return 处理后的结果
    */
  protected def postCount(condition: String, parameters: List[Any], countResult: Long, context: EZStorageContext): Resp[Long] = Resp.success(countResult)

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
  protected def doCount(condition: String, parameters: List[Any], context: EZStorageContext): Resp[Long]

}



