package com.ecfront.ez.framework.service.storage.mongo

import com.ecfront.common.Resp
import com.ecfront.ez.framework.service.storage.foundation.{EZStorageContext, StatusModel, StatusStorage}

/**
  * Mongo带状态的持久化实现
  *
  * @tparam M 实体类型
  */
trait MongoStatusStorage[M <: StatusModel] extends MongoBaseStorage[M] with StatusStorage[M] {

  override def doEnableById(id: Any, context: EZStorageContext): Resp[Void] = {
    doUpdateByCond( s"""{"$$set":{"enable":true}}""", s"""{"_id":"$id"}""", List(id), context)
  }

  override def doDisableById(id: Any, context: EZStorageContext): Resp[Void] = {
    doUpdateByCond( s"""{"$$set":{"enable":false}}""", s"""{"_id":"$id"}""", List(id), context)
  }

  override protected def appendEnabled(condition: String): String = {
    packageCondition(condition).put(StatusModel.ENABLE_FLAG, true).encode()
  }

}






