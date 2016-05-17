package com.ecfront.ez.framework.service.rpc.websocket

import com.ecfront.common.JsonHelper
import com.typesafe.scalalogging.slf4j.LazyLogging
import io.vertx.core.Handler
import io.vertx.core.http._

import scala.collection.mutable.ListBuffer

/**
  * WebSocket 消息推送管理
  *
  */
object WebSocketMessagePushManager extends LazyLogging {

  private val webSocketContainer = collection.mutable.Map[String, ListBuffer[ServerWebSocket]]()

  // 注册新的WebSocket客户端
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

  /**
    * 向所有客户端推送消息
    *
    * @param method 连接方法，目前只限于 `REQUEST` 方法
    * @param path   连接路径
    * @param data   消息
    */
  def ws(method: String, path: String, data: Any): Unit = {
    if (webSocketContainer.contains(method + ":" + path)) {
      webSocketContainer(method + ":" + path).foreach(_.writeFinalTextFrame(JsonHelper.toJsonString(data)))
    }
  }

  /**
    * 移除推送消息
    *
    * @param method 连接方法，目前只限于 `REQUEST` 方法
    * @param path   连接路径
    */
  def remove(method: String, path: String): Unit = {
    webSocketContainer -= method + ":" + path
  }

}

