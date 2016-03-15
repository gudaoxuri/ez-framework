package com.ecfront.ez.framework.service.masterslave

import com.ecfront.common.Resp
import com.ecfront.ez.framework.core.{EZContext, EZServiceAdapter}
import io.vertx.core.json.JsonObject

import scala.collection.JavaConversions._
import scala.collection.mutable

object ServiceAdapter extends EZServiceAdapter[JsonObject] {

  override def init(parameter: JsonObject): Resp[String] = {
    Assigner.init(EZContext.module, parameter.getString("clusterId"))
    if (parameter.containsKey("category")) {
      parameter.getJsonObject("category").foreach {
        item =>
          ExecutorPool.initPool(
            item.getKey,
            item.getValue.asInstanceOf[JsonObject].getInteger("pool", ExecutorPool.DEFAULT_MAX_NUMBER),
            item.getValue.asInstanceOf[JsonObject].getBoolean("newThread", ExecutorPool.DEFAULT_IS_NEW_TREAD)
          )
      }
    }
    Resp.success("")
  }

  override def destroy(parameter: JsonObject): Resp[String] = {
    Assigner.close()
    Resp.success("")
  }

  override lazy val dependents: mutable.Set[String] = mutable.Set(
    com.ecfront.ez.framework.service.kafka.ServiceAdapter.serviceName
  )

}


