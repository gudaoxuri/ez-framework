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
    * @param parameters 参数
    * @return 是否允许获取
    */
  def preGetEnabledByCond(
                           condition: String, parameters: List[Any]): Resp[(String, List[Any])] = Resp.success((condition, parameters))

  /**
    * 获取一条启用的记录后处理
    *
    * @param condition  条件，SQL (相当于Where中的条件)或Json
    * @param parameters 参数
    * @param getResult  获取到的记录
    * @return 处理后的记录
    */
  def postGetEnabledByCond(condition: String, parameters: List[Any], getResult: M): Resp[M] = Resp.success(getResult)

  /**
    * 获取一条启用的记录
    *
    * @param condition  条件，SQL (相当于Where中的条件)或Json
    * @param parameters 参数
    * @return 获取到的记录
    */
  def getEnabledByCond(condition: String, parameters: List[Any] = List()): Resp[M] = {
    if (condition == null) {
      logger.warn("【condition】not null")
      Resp.badRequest("【condition】not null")
    } else {
      val preR = preGetEnabledByCond(condition, parameters)
      if (preR) {
        val filterR = filterByCond(preR.body._1, preR.body._2)
        if (filterR) {
          val doR = doGetEnabledByCond(filterR.body._1, filterR.body._2)
          if (doR) {
            postGetEnabledByCond(filterR.body._1, filterR.body._2, doR.body)
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
    * @param parameters 参数
    * @return 获取到的记录
    */
  def doGetEnabledByCond(condition: String, parameters: List[Any]): Resp[M] = {
    getByCond(appendEnabled(condition), parameters)
  }

  /**
    * 启用记录查找前处理
    *
    * @param condition  条件，SQL (相当于Where中的条件)或Json
    * @param parameters 参数
    * @return 是否允许查找
    */
  def preFindEnabled(
                      condition: String, parameters: List[Any]): Resp[(String, List[Any])] = Resp.success((condition, parameters))

  /**
    * 启用记录查找后处理
    *
    * @param condition  条件，SQL (相当于Where中的条件)或Json
    * @param parameters 参数
    * @param findResult 查找到的记录
    * @return 处理后的记录
    */
  def postFindEnabled(
                       condition: String, parameters: List[Any],
                       findResult: List[M]): Resp[List[M]] = Resp.success(findResult)

  /**
    * 启用记录查找
    *
    * @param condition  条件，SQL (相当于Where中的条件)或Json
    * @param parameters 参数
    * @return 查找到的记录
    */
  def findEnabled(condition: String, parameters: List[Any] = List()): Resp[List[M]] = {
    val preR = preFindEnabled(condition, parameters)
    if (preR) {
      val filterR = filterByCond(preR.body._1, preR.body._2)
      if (filterR) {
        val doR = doFindEnabled(filterR.body._1, filterR.body._2)
        if (doR) {
          postFindEnabled(filterR.body._1, filterR.body._2, doR.body)
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
    * @param parameters 参数
    * @return 查找到的记录
    */
  def doFindEnabled(condition: String, parameters: List[Any]): Resp[List[M]] = {
    doFind(appendEnabled(condition), parameters)
  }

  /**
    * 启用记录分页前处理
    *
    * @param condition  条件，SQL (相当于Where中的条件)或Json
    * @param parameters 参数
    * @param pageNumber 当前页，从1开始
    * @param pageSize   每页条数
    * @return 是否允许分页
    */
  def prePageEnabled(condition: String, parameters: List[Any],
                     pageNumber: Long, pageSize: Int): Resp[(String, List[Any])] = Resp.success((condition, parameters))

  /**
    * 启用记录分页后处理
    *
    * @param condition  条件，SQL (相当于Where中的条件)或Json
    * @param parameters 参数
    * @param pageNumber 当前页，从1开始
    * @param pageSize   每页条数
    * @param pageResult 是否存在
    * @return 处理后的结果
    */
  def postPageEnabled(condition: String, parameters: List[Any],
                      pageNumber: Long, pageSize: Int, pageResult: Page[M]): Resp[Page[M]] = Resp.success(pageResult)

  /**
    * 启用记录分页
    *
    * @param condition  条件，SQL (相当于Where中的条件)或Json
    * @param parameters 参数
    * @param pageNumber 当前页，从1开始
    * @param pageSize   每页条数
    * @return 分页结果
    */
  def pageEnabled(
                   condition: String, parameters: List[Any] = List(), pageNumber: Long = 1, pageSize: Int = 10): Resp[Page[M]] = {
    val preR = prePageEnabled(condition, parameters, pageNumber, pageSize)
    if (preR) {
      val filterR = filterByCond(preR.body._1, preR.body._2)
      if (filterR) {
        val doR = doPageEnabled(filterR.body._1, filterR.body._2, pageNumber, pageSize)
        if (doR) {
          postPageEnabled(filterR.body._1, filterR.body._2, pageNumber, pageSize, doR.body)
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
    * @param parameters 参数
    * @param pageNumber 当前页，从1开始
    * @param pageSize   每页条数
    * @return 分页结果
    */
  def doPageEnabled(condition: String, parameters: List[Any], pageNumber: Long, pageSize: Int): Resp[Page[M]] = {
    doPage(appendEnabled(condition), parameters, pageNumber, pageSize)
  }

  /**
    * 判断启用记录是否存在前处理
    *
    * @param condition  条件，SQL (相当于Where中的条件)或Json
    * @param parameters 参数
    * @return 是否允许判断是否存在
    */
  def preExistEnabledByCond(condition: String,
                            parameters: List[Any]): Resp[(String, List[Any])] = Resp.success((condition, parameters))

  /**
    * 判断启用记录是否存在后处理
    *
    * @param condition   条件，SQL (相当于Where中的条件)或Json
    * @param parameters  参数
    * @param existResult 是否存在
    * @return 处理后的结果
    */
  def postExistEnabledByCond(
                              condition: String,
                              parameters: List[Any],
                              existResult: Boolean): Resp[Boolean] = Resp.success(existResult)

  /**
    * 判断启用记录是否存在
    *
    * @param condition  条件，SQL (相当于Where中的条件)或Json
    * @param parameters 参数
    * @return 是否存在
    */
  def existEnabledByCond(condition: String, parameters: List[Any] = List()): Resp[Boolean] = {
    if (condition == null) {
      logger.warn("【condition】not null")
      Resp.badRequest("【condition】not null")
    } else {
      val preR = preExistEnabledByCond(condition, parameters)
      if (preR) {
        val filterR = filterByCond(preR.body._1, preR.body._2)
        if (filterR) {
          val doR = doExistEnabledByCond(filterR.body._1, filterR.body._2)
          if (doR) {
            postExistEnabledByCond(filterR.body._1, filterR.body._2, doR.body)
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
    * @param parameters 参数
    * @return 是否存在
    */
  def doExistEnabledByCond(condition: String, parameters: List[Any]): Resp[Boolean] = {
    doExistByCond(appendEnabled(condition), parameters)
  }

  /**
    * 启用记录计数前处理
    *
    * @param condition  条件，SQL (相当于Where中的条件)或Json
    * @param parameters 参数
    * @return 是否允许计数
    */
  def preCountEnabled(
                       condition: String,
                       parameters: List[Any]): Resp[(String, List[Any])] = Resp.success((condition, parameters))

  /**
    * 启用记录计数后处理
    *
    * @param condition   条件，SQL (相当于Where中的条件)或Json
    * @param parameters  参数
    * @param countResult 是否存在
    * @return 处理后的结果
    */
  def postCountEnabled(condition: String, parameters: List[Any], countResult: Long): Resp[Long] = Resp.success(countResult)

  /**
    * 启用记录计数
    *
    * @param condition  条件，SQL (相当于Where中的条件)或Json
    * @param parameters 参数
    * @return 条数
    */
  def countEnabled(condition: String, parameters: List[Any] = List()): Resp[Long] = {
    val preR = preCountEnabled(condition, parameters)
    if (preR) {
      val filterR = filterByCond(preR.body._1, preR.body._2)
      if (filterR) {
        val doR = doCountEnabled(filterR.body._1, filterR.body._2)
        if (doR) {
          postCountEnabled(filterR.body._1, filterR.body._2, doR.body)
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
    * @param parameters 参数
    * @return 条数
    */
  def doCountEnabled(condition: String, parameters: List[Any]): Resp[Long] = {
    doCount(appendEnabled(condition), parameters)
  }

  /**
    * 启用一条记录前处理
    *
    * @param id 主键
    * @return 是否允许启用
    */
  def preEnableById(id: Any): Resp[Any] = Resp.success(id)

  /**
    * 启用一条记录后处理
    *
    * @param id 主键
    * @return 处理后的结果
    */
  def postEnableById(id: Any): Resp[Void] = Resp.success(null)

  /**
    * 启用一条记录
    *
    * @param id 主键
    * @return 启用结果
    */
  def enableById(id: Any): Resp[Void] = {
    if (id == null) {
      logger.warn("【id】not null")
      Resp.badRequest("【id】not null")
    } else {
      val preR = preEnableById(id)
      if (preR) {
        val filterR = filterById(id)
        if (filterR) {
          val doR =
            if (filterR.body == null) {
              doEnableById(preR.body)
            } else {
              val m = doGetByCond(filterR.body._1, filterR.body._2)
              if (m && m.body != null) {
                doEnableById(getIdValue(m.body))
              } else {
                Resp.notFound("")
              }
            }
          if (doR) {
            postEnableById(preR.body)
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
    * @param id 主键
    * @return 启用结果
    */
  protected def doEnableById(id: Any): Resp[Void] = {
    doUpdateByCond(" enable = true ", s"${_entityInfo.idFieldName} = ?", List(id))
  }

  /**
    * 禁用一条记录前处理
    *
    * @param id 主键
    * @return 是否允许禁用
    */
  def preDisableById(id: Any): Resp[Any] = Resp.success(id)

  /**
    * 禁用一条记录后处理
    *
    * @param id 主键
    * @return 处理后的结果
    */
  def postDisableById(id: Any): Resp[Void] = Resp.success(null)

  /**
    * 禁用一条记录
    *
    * @param id 主键
    * @return 禁用结果
    */
  def disableById(id: Any): Resp[Void] = {
    if (id == null) {
      logger.warn("【id】not null")
      Resp.badRequest("【id】not null")
    } else {
      val preR = preDisableById(id)
      if (preR) {
        val filterR = filterById(id)
        if (filterR) {
          val doR =
            if (filterR.body == null) {
              doDisableById(preR.body)
            } else {
              val m = doGetByCond(filterR.body._1, filterR.body._2)
              if (m && m.body != null) {
                doDisableById(getIdValue(m.body))
              } else {
                Resp.notFound("")
              }
            }
          if (doR) {
            postDisableById(preR.body)
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
    * @param id 主键
    * @return 禁用结果
    */
  protected def doDisableById(id: Any): Resp[Void] = {
    doUpdateByCond(" enable = false ", s"${_entityInfo.idFieldName} = ?", List(id))
  }


  /**
    * 启用一条记录前处理
    *
    * @param uuid 主键
    * @return 是否允许启用
    */
  def preEnableByUUID(uuid: String): Resp[String] = Resp.success(uuid)

  /**
    * 启用一条记录后处理
    *
    * @param uuid 主键
    * @return 处理后的结果
    */
  def postEnableByUUID(uuid: String): Resp[Void] = Resp.success(null)

  /**
    * 启用一条记录
    *
    * @param uuid 主键
    * @return 启用结果
    */
  def enableByUUID(uuid: String): Resp[Void] = {
    if (uuid == null) {
      logger.warn("【uuid】not null")
      Resp.badRequest("【uuid】not null")
    } else {
      val preR = preEnableByUUID(uuid)
      if (preR) {
        val filterR = filterByUUID(uuid)
        if (filterR) {
          val doR =
            if (filterR.body == null) {
              doEnableByUUID(preR.body)
            } else {
              val m = doGetByCond(filterR.body._1, filterR.body._2)
              if (m && m.body != null) {
                doEnableByUUID(getUUIDValue(m.body))
              } else {
                Resp.notFound("")
              }
            }
          if (doR) {
            postEnableByUUID(preR.body)
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
    * @param uuid 主键
    * @return 启用结果
    */
  protected def doEnableByUUID(uuid: String): Resp[Void] = {
    doUpdateByCond(" enable = true ", s"${_entityInfo.uuidFieldName} = ?", List(uuid))
  }

  /**
    * 禁用一条记录前处理
    *
    * @param uuid 主键
    * @return 是否允许禁用
    */
  def preDisableByUUID(uuid: String): Resp[String] = Resp.success(uuid)

  /**
    * 禁用一条记录后处理
    *
    * @param uuid 主键
    * @return 处理后的结果
    */
  def postDisableByUUID(uuid: String): Resp[Void] = Resp.success(null)

  /**
    * 禁用一条记录
    *
    * @param uuid 主键
    * @return 禁用结果
    */
  def disableByUUID(uuid: String): Resp[Void] = {
    if (uuid == null) {
      logger.warn("【uuid】not null")
      Resp.badRequest("【uuid】not null")
    } else {
      val preR = preDisableByUUID(uuid)
      if (preR) {
        val filterR = filterByUUID(uuid)
        if (filterR) {
          val doR =
            if (filterR.body == null) {
              doDisableByUUID(preR.body)
            } else {
              val m = doGetByCond(filterR.body._1, filterR.body._2)
              if (m && m.body != null) {
                doDisableByUUID(getUUIDValue(m.body))
              } else {
                Resp.notFound("")
              }
            }
          if (doR) {
            postDisableByUUID(preR.body)
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
    * @param uuid 主键
    * @return 禁用结果
    */
  protected def doDisableByUUID(uuid: String): Resp[Void] = {
    doUpdateByCond(" enable = false ", s"${_entityInfo.uuidFieldName} = ?", List(uuid))
  }

  /**
    * 禁用前处理
    *
    * @param condition  条件，SQL (相当于Where中的条件)
    * @param parameters 参数
    * @return 是否允许禁用
    */
  def preDisableByCond(condition: String, parameters: List[Any]): Resp[(String, List[Any])] =
  Resp.success((condition, parameters))

  /**
    * 禁用后处理
    *
    * @param condition  条件，SQL (相当于Where中的条件)
    * @param parameters 参数
    * @return 是否成功
    */
  def postDisableByCond(condition: String, parameters: List[Any]): Resp[Void] = Resp.success(null)

  /**
    * 禁用
    *
    * @param condition  条件，SQL (相当于Where中的条件)
    * @param parameters 参数
    * @return 是否成功
    */
  def disableByCond(condition: String, parameters: List[Any] = List()): Resp[Void] = {
    val preR = preDisableByCond(condition, parameters)
    if (preR) {
      val filterR = filterByCond(preR.body._1, preR.body._2)
      if (filterR) {
        val doR = doDisableByCond(preR.body._1, filterR.body._2)
        if (doR) {
          postDisableByCond(preR.body._1, filterR.body._2)
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
    * 禁用实现方法
    *
    * @param condition  条件，SQL (相当于Where中的条件)
    * @param parameters 参数
    * @return 是否成功
    */
  protected def doDisableByCond(condition: String, parameters: List[Any]): Resp[Void] = {
    doUpdateByCond("enable = false", condition, parameters)
  }

  /**
    * 启用前处理
    *
    * @param condition  条件，SQL (相当于Where中的条件)
    * @param parameters 参数
    * @return 是否允许启用
    */
  def preEnableByCond(condition: String, parameters: List[Any]): Resp[(String, List[Any])] =
  Resp.success((condition, parameters))

  /**
    * 启用后处理
    *
    * @param condition  条件，SQL (相当于Where中的条件)
    * @param parameters 参数
    * @return 是否成功
    */
  def postEnableByCond(condition: String, parameters: List[Any]): Resp[Void] = Resp.success(null)

  /**
    * 启用
    *
    * @param condition  条件，SQL (相当于Where中的条件)
    * @param parameters 参数
    * @return 是否成功
    */
  def enableByCond(condition: String, parameters: List[Any] = List()): Resp[Void] = {
    val preR = preEnableByCond(condition, parameters)
    if (preR) {
      val filterR = filterByCond(preR.body._1, preR.body._2)
      if (filterR) {
        val doR = doEnableByCond(preR.body._1, filterR.body._2)
        if (doR) {
          postEnableByCond(preR.body._1, filterR.body._2)
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
    * 启用实现方法
    *
    * @param condition  条件，SQL (相当于Where中的条件)
    * @param parameters 参数
    * @return 是否成功
    */
  protected def doEnableByCond(condition: String, parameters: List[Any]): Resp[Void] = {
    doUpdateByCond("enable = true", condition, parameters)
  }


}






