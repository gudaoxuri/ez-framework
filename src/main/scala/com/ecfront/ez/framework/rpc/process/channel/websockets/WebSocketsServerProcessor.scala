package com.ecfront.ez.framework.rpc.process.channel.websockets

import java.net.URLDecoder
import java.util.concurrent.CountDownLatch

import com.ecfront.common.{JsonHelper, Resp}
import com.ecfront.ez.framework.rpc.Fun
import com.ecfront.ez.framework.rpc.process.ServerProcessor
import io.vertx.core._
import io.vertx.core.buffer.Buffer
import io.vertx.core.http._

import scala.collection.JavaConversions._

/**
 * WebSockets服务处理器
 */
class WebSocketsServerProcessor extends ServerProcessor {

  private var server: HttpServer = _

  override protected def init(): Unit = {
    val latch = new CountDownLatch(1)
    server = vertx.createHttpServer(new HttpServerOptions().setHost(host).setPort(port).setCompressionSupported(true))
      .websocketHandler(new Handler[ServerWebSocket] {
        override def handle(request: ServerWebSocket): Unit = {
          val parameters = collection.mutable.Map[String, String]()
          val interceptInfo = collection.mutable.Map[String, String]()
          if (request.query() != null && request.query().trim.nonEmpty) {
            URLDecoder.decode(request.query(), "UTF-8").split("&").foreach {
              item =>
                //只支持有值的参数
                val kv = item.split("=")
                parameters += (kv(0) -> kv(1))
            }
          }
          val method = if (request.headers().contains(FLAG_METHOD)) request.headers.get(FLAG_METHOD).toUpperCase else "GET"
          request.headers().foreach {
            item =>
              if (item.getKey.startsWith(FLAG_INTERCEPTOR_INFO)) {
                interceptInfo += item.getKey.substring(FLAG_INTERCEPTOR_INFO.length) -> item.getValue
              }
          }
          val (preResult, fun, newParameters, postFun) = router.getFunction(method, request.path(), parameters.toMap, interceptInfo.toMap)
          if (preResult) {
            if (method == "POST" || method == "PUT") {
              request.frameHandler(new Handler[WebSocketFrame] {
                override def handle(event: WebSocketFrame): Unit = {
                  execute(method, request.uri(), newParameters, event.textData(), preResult.body, fun, postFun, request)
                }
              })
            } else {
              execute(method, request.path(), newParameters, null, preResult.body, fun, postFun, request)
            }
          } else {
            returnContent(preResult, request)
          }
        }
      }).listen(new Handler[AsyncResult[HttpServer]] {
      override def handle(event: AsyncResult[HttpServer]): Unit = {
        if (event.succeeded()) {
          latch.countDown()
        } else {
          logger.error("Startup fail.", event.cause())
        }
      }
    })
    latch.await()
  }

  override protected[rpc] def process(method: String, path: String, isRegex: Boolean): Unit = {}

  override private[rpc] def destroy(): Unit = {
    val latch = new CountDownLatch(1)
    server.close(new Handler[AsyncResult[Void]] {
      override def handle(event: AsyncResult[Void]): Unit = {
        if (event.succeeded()) {
          latch.countDown()
        } else {
          logger.error("Shutdown failed.", event.cause())
        }
      }
    })
    latch.await()
  }

  private def execute(method: String, uri: String, parameters: Map[String, String], body: Any, preData: Any, fun: Fun[_], postFun: => Any => Any, request: ServerWebSocket) {
    vertx.executeBlocking(new Handler[Future[Any]] {
      override def handle(future: Future[Any]): Unit = {
        try {
          val result =
            if (fun != null) {
              fun.execute(parameters, JsonHelper.toObject(body, fun.requestClass), preData)
            } else {
              rpcServer.any(method, uri, parameters, body, preData)
            }
          returnContent(postFun(result), request)
        }
        catch {
          case e: Exception =>
            logger.error("Execute function error.", e)
            returnContent(Resp.serverError(e.getMessage), request)
        }
        future.complete()
      }
    }, false, new Handler[AsyncResult[Any]] {
      override def handle(event: AsyncResult[Any]): Unit = {
      }
    })
  }

  private def returnContent(result: Any, request: ServerWebSocket) {
    val body = result match {
      case r: String => r
      case _ => JsonHelper.toJsonString(result)
    }
    request.write(Buffer.buffer(body))
  }

}
