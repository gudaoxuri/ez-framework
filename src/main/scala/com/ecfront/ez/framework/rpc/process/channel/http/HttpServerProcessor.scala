package com.ecfront.ez.framework.rpc.process.channel.http

import java.io.File
import java.util.concurrent.CountDownLatch

import com.ecfront.common.{JsonHelper, Resp}
import com.ecfront.ez.framework.rpc.Fun
import com.ecfront.ez.framework.rpc.process.ServerProcessor
import io.vertx.core._
import io.vertx.core.buffer.Buffer
import io.vertx.core.http._
import org.jsoup.Jsoup

import scala.collection.JavaConversions._

/**
 * HTTP服务处理器
 */
class HttpServerProcessor extends ServerProcessor {

  private var server: HttpServer = _

  override protected def init(): Unit = {
    val latch = new CountDownLatch(1)
    server = vertx.createHttpServer(new HttpServerOptions().setHost(host).setPort(port).setCompressionSupported(true).setTcpKeepAlive(true))
      .requestHandler(new Handler[HttpServerRequest] {
        override def handle(request: HttpServerRequest): Unit = {
          if (request.method().name() == "OPTIONS") {
            returnContent("", request.response(), "text/html")
          } else {
            if (request.path() != "/favicon.ico") {
              val accept =
                if (request.headers().contains("Accept") && request.headers().get("Accept") != "*.*") request.headers().get("Accept").split(",")(0).toLowerCase else "application/json"
              val contentType =
                if (request.headers().contains("Content-Type")) request.headers().get("Content-Type").toLowerCase else "application/json; charset=UTF-8"
              val parameters = collection.mutable.Map[String, String]()
              val interceptInfo = collection.mutable.Map[String, String]()
              request.params().entries().foreach {
                item =>
                  //排除框架定义的变量
                  if (!item.getKey.startsWith("__") && !item.getKey.endsWith("__")) {
                    parameters += item.getKey -> item.getValue
                  } else if (item.getKey.startsWith(FLAG_INTERCEPTOR_INFO)) {
                    interceptInfo += item.getKey.substring(FLAG_INTERCEPTOR_INFO.length) -> item.getValue
                  }
              }
              val uri = request.path() + (if (request.path().contains("?")) "" else "?") + parameters.map(item => item._1 + "=" + item._2).mkString("&")
              val (preResult, fun, newParameters, postFun) = router.getFunction(request.method().name(), request.path(), parameters.toMap, interceptInfo.toMap)
              if (preResult) {
                if (request.headers().contains("Content-Type") && (request.headers.get("Content-Type").toLowerCase.startsWith("multipart/form-data") || request.headers.get("Content-Type").toLowerCase.startsWith("application/x-www-form-urlencoded"))) {
                  //上传处理
                  request.setExpectMultipart(true)
                  request.uploadHandler(new Handler[HttpServerFileUpload] {
                    override def handle(upload: HttpServerFileUpload): Unit = {
                      var path = if (request.params().contains("path")) request.params().get("path") else ""
                      if (path.nonEmpty && !path.endsWith(File.separator)) {
                        path += File.separator
                      }
                      val newName = if (request.params().contains("name")) request.params().get("name")
                      else {
                        if (upload.filename().contains(".")) {
                          upload.filename().substring(0, upload.filename().lastIndexOf(".")) + "_" + System.nanoTime() + "." + upload.filename().substring(upload.filename().lastIndexOf(".") + 1)
                        } else {
                          upload.filename() + "_" + System.nanoTime()
                        }
                      }
                      val tPath = rootUploadPath + path + newName
                      upload.exceptionHandler(new Handler[Throwable] {
                        override def handle(e: Throwable): Unit = {
                          returnContent(Resp.serverError(e.getMessage), request.response(), accept)
                        }
                      })
                      upload.endHandler(new Handler[Void] {
                        override def handle(e: Void): Unit = {
                          execute(request.method().name(), uri, newParameters, path + newName, preResult.body, fun, postFun, request.response(), "application/json; charset=UTF-8",accept)
                        }
                      })
                      upload.streamToFileSystem(tPath)
                    }
                  })
                } else if (request.method().name() == "POST" || request.method().name() == "PUT") {
                  //Post或Put请求，需要处理Body
                  request.bodyHandler(new Handler[Buffer] {
                    override def handle(data: Buffer): Unit = {
                      execute(request.method().name(), uri, newParameters, data.getString(0, data.length), preResult.body, fun, postFun, request.response(), contentType, accept)
                    }
                  })
                } else {
                  //Get或Delete请求
                  execute(request.method().name(), uri, newParameters, null, preResult.body, fun, postFun, request.response(), contentType, accept)
                }
              } else {
                //前置处理错误，直接返回结果
                returnContent(preResult, request.response(), accept)
              }
            }
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

  private def execute(method: String, uri: String, parameters: Map[String, String], body: Any, preData: Any, fun: Fun[_], postFun: => Any => Any, response: HttpServerResponse, contentType: String, accept: String) {
    vertx.executeBlocking(new Handler[Future[Any]] {
      override def handle(future: Future[Any]): Unit = {
        try {
          val result =
            if (fun != null) {
              val b = contentType match {
                case t if t.contains("json") => JsonHelper.toObject(body, fun.requestClass)
                case t if t.contains("xml") => Jsoup.parse(body.asInstanceOf[String])
                case _ => logger.error("Not support content type:" + contentType)
              }
              fun.execute(parameters, b, preData)
            } else {
              rpcServer.any(method, uri, parameters, body, preData)
            }
          returnContent(postFun(result), response, accept)
        }
        catch {
          case e: Exception =>
            logger.error("Execute function error.", e)
            returnContent(Resp.serverError(e.getMessage), response, accept)
        }
        future.complete()
      }
    }, false, new Handler[AsyncResult[Any]] {
      override def handle(event: AsyncResult[Any]): Unit = {
      }
    })
  }

  private def returnContent(result: Any, response: HttpServerResponse, accept: String) {
    val (body, contentType) = accept match {
      case t if t.contains("json") => result match {
        case r: String => (r, "application/json; charset=UTF-8")
        case _ => (JsonHelper.toJsonString(result), "application/json; charset=UTF-8")
      }
      case t if t.contains("xml") => (result.toString, "text/xml; charset=UTF-8")
      case _ => (result.toString, s"$accept; charset=UTF-8")
    }
    //支持CORS
    response.setStatusCode(200).putHeader("Content-Type", contentType)
      .putHeader("Cache-Control", "no-cache")
      .putHeader("Access-Control-Allow-Origin", "*")
      .putHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
      .putHeader("Access-Control-Allow-Headers", "Content-Type, X-Requested-With, X-authentication, X-client")
      .end(body)
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

}
