package com.ecfront.ez.framework.service.rpc.websocket

import com.ecfront.common.Resp
import com.ecfront.ez.framework.core.{EZContext, EZServiceAdapter}
import com.ecfront.ez.framework.service.rpc.foundation.AutoBuildingProcessor
import io.vertx.core.http.HttpServer
import io.vertx.core.json.JsonObject
import io.vertx.core.{AsyncResult, Handler}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Promise}

object ServiceAdapter extends EZServiceAdapter[JsonObject] {

  override def init(parameter: JsonObject): Resp[String] = {
    val servicePath = parameter.getString("servicePath")
    AutoBuildingProcessor.autoBuilding[WebSocket](servicePath, classOf[WebSocket])
    val p = Promise[Resp[String]]()
    EZContext.vertx
      .createHttpServer().websocketHandler(new WebSocketServerProcessor)
      .listen(parameter.getInteger("port"), parameter.getString("host"), new Handler[AsyncResult[HttpServer]] {
        override def handle(event: AsyncResult[HttpServer]): Unit = {
          if (event.succeeded()) {
            p.success(Resp.success(
              s"""WS start successful.
                  | ws://${parameter.getString("host")}:${parameter.getInteger("port")}/""".stripMargin))
          } else {
            logger.error(s"WS start fail .", event.cause())
            p.success(Resp.serverError(s"WS start fail : ${event.cause().getMessage}"))
          }
        }
      })
    Await.result(p.future, Duration.Inf)
  }

  override def destroy(parameter: JsonObject): Resp[String] = {
    Resp.success("")
  }

  override var serviceName: String = "websocket"
}


