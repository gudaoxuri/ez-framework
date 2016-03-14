package com.ecfront.ez.framework.service.storage.mongo

import com.ecfront.common.Resp
import com.ecfront.ez.framework.service.storage.foundation.{EZStorageContext, StatusModel, StatusStorage}
import io.vertx.core.json.JsonObject

trait MongoStatusStorage[M <: StatusModel] extends MongoBaseStorage[M] with StatusStorage[M] {

  override def doEnableById(id: Any, context: EZStorageContext): Resp[Void] = {
    doUpdateByCond( s"""{"$$set":{"enable":true}}""", s"""{"_id":"$id"}""", List(id), context)
  }

  override def doDisableById(id: Any, context: EZStorageContext): Resp[Void] = {
    doUpdateByCond( s"""{"$$set":{"enable":false}}""", s"""{"_id":"$id"}""", List(id), context)
  }

  override protected def appendEnabled(condition: String): String = {
    new JsonObject(condition).put(StatusModel.ENABLE_FLAG, true).encode()
  }

}






