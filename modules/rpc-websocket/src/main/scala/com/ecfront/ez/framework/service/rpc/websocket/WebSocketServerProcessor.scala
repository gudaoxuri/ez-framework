package com.ecfront.ez.framework.service.rpc.websocket

import java.net.URLDecoder

import com.ecfront.common.{JsonHelper, Resp}
import com.ecfront.ez.framework.core.EZContext
import com.ecfront.ez.framework.service.rpc.foundation.{EZRPCContext, Fun, Router}
import com.typesafe.scalalogging.slf4j.LazyLogging
import io.vertx.core.buffer.Buffer
import io.vertx.core.http._
import io.vertx.core.{AsyncResult, Future, Handler}

class WebSocketServerProcessor extends Handler[ServerWebSocket] with LazyLogging {

  protected val FLAG_METHOD: String = "__method__"

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
        logger.error("Http process error.", ex)
        returnContent(s"Request process error：${ex.getMessage}", request)
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
    val method = if (request.headers().contains(FLAG_METHOD)) request.headers.get(FLAG_METHOD).toUpperCase else "GET"
    val result = Router.getFunction("WebSocket", method, request.path(), parameters)
    parameters = result._3
    if (result._1) {
      if (method == "POST" || method == "PUT") {
        request.frameHandler(new Handler[WebSocketFrame] {
          override def handle(event: WebSocketFrame): Unit = {
            execute(method, result._4, parameters, event.textData(), result._2, ip, request)
          }
        })
      } else {
        execute(method, result._4, parameters, null, result._2, ip, request)
      }
    } else {
      returnContent(result._1, request)
    }
  }

  private def execute(method: String, path: String, parameters: Map[String, String], body: Any, fun: Fun[_], ip: String, request: ServerWebSocket): Unit = {
    val context = new EZRPCContext()
    context.remoteIP = ip
    WebSocketProcessor.createWS(method, path, request)
    EZContext.vertx.executeBlocking(new Handler[Future[Resp[Any]]] {
      override def handle(e: Future[Resp[Any]]): Unit = {
        e.complete(fun.execute(parameters, if (body != null) JsonHelper.toObject(body, fun.requestClass) else null, context))
      }
    }, false, new Handler[AsyncResult[Resp[Any]]] {
      override def handle(e: AsyncResult[Resp[Any]]): Unit = {
        returnContent(e.result(), request)
      }
    })
  }

  private def returnContent(result: Any, request: ServerWebSocket): ServerWebSocket = {
    val body = result match {
      case r: String => r
      case _ => JsonHelper.toJsonString(result)
    }
    request.write(Buffer.buffer(body))
  }

}
