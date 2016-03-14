package com.ecfront.ez.framework.service.rpc.websocket

import com.ecfront.common.JsonHelper
import com.typesafe.scalalogging.slf4j.LazyLogging
import io.vertx.core.Handler
import io.vertx.core.http._

import scala.collection.mutable.ListBuffer

/**
  * Websocket 异步操作辅助类
  *
  */
object WebSocketProcessor extends LazyLogging {

  private val webSocketContainer = collection.mutable.Map[String, ListBuffer[ServerWebSocket]]()

  private[websocket] def createWS(method: String, path: String, webSocket: ServerWebSocket): Unit = {
    if (!webSocketContainer.contains(method + ":" + path)) {
      webSocketContainer += (method + ":" + path) -> ListBuffer[ServerWebSocket]()
    }
    webSocket.closeHandler(new Handler[Void] {
      override def handle(event: Void): Unit = {
        webSocketContainer(method + ":" + path) -= webSocket
      }
    })
    webSocketContainer(method + ":" + path) += webSocket
  }

  def ws(method: String, path: String, data: Any): Unit = {
    if (webSocketContainer.contains(method + ":" + path)) {
      webSocketContainer(method + ":" + path).foreach(_.writeFinalTextFrame(JsonHelper.toJsonString(data)))
    }
  }

}

