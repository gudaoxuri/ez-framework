package com.ecfront.ez.framework.rpc.process.channel.http

import java.net.URL

import com.ecfront.common.{JsonHelper, Resp, StandardCode}
import com.ecfront.ez.framework.rpc.process.ClientProcessor
import io.vertx.core.Handler
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.{HttpClientOptions, HttpClientResponse, HttpMethod}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

/**
 * HTTP 连接处理器
 */
class HttpClientProcessor extends ClientProcessor {

  private val httpClient = vertx.createHttpClient(new HttpClientOptions().setMaxPoolSize(100).setKeepAlive(true))

  override protected def init(): Unit = {
  }

  override protected def doProcess[E, F](method: String, path: String, requestBody: Any, responseClass: Class[E], resultClass: Class[F], finishFun: => (Option[F]) => Unit, inject: Any): Unit = {
    val jsonType = responseClass != classOf[Document]
    //支持json和xml
    val body = getBody(requestBody, if (jsonType) "json" else "xml")
    val (tHost, tPort, tPath) = getUrlInfo(path)
    val preResult = rpcClient.preExecuteInterceptor(method, tPath, inject)
    if (preResult) {
      val args = if (preResult.body == null) ""
      else preResult.body.map {
        item => FLAG_INTERCEPTOR_INFO + item._1 + "=" + item._2
      }.mkString("&")
      httpClient.request(HttpMethod.valueOf(method), tPort, tHost, tPath + (if (!tPath.contains("?")) "?" else "&") + args, new Handler[HttpClientResponse] {
        override def handle(response: HttpClientResponse): Unit = {
          if (responseClass != null) {
            if (response.statusCode + "" != StandardCode.SUCCESS) {
              logger.error("Server NOT responded.")
              if (resultClass.isInstance(Resp)) {
                clientExecute(method, tPath, finishFun, Some(Resp.serverUnavailable[E]("Server NOT responded.").asInstanceOf[F]))
              } else {
                clientExecute(method, tPath, finishFun, None)
              }
            } else {
              response.bodyHandler(new Handler[Buffer] {
                override def handle(data: Buffer): Unit = {
                  if (resultClass == classOf[Resp[E]]) {
                    clientExecute(method, tPath, finishFun, Some(parseResp(data.getString(0, data.length), responseClass).asInstanceOf[F]))
                  } else if (jsonType) {
                    clientExecute(method, tPath, finishFun, Some(JsonHelper.toObject(data.getString(0, data.length), responseClass).asInstanceOf[F]))
                  } else {
                    clientExecute(method, tPath, finishFun, Some(Jsoup.parse(data.getString(0, data.length)).asInstanceOf[F]))
                  }
                }
              })
            }
          } else {
            clientExecute(method, tPath, finishFun, None)
          }
        }
      }).putHeader("content-type", if (jsonType) "application/json; charset=UTF-8" else "application/xml; charset=UTF-8").end(body)
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

  private def getBody(requestBody: Any, contentType: String = "json"): String = {
    requestBody match {
      case b: String => b
      case b if contentType == "json" => JsonHelper.toJsonString(b)
      case b if contentType == "xml" => requestBody.toString
    }
  }

  override private[rpc] def destroy(): Unit = {
    httpClient.close()
  }

}

