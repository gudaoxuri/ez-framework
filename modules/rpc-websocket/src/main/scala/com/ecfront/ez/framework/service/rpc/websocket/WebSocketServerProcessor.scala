package com.ecfront.ez.framework.service.rpc.websocket

import java.net.URLDecoder

import com.ecfront.common.{JsonHelper, Resp}
import com.ecfront.ez.framework.core.EZContext
import com.ecfront.ez.framework.service.rpc.foundation.{EZRPCContext, Fun, Method, Router}
import com.typesafe.scalalogging.slf4j.LazyLogging
import io.vertx.core.http._
import io.vertx.core.{AsyncResult, Future, Handler}

/**
  * WebSocket 服务操作
  */
class WebSocketServerProcessor extends Handler[ServerWebSocket] with LazyLogging {

  override def handle(request: ServerWebSocket): Unit = {
    val ip =
      if (request.headers().contains("X-Forwarded-For") && request.headers.get("X-Forwarded-For").nonEmpty) {
        request.headers.get("X-Forwarded-For")
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
    var parameters =
      if (request.query() != null && request.query().nonEmpty) {
        URLDecoder.decode(request.query(), "UTF-8").split("&").map {
          item =>
            val entry = item.split("=")
            entry(0) -> entry(1)
        }.toMap
      } else {
        Map[String, String]()
      }
    // 目前只限于 `REQUEST` 方法
    val result = Router.getFunction("WebSocket", Method.REQUEST, request.path(), parameters)
    parameters = result._3
    WebSocketMessagePushManager.createWS(Method.REQUEST, result._4, request)
    if (result._1) {
      request.frameHandler(new Handler[WebSocketFrame] {
        override def handle(event: WebSocketFrame): Unit = {
          execute(Method.REQUEST, result._4, parameters, event.textData(), result._2, ip, request)
        }
      })
    } else {
      request.writeFinalTextFrame(JsonHelper.toJsonString(result._1))
    }
  }

  private def execute(method: String, path: String, parameters: Map[String, String], body: Any, fun: Fun[_], ip: String, request: ServerWebSocket): Unit = {
    val context = new EZRPCContext()
    context.remoteIP = ip
    context.method = method
    context.templateUri = path
    context.realUri = request.uri()
    context.parameters = parameters.map { i => i._1 -> URLDecoder.decode(i._2, "UTF-8") }
    EZContext.vertx.executeBlocking(new Handler[Future[Resp[Any]]] {
      override def handle(e: Future[Resp[Any]]): Unit = {
        e.complete(fun.execute(parameters, if (body != null) JsonHelper.toObject(body, fun.requestClass) else null, context))
      }
    }, false, new Handler[AsyncResult[Resp[Any]]] {
      override def handle(e: AsyncResult[Resp[Any]]): Unit = {
        WebSocketMessagePushManager.ws(method, path, JsonHelper.toJsonString(e.result()))
      }
    })
  }

}
