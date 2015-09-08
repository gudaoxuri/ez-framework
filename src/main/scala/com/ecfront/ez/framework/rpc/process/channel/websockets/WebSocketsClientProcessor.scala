package com.ecfront.ez.framework.rpc.process.channel.websockets

import java.net.URL

import com.ecfront.common.{JsonHelper, Resp}
import com.ecfront.ez.framework.rpc.process.ClientProcessor
import io.netty.handler.codec.http.DefaultHttpHeaders
import io.vertx.core.Handler
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.impl.HeadersAdaptor
import io.vertx.core.http.{HttpClientOptions, WebSocket}

/**
 * WebSockets连接处理器
 */
class WebSocketsClientProcessor extends ClientProcessor {

  private val webSocketClient = vertx.createHttpClient(new HttpClientOptions().setMaxPoolSize(200))

  override protected def init(): Unit = {
  }

  override protected def doProcess[E, F](method: String, path: String, requestBody: Any, responseClass: Class[E], resultClass: Class[F], finishFun: => (Option[F]) => Unit, inject: Any): Unit = {
    val (tHost, tPort, tPath) = getUrlInfo(path)
    val headers = new DefaultHttpHeaders
    headers.add(FLAG_METHOD, method)
    val preResult = rpcClient.preExecuteInterceptor(method, tPath, inject)
    if (preResult) {
      webSocketClient.websocket(tPort, tHost, tPath, new HeadersAdaptor(setHeader(method, preResult.body)), new Handler[WebSocket] {
        override def handle(response: WebSocket): Unit = {
          response.write(Buffer.buffer(getBody(requestBody)))
          if (responseClass != null) {
            response.handler(new Handler[Buffer] {
              override def handle(data: Buffer): Unit = {
                if (resultClass == classOf[Resp[E]]) {
                  clientExecute(method, tPath, finishFun, Some(parseResp(data.getString(0, data.length), responseClass).asInstanceOf[F]))
                } else {
                  clientExecute(method, tPath, finishFun, Some(JsonHelper.toObject(data.getString(0, data.length), responseClass).asInstanceOf[F]))
                }
              }
            })
          } else {
            clientExecute(method, tPath, finishFun, None)
          }
        }
      })
    }
  }

  private def getUrlInfo(path: String): (String, Int, String) = {
    var tHost = host
    var tPort = port
    var tPath = path
    if (path.toLowerCase.startsWith("http")) {
      val url = new URL(path)
      tHost = url.getHost
      tPort = if (url.getPort == -1) 80 else url.getPort
      tPath = url.getPath
    }
    (tHost, tPort, tPath)
  }

  private def setHeader(method: String, interceptInfo: Map[String, String]): DefaultHttpHeaders = {
    val headers = new DefaultHttpHeaders
    headers.add(FLAG_METHOD, method)
    if (interceptInfo != null) {
      interceptInfo.foreach {
        item =>
          headers.add(FLAG_INTERCEPTOR_INFO + item._1, item._2)
      }
    }
    headers
  }

  private def getBody(requestBody: Any): String = {
    requestBody match {
      case b: String => b
      case _ => JsonHelper.toJsonString(requestBody)
    }
  }

  override private[rpc] def destroy(): Unit = {
    webSocketClient.close()
  }

}

