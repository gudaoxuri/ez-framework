package com.ecfront.ez.framework.service.gateway

import java.net.URLDecoder

import com.ecfront.common.JsonHelper
import com.ecfront.ez.framework.core.rpc.{Channel, Method}
import com.ecfront.ez.framework.service.gateway.interceptor.EZAPIContext
import io.vertx.core.Handler
import io.vertx.core.http._

/**
  * WebSocket 服务操作
  */
class WebSocketServerProcessor extends Handler[ServerWebSocket] with GatewayProcessor {

  override def handle(request: ServerWebSocket): Unit = {
    val ip =
      if (request.headers().contains(FLAG_PROXY) && request.headers.get(FLAG_PROXY).nonEmpty) {
        request.headers.get(FLAG_PROXY)
      } else {
        request.remoteAddress().host()
      }
    logger.trace(s"Receive a request [${request.uri()}] , from $ip ")
    try {
      router(request, ip)
    } catch {
      case ex: Throwable =>
        logger.error("WS process error.", ex)
        request.writeFinalTextFrame("Request process error：${ex.getMessage}")
    }
  }

  private def router(request: ServerWebSocket, ip: String): Unit = {
    val parameters =
      if (request.query() != null && request.query().nonEmpty) {
        URLDecoder.decode(request.query(), "UTF-8").split("&").map {
          item =>
            val entry = item.split("=")
            entry(0) -> entry(1)
        }.toMap
      } else {
        Map[String, String]()
      }
    val result = LocalCacheContainer.getRouter(Channel.WS.toString, Method.WS.toString, request.path(), parameters, ip)
    WebSocketMessagePushManager.createWS(result._3, request)
    if (result._1) {
      val context = new EZAPIContext()
      context.remoteIP = ip
      context.channel = Channel.WS.toString
      context.method = Method.WS.toString
      context.templateUri = result._3
      context.realUri = request.uri()
      context.parameters = result._2
      context.accept = ""
      context.contentType = ""
      request.frameHandler(new Handler[WebSocketFrame] {
        override def handle(event: WebSocketFrame): Unit = {
          execute(request, event.textData(), context)
        }
      })
    } else {
      request.writeFinalTextFrame(JsonHelper.toJsonString(result._1))
    }
  }

  private def execute(request: ServerWebSocket, body: String, context: EZAPIContext): Unit = {
    execute(body, context, {
      resp =>
        WebSocketMessagePushManager.ws(context.templateUri, resp.body._1.executeResult)
    })
  }

}
