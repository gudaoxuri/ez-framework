package com.asto.ez.framework.helper

import com.ecfront.common.{JsonHelper, Resp, StandardCode}
import com.typesafe.scalalogging.slf4j.LazyLogging
import io.vertx.core.Handler
import io.vertx.core.buffer.Buffer
import io.vertx.core.http._
import scala.async.Async.{async, await}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}

/**
  * HTTP 异步请求操作辅助类
  *
  * 包含了对HTTP GET POST PUT DELETE 四类常用操作
  *
  */
object HttpClientHelper extends LazyLogging {

  var httpClient: HttpClient = _

  def get[E](url: String, responseClass: Class[E], contentType: String = "application/json"): Future[Resp[E]] = {
    request(HttpMethod.GET, url, null, responseClass, contentType)
  }

  def post[E](url: String, body: Any, responseClass: Class[E], contentType: String = "application/json"): Future[Resp[E]] = {
    request(HttpMethod.POST, url, body, responseClass, contentType)
  }

  def put[E](url: String, body: Any, responseClass: Class[E], contentType: String = "application/json"): Future[Resp[E]] = {
    request(HttpMethod.PUT, url, body, responseClass, contentType)
  }

  def delete[E](url: String, responseClass: Class[E], contentType: String = "application/json"): Future[Resp[E]] = {
    request(HttpMethod.DELETE, url, null, responseClass, contentType)
  }

  private def request[E](method: HttpMethod, url: String, body: Any, responseClass: Class[E], contentType: String): Future[Resp[E]] = async {
    val p = Promise[Resp[E]]()
    val client = httpClient.requestAbs(method, url, new Handler[HttpClientResponse] {
      override def handle(response: HttpClientResponse): Unit = {
        if (response.statusCode + "" != StandardCode.SUCCESS) {
          logger.error("Server NOT responded.")
          p.success(Resp.serverUnavailable(s"Server [$method:$url] NOT responded."))
        } else {
          response.bodyHandler(new Handler[Buffer] {
            override def handle(data: Buffer): Unit = {
              val result = JsonHelper.toJson(data.toString("UTF-8"))
              if (result.has("code") && result.has("message") && result.has("body")) {
                val body = if (result.get("body") != null) {
                  JsonHelper.toObject(result.get("body"), responseClass)
                } else {
                  null.asInstanceOf
                }
                val resp = Resp[E](result.get("code").asText(), result.get("message").asText(), Some(body))
                resp.body = body
                p.success(resp)
              } else {
                p.success(Resp.success(JsonHelper.toObject(result, responseClass)))
              }
            }
          })
        }
      }
    }).putHeader("content-type", contentType)
    if (body != null) {
      contentType.toLowerCase match {
        case c if c == "application/x-www-form-urlencoded" && body.isInstanceOf[Map[_, _]] =>
          client.end(body.asInstanceOf[Map[String, String]].map(i => i._1 + "=" + i._2).mkString("&"))
        case _ =>
          client.end(JsonHelper.toJsonString(body))
      }
    } else {
      client.end()
    }
    await(p.future)
  }

  def returnContent(result: Any, response: HttpServerResponse, accept: String = "application/json; charset=UTF-8") {
    //支持CORS
    val res = result match {
      case r: String => r
      case _ => JsonHelper.toJsonString(result)
    }
    logger.trace("Response: \r\n" + res)
    response.setStatusCode(200).putHeader("Content-Type", accept)
      .putHeader("Cache-Control", "no-cache")
      .putHeader("Access-Control-Allow-Origin", "*")
      .putHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
      .putHeader("Access-Control-Allow-Headers", "Content-Type, X-Requested-With, X-authentication, X-client")
      .end(res)
  }

}

