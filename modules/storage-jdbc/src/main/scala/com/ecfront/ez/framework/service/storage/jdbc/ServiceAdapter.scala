package com.ecfront.ez.framework.service.storage.jdbc

import com.ecfront.common.Resp
import com.ecfront.ez.framework.core.{EZContext, EZManager, EZServiceAdapter}
import io.vertx.core.json.JsonObject
import io.vertx.ext.jdbc.JDBCClient

object ServiceAdapter extends EZServiceAdapter[JsonObject] {

  override def init(parameter: JsonObject): Resp[String] = {
    // 此处使用新创建的Vertx的实例，解决在对接http并发过100时僵死的问题
    JDBCProcessor.dbClient = JDBCClient.createShared(EZManager.initVertx(EZContext.perf), parameter)
    Resp.success("")
  }

  override def destroy(parameter: JsonObject): Resp[String] = {
    Resp.success("")
  }

  override var serviceName: String = "storage.jdbc"

}


