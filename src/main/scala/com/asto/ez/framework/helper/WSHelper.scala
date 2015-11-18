package com.asto.ez.framework.helper

import com.ecfront.common.JsonHelper
import com.typesafe.scalalogging.slf4j.LazyLogging
import io.vertx.core.Handler
import io.vertx.core.http._

import scala.collection.mutable.ListBuffer

/**
  * Websocket 异步操作辅助类
  *
  */
object WSHelper extends LazyLogging {

  private val websockets = collection.mutable.Map[String, ListBuffer[ServerWebSocket]]()

  def createWS(method: String, path: String, websocket: ServerWebSocket): Unit = {
    if (!websockets.contains(method + ":" + path)) {
      websockets += (method + ":" + path) -> ListBuffer[ServerWebSocket]()
    }
    websocket.closeHandler(new Handler[Void] {
      override def handle(event: Void): Unit = {
        websockets(method + ":" + path) -= websocket
      }
    })
    websockets(method + ":" + path) += websocket
  }

  def ws(method: String, path: String, data: Any) = {
    if (websockets.contains(method + ":" + path)) {
      websockets(method + ":" + path).foreach(_.writeFinalTextFrame(JsonHelper.toJsonString(data)))
    }
  }

}

