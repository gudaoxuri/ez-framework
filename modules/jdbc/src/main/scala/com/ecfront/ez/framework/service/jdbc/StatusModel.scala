package com.ecfront.ez.framework.service.jdbc

import com.ecfront.common.Resp

import scala.beans.BeanProperty

/**
  * 带状态的实体基类
  */
trait StatusModel extends BaseModel {

  @BeanProperty var enable: Boolean = true

}

object StatusModel {

  val ENABLE_FLAG = "enable"

}

trait StatusStorage[M <: StatusModel] extends BaseStorage[M] {

  /**
    * 注入启用条件
    *
    * @param condition 要注入的条件
    * @return 添加了启用的条件
    */
  private def appendEnabled(condition: String): String = {
    val cond =
      if (condition == null || condition.trim == "") {
        "1=1"
      } else {
        condition
      }
    cond + " AND enable = true "
  }

  /**
    * 获取一条启用的记录前处理
    *
    * @param condition  条件，SQL (相当于Where中的条件)或Json
    * @param parameters 参数 ，Mongo不需要
    * @param context    上下文
    * @return 是否允许获取
    */
  def preGetEnabledByCond(
                           condition: String,
                           parameters: List[Any],
                           context: EZStorageContext): Resp[(String, List[Any])] = Resp.success((condition, parameters))

  /**
    * 获取一条启用的记录后处理
    *
    * @param condition  条件，SQL (相当于Where中的条件)或Json
    * @param parameters 参数 ，Mongo不需要
    * @param getResult  获取到的记录
    * @param context    上下文
    * @return 处理后的记录
    */
  def postGetEnabledByCond(condition: String, parameters: List[Any], getResult: M, context: EZStorageContext): Resp[M] = Resp.success(getResult)

  /**
    * 获取一条启用的记录
    *
    * @param condition  条件，SQL (相当于Where中的条件)或Json
    * @param parameters 参数 ，Mongo不需要
    * @param context    上下文
    * @return 获取到的记录
    */
  def getEnabledByCond(condition: String, parameters: List[Any] = List(), context: EZStorageContext = EZStorageContext()): Resp[M] = {
    if (condition == null) {
      logger.warn("【condition】not null")
      Resp.badRequest("【condition】not null")
    } else {
      val preR = preGetEnabledByCond(condition, parameters, context)
      if (preR) {
        val filterR = filterByCond(preR.body._1, preR.body._2, context)
        if (filterR) {
          val doR = doGetEnabledByCond(filterR.body._1, filterR.body._2, context)
          if (doR) {
            postGetEnabledByCond(filterR.body._1, filterR.body._2, doR.body, context)
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
    * 获取一条启用的记录实现方法
    *
    * @param condition  条件，SQL (相当于Where中的条件)或Json
    * @param parameters 参数 ，Mongo不需要
    * @param context    上下文
    * @return 获取到的记录
    */
  def doGetEnabledByCond(condition: String, parameters: List[Any], context: EZStorageContext): Resp[M] = {
    getByCond(appendEnabled(condition), parameters, context)
  }

  /**
    * 启用记录查找前处理
    *
    * @param condition  条件，SQL (相当于Where中的条件)或Json
    * @param parameters 参数 ，Mongo不需要
    * @param context    上下文
    * @return 是否允许查找
    */
  def preFindEnabled(
                      condition: String,
                      parameters: List[Any],
                      context: EZStorageContext): Resp[(String, List[Any])] = Resp.success((condition, parameters))

  /**
    * 启用记录查找后处理
    *
    * @param condition  条件，SQL (相当于Where中的条件)或Json
    * @param parameters 参数 ，Mongo不需要
    * @param findResult 查找到的记录
    * @param context    上下文
    * @return 处理后的记录
    */
  def postFindEnabled(
                       condition: String,
                       parameters: List[Any],
                       findResult: List[M], context: EZStorageContext): Resp[List[M]] = Resp.success(findResult)

  /**
    * 启用记录查找
    *
    * @param condition  条件，SQL (相当于Where中的条件)或Json
    * @param parameters 参数 ，Mongo不需要
    * @param context    上下文
    * @return 查找到的记录
    */
  def findEnabled(condition: String, parameters: List[Any] = List(), context: EZStorageContext = EZStorageContext()): Resp[List[M]] = {
    val preR = preFindEnabled(condition, parameters, context)
    if (preR) {
      val filterR = filterByCond(preR.body._1, preR.body._2, context)
      if (filterR) {
        val doR = doFindEnabled(filterR.body._1, filterR.body._2, context)
        if (doR) {
          postFindEnabled(filterR.body._1, filterR.body._2, doR.body, context)
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
    * 启用记录查找实现方法
    *
    * @param condition  条件，SQL (相当于Where中的条件)或Json
    * @param parameters 参数 ，Mongo不需要
    * @param context    上下文
    * @return 查找到的记录
    */
  def doFindEnabled(condition: String, parameters: List[Any], context: EZStorageContext): Resp[List[M]] = {
    doFind(appendEnabled(condition), parameters, context)
  }

  /**
    * 启用记录分页前处理
    *
    * @param condition  条件，SQL (相当于Where中的条件)或Json
    * @param parameters 参数 ，Mongo不需要
    * @param pageNumber 当前页，从1开始
    * @param pageSize   每页条数
    * @param context    上下文
    * @return 是否允许分页
    */
  def prePageEnabled(
                      condition: String,
                      parameters: List[Any],
                      pageNumber: Long, pageSize: Int, context: EZStorageContext): Resp[(String, List[Any])] = Resp.success((condition, parameters))

  /**
    * 启用记录分页后处理
    *
    * @param condition  条件，SQL (相当于Where中的条件)或Json
    * @param parameters 参数 ，Mongo不需要
    * @param pageNumber 当前页，从1开始
    * @param pageSize   每页条数
    * @param pageResult 是否存在
    * @param context    上下文
    * @return 处理后的结果
    */
  def postPageEnabled(
                       condition: String,
                       parameters: List[Any],
                       pageNumber: Long, pageSize: Int, pageResult: Page[M], context: EZStorageContext): Resp[Page[M]] = Resp.success(pageResult)

  /**
    * 启用记录分页
    *
    * @param condition  条件，SQL (相当于Where中的条件)或Json
    * @param parameters 参数 ，Mongo不需要
    * @param pageNumber 当前页，从1开始
    * @param pageSize   每页条数
    * @param context    上下文
    * @return 分页结果
    */
  def pageEnabled(
                   condition: String,
                   parameters: List[Any] = List(),
                   pageNumber: Long = 1, pageSize: Int = 10, context: EZStorageContext = EZStorageContext()): Resp[Page[M]] = {
    val preR = prePageEnabled(condition, parameters, pageNumber, pageSize, context)
    if (preR) {
      val filterR = filterByCond(preR.body._1, preR.body._2, context)
      if (filterR) {
        val doR = doPageEnabled(filterR.body._1, filterR.body._2, pageNumber, pageSize, context)
        if (doR) {
          postPageEnabled(filterR.body._1, filterR.body._2, pageNumber, pageSize, doR.body, context)
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
    * 启用记录分页实现方法
    *
    * @param condition  条件，SQL (相当于Where中的条件)或Json
    * @param parameters 参数 ，Mongo不需要
    * @param pageNumber 当前页，从1开始
    * @param pageSize   每页条数
    * @param context    上下文
    * @return 分页结果
    */
  def doPageEnabled(condition: String, parameters: List[Any], pageNumber: Long, pageSize: Int, context: EZStorageContext): Resp[Page[M]] = {
    doPage(appendEnabled(condition), parameters, pageNumber, pageSize, context)
  }

  /**
    * 判断启用记录是否存在前处理
    *
    * @param condition  条件，SQL (相当于Where中的条件)或Json
    * @param parameters 参数 ，Mongo不需要
    * @param context    上下文
    * @return 是否允许判断是否存在
    */
  def preExistEnabledByCond(
                             condition: String,
                             parameters: List[Any],
                             context: EZStorageContext): Resp[(String, List[Any])] = Resp.success((condition, parameters))

  /**
    * 判断启用记录是否存在后处理
    *
    * @param condition   条件，SQL (相当于Where中的条件)或Json
    * @param parameters  参数 ，Mongo不需要
    * @param existResult 是否存在
    * @param context     上下文
    * @return 处理后的结果
    */
  def postExistEnabledByCond(
                              condition: String,
                              parameters: List[Any],
                              existResult: Boolean, context: EZStorageContext): Resp[Boolean] = Resp.success(existResult)

  /**
    * 判断启用记录是否存在
    *
    * @param condition  条件，SQL (相当于Where中的条件)或Json
    * @param parameters 参数 ，Mongo不需要
    * @param context    上下文
    * @return 是否存在
    */
  def existEnabledByCond(condition: String, parameters: List[Any] = List(), context: EZStorageContext = EZStorageContext()): Resp[Boolean] = {
    if (condition == null) {
      logger.warn("【condition】not null")
      Resp.badRequest("【condition】not null")
    } else {
      val preR = preExistEnabledByCond(condition, parameters, context)
      if (preR) {
        val filterR = filterByCond(preR.body._1, preR.body._2, context)
        if (filterR) {
          val doR = doExistEnabledByCond(filterR.body._1, filterR.body._2, context)
          if (doR) {
            postExistEnabledByCond(filterR.body._1, filterR.body._2, doR.body, context)
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
    * 判断启用记录是否存在实现方法
    *
    * @param condition  条件，SQL (相当于Where中的条件)或Json
    * @param parameters 参数 ，Mongo不需要
    * @param context    上下文
    * @return 是否存在
    */
  def doExistEnabledByCond(condition: String, parameters: List[Any], context: EZStorageContext): Resp[Boolean] = {
    doExistByCond(appendEnabled(condition), parameters, context)
  }

  /**
    * 启用记录计数前处理
    *
    * @param condition  条件，SQL (相当于Where中的条件)或Json
    * @param parameters 参数 ，Mongo不需要
    * @param context    上下文
    * @return 是否允许计数
    */
  def preCountEnabled(
                       condition: String,
                       parameters: List[Any],
                       context: EZStorageContext): Resp[(String, List[Any])] = Resp.success((condition, parameters))

  /**
    * 启用记录计数后处理
    *
    * @param condition   条件，SQL (相当于Where中的条件)或Json
    * @param parameters  参数 ，Mongo不需要
    * @param countResult 是否存在
    * @param context     上下文
    * @return 处理后的结果
    */
  def postCountEnabled(condition: String, parameters: List[Any], countResult: Long, context: EZStorageContext): Resp[Long] = Resp.success(countResult)

  /**
    * 启用记录计数
    *
    * @param condition  条件，SQL (相当于Where中的条件)或Json
    * @param parameters 参数 ，Mongo不需要
    * @param context    上下文
    * @return 条数
    */
  def countEnabled(condition: String, parameters: List[Any] = List(), context: EZStorageContext = EZStorageContext()): Resp[Long] = {
    val preR = preCountEnabled(condition, parameters, context)
    if (preR) {
      val filterR = filterByCond(preR.body._1, preR.body._2, context)
      if (filterR) {
        val doR = doCountEnabled(filterR.body._1, filterR.body._2, context)
        if (doR) {
          postCountEnabled(filterR.body._1, filterR.body._2, doR.body, context)
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
    * 启用记录计数实现方法
    *
    * @param condition  条件，SQL (相当于Where中的条件)或Json
    * @param parameters 参数 ，Mongo不需要
    * @param context    上下文
    * @return 条数
    */
  def doCountEnabled(condition: String, parameters: List[Any], context: EZStorageContext): Resp[Long] = {
    doCount(appendEnabled(condition), parameters, context)
  }

  /**
    * 启用一条记录前处理
    *
    * @param id      主键
    * @param context 上下文
    * @return 是否允许启用
    */
  def preEnableById(id: Any, context: EZStorageContext): Resp[Any] = Resp.success(id)

  /**
    * 启用一条记录后处理
    *
    * @param id      主键
    * @param context 上下文
    * @return 处理后的结果
    */
  def postEnableById(id: Any, context: EZStorageContext): Resp[Void] = Resp.success(null)

  /**
    * 启用一条记录
    *
    * @param id      主键
    * @param context 上下文
    * @return 启用结果
    */
  def enableById(id: Any, context: EZStorageContext = EZStorageContext()): Resp[Void] = {
    if (id == null) {
      logger.warn("【id】not null")
      Resp.badRequest("【id】not null")
    } else {
      val preR = preEnableById(id, context)
      if (preR) {
        val filterR = filterById(id, context)
        if (filterR) {
          val doR =
            if (filterR.body == null) {
              doEnableById(preR.body, context)
            } else {
              val m = doGetByCond(filterR.body._1, filterR.body._2, context)
              if (m && m.body != null) {
                doEnableById(m.body.id, context)
              } else {
                Resp.notFound("")
              }
            }
          if (doR) {
            postEnableById(preR.body, context)
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
    * 启用一条记录实现方法
    *
    * @param id      主键
    * @param context 上下文
    * @return 启用结果
    */
  protected def doEnableById(id: Any, context: EZStorageContext): Resp[Void] = {
    doUpdateByCond(" enable = true ", " id = ?", List(id), context)
  }

  /**
    * 禁用一条记录前处理
    *
    * @param id      主键
    * @param context 上下文
    * @return 是否允许禁用
    */
  def preDisableById(id: Any, context: EZStorageContext): Resp[Any] = Resp.success(id)

  /**
    * 禁用一条记录后处理
    *
    * @param id      主键
    * @param context 上下文
    * @return 处理后的结果
    */
  def postDisableById(id: Any, context: EZStorageContext): Resp[Void] = Resp.success(null)

  /**
    * 禁用一条记录
    *
    * @param id      主键
    * @param context 上下文
    * @return 禁用结果
    */
  def disableById(id: Any, context: EZStorageContext = EZStorageContext()): Resp[Void] = {
    if (id == null) {
      logger.warn("【id】not null")
      Resp.badRequest("【id】not null")
    } else {
      val preR = preDisableById(id, context)
      if (preR) {
        val filterR = filterById(id, context)
        if (filterR) {
          val doR =
            if (filterR.body == null) {
              doDisableById(preR.body, context)
            } else {
              val m = doGetByCond(filterR.body._1, filterR.body._2, context)
              if (m && m.body != null) {
                doDisableById(m.body.id, context)
              } else {
                Resp.notFound("")
              }
            }
          if (doR) {
            postDisableById(preR.body, context)
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
    * 禁用一条记录实现方法
    *
    * @param id      主键
    * @param context 上下文
    * @return 禁用结果
    */
 protected def doDisableById(id: Any, context: EZStorageContext): Resp[Void] = {
    doUpdateByCond(" enable = false ", " id = ?", List(id), context)
  }

}






