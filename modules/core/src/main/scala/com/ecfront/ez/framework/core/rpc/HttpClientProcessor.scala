package com.ecfront.ez.framework.core.rpc

import java.io.File
import java.net.URLEncoder
import java.nio.file.Files
import java.util.Date

import com.ecfront.common.JsonHelper
import com.typesafe.scalalogging.slf4j.LazyLogging
import io.vertx.core.buffer.Buffer
import io.vertx.core.file.{AsyncFile, FileProps, OpenOptions}
import io.vertx.core.http._
import io.vertx.core.streams.Pump
import io.vertx.core.{AsyncResult, Handler, Vertx}
import org.joox.JOOX._
import org.w3c.dom.Document

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future, Promise}

/**
  * HTTP 请求操作
  *
  * 包含了对HTTP GET POST PUT DELETE 四类常用操作
  *
  */
object HttpClientProcessor extends LazyLogging {

  private val FLAG_PERF_MAX_POOL_SIZE = "maxPoolSize"

  private var vertx: Vertx = _
  private var httpClient: HttpClient = _
  private var httpClients: HttpClient = _

  def init(_vertx: Vertx): Unit = {
    vertx = _vertx
    val httpOpt = new HttpClientOptions()
    val httpsOpt = new HttpClientOptions().setSsl(true).setVerifyHost(false).setTrustAll(true)
    if (System.getProperty(FLAG_PERF_MAX_POOL_SIZE) != null) {
      httpOpt.setMaxPoolSize(System.getProperty(FLAG_PERF_MAX_POOL_SIZE).toInt)
      httpsOpt.setMaxPoolSize(System.getProperty(FLAG_PERF_MAX_POOL_SIZE).toInt)
    } else {
      httpOpt.setMaxPoolSize(100)
      httpsOpt.setMaxPoolSize(100)
    }
    httpClient = vertx.createHttpClient(httpOpt)
    httpClients = vertx.createHttpClient(httpsOpt)
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

    private[core] def request(method: HttpMethod, url: String, body: Any, contentType: String): Future[String] = {
      val realContextType = getRealContextType(body, contentType)
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
      }).putHeader("content-type", realContextType)
      if (body != null) {
        realContextType.toLowerCase match {
          case t if t.toLowerCase.contains("application/x-www-form-urlencoded") && body.isInstanceOf[Map[_, _]] =>
            client.end(body.asInstanceOf[Map[String, String]].map(i => i._1 + "=" + URLEncoder.encode(i._2, "utf-8")).mkString("&"))
          case t if t.toLowerCase.contains("xml") =>
            body match {
              case b: Document =>
                client.end($(b).toString)
              case b: String =>
                client.end(b)
              case _ =>
                logger.error(s"Not support return type [${body.getClass.getName}] by xml")
                client.end()
            }
          case t if t.toLowerCase.contains("multipart/form-data") =>
            body match {
              case reqFile: ReqFile =>
                client.setChunked(false)
                client.end(getBufferBody(reqFile.file, reqFile.fieldName, reqFile.fileName))
              case _ =>
                val fs = vertx.fileSystem()
                val path = body.asInstanceOf[File].getPath
                fs.props(path, new Handler[AsyncResult[FileProps]] {
                  override def handle(event: AsyncResult[FileProps]): Unit = {
                    client.headers().set("content-length", String.valueOf(event.result().size()))
                    fs.open(path, new OpenOptions(), new Handler[AsyncResult[AsyncFile]] {
                      override def handle(event: AsyncResult[AsyncFile]): Unit = {
                        val file = event.result()
                        val pump = Pump.pump(file, client)
                        file.endHandler(new Handler[Void] {
                          override def handle(event: Void): Unit = {
                            client.end()
                          }
                        })
                        pump.start()
                      }
                    })
                  }
                })
            }
          case _ =>
            val str = body match {
              case b: String =>
                body.asInstanceOf[String]
              case b if b.isInstanceOf[Int] || b.isInstanceOf[Long] ||
                b.isInstanceOf[Float] || b.isInstanceOf[Double] ||
                b.isInstanceOf[Char] || b.isInstanceOf[Short] ||
                b.isInstanceOf[BigDecimal] || b.isInstanceOf[Boolean] ||
                b.isInstanceOf[Byte] || b.isInstanceOf[Date]
              =>
                b.toString
              case _ =>
                JsonHelper.toJsonString(body)
            }
            client.end(str)
        }
      } else {
        client.end()
      }
      p.future
    }

    private def getRealContextType(body: Any, contentType: String): String = {
      if (body != null && body.isInstanceOf[File]) {
        "multipart/form-data"
      } else if (body != null && body.isInstanceOf[ReqFile]) {
        "multipart/form-data; boundary=ez_boundary"
      } else {
        contentType
      }
    }

    private def getBufferBody(file: File, fieldName: String, fileName: String = null): Buffer = {
      val finalFileName = if (fileName == null) {
        file.getName.substring(0, file.getName.lastIndexOf(".")) + "_" + System.nanoTime() + "." + file.getName.substring(file.getName.lastIndexOf(".") + 1)
      } else {
        fileName
      }
      val buffer = Buffer.buffer()
      buffer.appendString("--ez_boundary\r\n")
      buffer.appendString("Content-Disposition: form-data; name=\"" + fieldName + "\"; filename=\"" + finalFileName + "\"\r\n")
      buffer.appendString("Content-Type: application/octet-stream\r\n")
      buffer.appendString("Content-Transfer-Encoding: binary\r\n")
      buffer.appendString("\r\n")
      buffer.appendBytes(Files.readAllBytes(file.toPath))
      buffer.appendString("\r\n")
      buffer.appendString("--ez_boundary--\r\n")
      buffer
    }

  }

}

