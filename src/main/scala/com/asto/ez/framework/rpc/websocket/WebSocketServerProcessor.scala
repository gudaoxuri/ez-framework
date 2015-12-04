package com.asto.ez.framework.rpc.websocket

import java.net.URLDecoder

import com.asto.ez.framework.EZContext
import com.asto.ez.framework.rpc.{EChannel, Fun, Router}
import com.ecfront.common.{JsonHelper, Resp}
import com.typesafe.scalalogging.slf4j.LazyLogging
import io.vertx.core.Handler
import io.vertx.core.buffer.Buffer
import io.vertx.core.http._

import scala.concurrent.ExecutionContext.Implicits.global

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
        returnContent(s"请求处理错误：${ex.getMessage}", request)
    }
  }

  private def router(request: ServerWebSocket, ip: String): Unit = {
    var parameters =
      if (request.query()!=null&&request.query().nonEmpty)
      URLDecoder.decode(request.query(), "UTF-8").split("&").map {
        item =>
          val entry = item.split("=")
          entry(0) -> entry(1)
      }.toMap
    else Map[String, String]()
    val method = if (request.headers().contains(FLAG_METHOD)) request.headers.get(FLAG_METHOD).toUpperCase else "GET"
    val result = Router.getFunction(EChannel.WEB_SOCKETS, method, request.path(), parameters)
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

  private def execute(method: String, path: String, parameters: Map[String, String], body: Any, fun: Fun[_], ip: String, request: ServerWebSocket) {
    try {
      //TODO
      val context = EZContext()
      context.remoteIP = ip
      WebSocketSProcssor.createWS(method, path, request)
      fun.execute(parameters,if(body != null) JsonHelper.toObject(body, fun.requestClass) else null, context).onSuccess {
        case excResp =>
          returnContent(excResp, request)
      }
    } catch {
      case e: Exception =>
        logger.error("Execute function error.", e)
        returnContent(Resp.serverError(e.getMessage), request)
    }
  }

  private def returnContent(result: Any, request: ServerWebSocket) {
    val body = result match {
      case r: String => r
      case _ => JsonHelper.toJsonString(result)
    }
    request.write(Buffer.buffer(body))
  }

}
