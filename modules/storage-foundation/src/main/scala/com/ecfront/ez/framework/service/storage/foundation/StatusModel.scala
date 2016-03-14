package com.ecfront.ez.framework.service.storage.foundation

import com.ecfront.common.Resp

import scala.beans.BeanProperty

trait StatusModel extends BaseModel {

  @Index
  @BeanProperty var enable: Boolean = _

}

object StatusModel {

  val ENABLE_FLAG = "enable"

}

trait StatusStorage[M <: StatusModel] extends BaseStorage[M] {

  protected def appendEnabled(condition: String): String

  protected def preGetEnabledByCond(condition: String, parameters: List[Any], context: EZStorageContext): Resp[(String, List[Any])] = Resp.success((condition, parameters))

  protected def postGetEnabledByCond(condition: String, parameters: List[Any], doResult: M, context: EZStorageContext): Resp[M] = Resp.success(doResult)

  def getEnabledByCond(condition: String, parameters: List[Any] = List(), context: EZStorageContext = null): Resp[M] = {
    if (condition == null) {
      Resp.badRequest("【condition】not null")
    } else {
      val preR = preGetEnabledByCond(condition, parameters, context)
      if (preR) {
        val doR = doGetEnabledByCond(preR.body._1, preR.body._2, context)
        if (doR) {
          postGetEnabledByCond(preR.body._1, preR.body._2, doR.body, context)
        } else {
          doR
        }
      } else {
        preR
      }
    }
  }

  protected def doGetEnabledByCond(condition: String, parameters: List[Any], context: EZStorageContext): Resp[M] = {
    getByCond(appendEnabled(condition), parameters, context)
  }

  protected def preFindEnabled(condition: String, parameters: List[Any], context: EZStorageContext): Resp[(String, List[Any])] = Resp.success((condition, parameters))

  protected def postFindEnabled(condition: String, parameters: List[Any], doResult: List[M], context: EZStorageContext): Resp[List[M]] = Resp.success(doResult)

  def findEnabled(condition: String, parameters: List[Any] = List(), context: EZStorageContext = null): Resp[List[M]] = {
    val preR = preFindEnabled(condition, parameters, context)
    if (preR) {
      val doR = doFindEnabled(preR.body._1, preR.body._2, context)
      if (doR) {
        postFindEnabled(preR.body._1, preR.body._2, doR.body, context)
      } else {
        doR
      }
    } else {
      preR
    }
  }

  protected def doFindEnabled(condition: String, parameters: List[Any], context: EZStorageContext): Resp[List[M]] = {
    doFind(appendEnabled(condition), parameters, context)
  }

  protected def prePageEnabled(condition: String, parameters: List[Any], pageNumber: Long, pageSize: Int, context: EZStorageContext): Resp[(String, List[Any])] = Resp.success((condition, parameters))

  protected def postPageEnabled(condition: String, parameters: List[Any], pageNumber: Long, pageSize: Int, doResult: Page[M], context: EZStorageContext): Resp[Page[M]] = Resp.success(doResult)

  def pageEnabled(condition: String, parameters: List[Any] = List(), pageNumber: Long = 1, pageSize: Int = 10, context: EZStorageContext = null): Resp[Page[M]] = {
    val preR = prePageEnabled(condition, parameters, pageNumber, pageSize, context)
    if (preR) {
      val doR = doPageEnabled(preR.body._1, preR.body._2, pageNumber, pageSize, context)
      if (doR) {
        postPageEnabled(preR.body._1, preR.body._2, pageNumber, pageSize, doR.body, context)
      } else {
        doR
      }
    } else {
      preR
    }
  }

  protected def doPageEnabled(condition: String, parameters: List[Any], pageNumber: Long, pageSize: Int, context: EZStorageContext): Resp[Page[M]] = {
    doPage(appendEnabled(condition), parameters, pageNumber, pageSize, context)
  }

  protected def preExistEnabledByCond(condition: String, parameters: List[Any], context: EZStorageContext): Resp[(String, List[Any])] = Resp.success((condition, parameters))

  protected def postExistEnabledByCond(condition: String, parameters: List[Any], doResult: Boolean, context: EZStorageContext): Resp[Boolean] = Resp.success(doResult)

  def existEnabledByCond(condition: String, parameters: List[Any] = List(), context: EZStorageContext = null): Resp[Boolean] = {
    if (condition == null) {
      Resp.badRequest("【condition】not null")
    } else {
      val preR = preExistEnabledByCond(condition, parameters, context)
      if (preR) {
        val doR = doExistEnabledByCond(preR.body._1, preR.body._2, context)
        if (doR) {
          postExistEnabledByCond(preR.body._1, preR.body._2, doR.body, context)
        } else {
          doR
        }
      } else {
        preR
      }
    }
  }

  protected def doExistEnabledByCond(condition: String, parameters: List[Any], context: EZStorageContext): Resp[Boolean] = {
    doExistByCond(appendEnabled(condition), parameters, context)
  }

  protected def preCountEnabled(condition: String, parameters: List[Any], context: EZStorageContext): Resp[(String, List[Any])] = Resp.success((condition, parameters))

  protected def postCountEnabled(condition: String, parameters: List[Any], doResult: Long, context: EZStorageContext): Resp[Long] = Resp.success(doResult)

  def countEnabled(condition: String, parameters: List[Any] = List(), context: EZStorageContext = null): Resp[Long] = {
    val preR = preCountEnabled(condition, parameters, context)
    if (preR) {
      val doR = doCountEnabled(preR.body._1, preR.body._2, context)
      if (doR) {
        postCountEnabled(preR.body._1, preR.body._2, doR.body, context)
      } else {
        doR
      }
    } else {
      preR
    }
  }

  protected def doCountEnabled(condition: String, parameters: List[Any], context: EZStorageContext): Resp[Long] = {
    doCount(appendEnabled(condition), parameters, context)
  }

  protected def preEnableById(id: Any, context: EZStorageContext): Resp[Any] = Resp.success(id)

  protected def postEnableById(id: Any, context: EZStorageContext): Resp[Void] = Resp.success(null)

  def enableById(id: Any, context: EZStorageContext = null): Resp[Void] = {
    if (id == null) {
      Resp.badRequest("【id】not null")
    } else {
      val preR = preEnableById(id, context)
      if (preR) {
        val doR = doEnableById(preR.body, context)
        if (doR) {
          postEnableById(preR.body, context)
        } else {
          doR
        }
      } else {
        preR
      }
    }
  }

  protected def doEnableById(id: Any, context: EZStorageContext): Resp[Void]

  protected def preDisableById(id: Any, context: EZStorageContext): Resp[Any] = Resp.success(id)

  protected def postDisableById(id: Any, context: EZStorageContext): Resp[Void] = Resp.success(null)

  def disableById(id: Any, context: EZStorageContext = null): Resp[Void] = {
    if (id == null) {
      Resp.badRequest("【id】not null")
    } else {
      val preR = preDisableById(id, context)
      if (preR) {
        val doR = doDisableById(preR.body, context)
        if (doR) {
          postDisableById(preR.body, context)
        } else {
          doR
        }
      } else {
        preR
      }
    }
  }

  protected def doDisableById(id: Any, context: EZStorageContext): Resp[Void]

}









