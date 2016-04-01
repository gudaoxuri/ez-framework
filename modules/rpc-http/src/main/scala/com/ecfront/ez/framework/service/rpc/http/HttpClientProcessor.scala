package com.ecfront.ez.framework.service.rpc.http

import com.ecfront.common.JsonHelper
import com.typesafe.scalalogging.slf4j.LazyLogging
import io.vertx.core.buffer.Buffer
import io.vertx.core.http._
import io.vertx.core.{Handler, Vertx}
import org.jsoup.nodes.Document

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future, Promise}

/**
  * HTTP 请求操作
  *
  * 包含了对HTTP GET POST PUT DELETE 四类常用操作
  *
  */
object HttpClientProcessor extends LazyLogging {


  private var httpClient: HttpClient = _
  private var httpClients: HttpClient = _

  def init(vertx: Vertx): Unit = {
    httpClient = vertx.createHttpClient()
    httpClients = vertx.createHttpClient(new HttpClientOptions().setSsl(true).setVerifyHost(false).setTrustAll(true))
  }

  /**
    * GET 请求
    *
    * @param url         请求URL
    * @param contentType 请求类型，默认为 application/json; charset=utf-8
    * @return 请求结果，string类型
    */
  def get(url: String, contentType: String = "application/json; charset=utf-8"): String = {
    Await.result(Async.get(url, contentType), Duration.Inf)
  }

  /**
    * POST 请求
    *
    * @param url         请求URL
    * @param body        请求体
    * @param contentType 请求类型，默认为 application/json; charset=utf-8
    * @return 请求结果，string类型
    */
  def post(url: String, body: Any, contentType: String = "application/json; charset=utf-8"): String = {
    Await.result(Async.post(url, body, contentType), Duration.Inf)
  }

  /**
    * PUT 请求
    *
    * @param url         请求URL
    * @param body        请求体
    * @param contentType 请求类型，默认为 application/json; charset=utf-8
    * @return 请求结果，string类型
    */
  def put(url: String, body: Any, contentType: String = "application/json; charset=utf-8"): String = {
    Await.result(Async.put(url, body, contentType), Duration.Inf)
  }

  /**
    * DELETE 请求
    *
    * @param url         请求URL
    * @param contentType 请求类型，默认为 application/json; charset=utf-8
    * @return 请求结果，string类型
    */
  def delete(url: String, contentType: String = "application/json; charset=utf-8"): String = {
    Await.result(Async.delete(url, contentType), Duration.Inf)
  }

  object Async {

    /**
      * GET 请求
      *
      * @param url         请求URL
      * @param contentType 请求类型，默认为 application/json; charset=utf-8
      * @return 请求结果，string类型
      */
    def get(url: String, contentType: String = "application/json; charset=utf-8"): Future[String] = {
      request(HttpMethod.GET, url, null, contentType)
    }

    /**
      * POST 请求
      *
      * @param url         请求URL
      * @param body        请求体
      * @param contentType 请求类型，默认为 application/json; charset=utf-8
      * @return 请求结果，string类型
      */
    def post(url: String, body: Any, contentType: String = "application/json; charset=utf-8"): Future[String] = {
      request(HttpMethod.POST, url, body, contentType)
    }

    /**
      * PUT 请求
      *
      * @param url         请求URL
      * @param body        请求体
      * @param contentType 请求类型，默认为 application/json; charset=utf-8
      * @return 请求结果，string类型
      */
    def put(url: String, body: Any, contentType: String = "application/json; charset=utf-8"): Future[String] = {
      request(HttpMethod.PUT, url, body, contentType)
    }

    /**
      * DELETE 请求
      *
      * @param url         请求URL
      * @param contentType 请求类型，默认为 application/json; charset=utf-8
      * @return 请求结果，string类型
      */
    def delete(url: String, contentType: String = "application/json; charset=utf-8"): Future[String] = {
      request(HttpMethod.DELETE, url, null, contentType)
    }

    private[http] def request(method: HttpMethod, url: String, body: Any, contentType: String): Future[String] = {
      val p = Promise[String]()
      val clientChannel =
        if (url.trim.toLowerCase().startsWith("https")) {
          httpClients
        } else {
          httpClient
        }
      val client = clientChannel.requestAbs(method, url.trim, new Handler[HttpClientResponse] {
        override def handle(response: HttpClientResponse): Unit = {
          response.exceptionHandler(new Handler[Throwable] {
            override def handle(event: Throwable): Unit = {
              p.failure(event)
            }
          })
          response.bodyHandler(new Handler[Buffer] {
            override def handle(data: Buffer): Unit = {
              p.success(data.toString("utf-8"))
            }
          })
        }
      }).putHeader("content-type", contentType)
      if (body != null) {
        contentType.toLowerCase match {
          case t if t.toLowerCase == "application/x-www-form-urlencoded" && body.isInstanceOf[Map[_, _]] =>
            client.end(body.asInstanceOf[Map[String, String]].map(i => i._1 + "=" + i._2).mkString("&"))
          case t if t.toLowerCase.contains("xml") =>
            body match {
              case b: Document =>
                client.end(b.outerHtml())
              case b: String =>
                client.end(b)
              case _ =>
                logger.error(s"Not support return type [${body.getClass.getName}] by xml")
                client.end()
            }
          case _ =>
            client.end(JsonHelper.toJsonString(body))
        }
      } else {
        client.end()
      }
      p.future
    }

  }

}

