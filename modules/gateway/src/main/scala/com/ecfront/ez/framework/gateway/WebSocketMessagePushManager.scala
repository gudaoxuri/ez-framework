package com.ecfront.ez.framework.gateway

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
  private[websocket] def createWS(path: String, webSocket: ServerWebSocket): Unit = {
    if (!webSocketContainer.contains(path)) {
      webSocketContainer += path -> ListBuffer[ServerWebSocket]()
    }
    webSocket.closeHandler(new Handler[Void] {
      override def handle(event: Void): Unit = {
        webSocketContainer(path) -= webSocket
      }
    })
    webSocketContainer(path) += webSocket
  }

  /**
    * 向所有客户端推送消息
    *
    * @param path   连接路径
    * @param data   消息
    */
  def ws(path: String, data: Any): Unit = {
    if (webSocketContainer.contains(path)) {
      webSocketContainer(path).foreach(_.writeFinalTextFrame(JsonHelper.toJsonString(data)))
    }
  }

  /**
    * 移除推送消息
    *
    * @param path     连接路径
    * @param matchAll 是否匹配全路径，为false时只按前缀匹配
    */
  def remove(path: String, matchAll: Boolean = true): Unit = {
    if (!matchAll) {
      webSocketContainer.keys.filter(_.startsWith(path)).foreach {
        webSocketContainer -= _
      }
    } else {
      webSocketContainer -= path
    }
  }

}

