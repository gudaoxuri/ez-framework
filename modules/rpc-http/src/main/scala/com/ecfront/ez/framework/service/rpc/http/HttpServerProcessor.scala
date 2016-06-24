package com.ecfront.ez.framework.service.rpc.http

import java.io.File
import java.net.URLEncoder
import java.nio.file.Files

import com.ecfront.common.{JsonHelper, Resp}
import com.ecfront.ez.framework.core.EZContext
import com.ecfront.ez.framework.core.helper.FileType
import com.ecfront.ez.framework.core.interceptor.EZAsyncInterceptorProcessor
import com.ecfront.ez.framework.service.rpc.foundation._
import com.ecfront.ez.framework.service.rpc.http.interceptor.AntiDDoSInterceptor
import com.typesafe.scalalogging.slf4j.LazyLogging
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.{HttpServerFileUpload, HttpServerRequest, HttpServerResponse}
import io.vertx.core.json.{JsonArray, JsonObject}
import io.vertx.core.{AsyncResult, Future, Handler}
import org.joox.JOOX._
import org.w3c.dom.Document

import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * HTTP 服务操作
  *
  * @param resourcePath             路径根路径
  * @param accessControlAllowOrigin 允许跨域的域名
  */
class HttpServerProcessor(resourcePath: String, accessControlAllowOrigin: String = "*") extends Handler[HttpServerRequest] with LazyLogging {


  private val HTTP_STATUS_200: Int = 200
  private val HTTP_STATUS_302: Int = 302

  override def handle(request: HttpServerRequest): Unit = {
    if (request.method().name() == "OPTIONS") {
      returnContent(request.response(), "text/html", "")
    } else if (request.path() != "/favicon.ico") {
      val ip =
        if (request.headers().contains("X-Forwarded-For") && request.getHeader("X-Forwarded-For").nonEmpty) {
          request.getHeader("X-Forwarded-For")
        } else {
          request.remoteAddress().host()
        }
      logger.trace(s"Receive a request [${request.uri()}] , from $ip ")
      try {
        val ddosFilterR = AntiDDoSInterceptor.limitFilter(ip)
        if (ddosFilterR) {
          router(request, ip)
        } else {
          returnContent(ddosFilterR, request.response(), "application/json", "application/json; charset=utf-8")
        }
      } catch {
        case ex: Throwable =>
          logger.error("Http process error.", ex)
          returnContent(request.response(), "text/html", s"Request process error：${ex.getMessage}")
      }
    }
  }

  private def router(request: HttpServerRequest, ip: String): Unit = {
    val accept =
      if (request.headers().contains("Accept") && request.headers().get("Accept") != "*.*") {
        request.headers().get("Accept").split(",")(0).toLowerCase
      } else {
        "application/json"
      }
    val contentType =
      if (request.headers().contains("Content-Type")) {
        request.headers().get("Content-Type").toLowerCase
      } else {
        "application/json; charset=utf-8"
      }
    val result = Router.getFunction("HTTP", request.method().name(), request.path(),
      request.params().map(entry => entry.getKey -> entry.getValue).toMap, ip)
    val parameters = result._3
    val context = new EZRPCContext()
    context.remoteIP = ip
    context.method = request.method().name()
    context.templateUri = result._4
    context.realUri = request.uri()
    context.parameters = parameters
    context.accept = accept.toLowerCase()
    context.contentType = contentType.toLowerCase()
    if (result._1) {
      execute(request, result._2, context)
    } else {
      AntiDDoSInterceptor.addIllegal(context.remoteIP)
      returnContent(result._1, request.response(), context.accept, context.contentType)
    }
  }

  private def execute(request: HttpServerRequest, fun: Fun[_], context: EZRPCContext): Unit = {
    if (context.contentType.startsWith("multipart/form-data")) {
      // 上传处理
      request.setExpectMultipart(true)
      request.uploadHandler(new Handler[HttpServerFileUpload] {
        override def handle(upload: HttpServerFileUpload): Unit = {
          val newName = if (request.params().contains("name")) {
            request.params().get("name")
          } else {
            upload.contentType()
            if (upload.filename().contains(".")) {
              upload.filename()
                .substring(0, upload.filename().lastIndexOf(".")) + "_" + System.nanoTime() + "." +
                upload.filename().substring(upload.filename().lastIndexOf(".") + 1)
            } else {
              upload.filename() + "_" + System.nanoTime()
            }
          }
          val tPath = resourcePath + newName
          upload.exceptionHandler(new Handler[Throwable] {
            override def handle(e: Throwable): Unit = {
              logger.error(s"Upload error ${context.method}:${context.realUri} from ${context.remoteIP}")
              returnContent(Resp.serverError(e.getMessage), request.response(), context.accept, context.contentType)
            }
          })
          upload.endHandler(new Handler[Void] {
            override def handle(e: Void): Unit = {
              context.contentType = "application/json; charset=utf-8"
              execute(request, newName, fun, context, request.response())
            }
          })
          upload.streamToFileSystem(tPath)
        }
      })
    } else if (request.method().name() == "POST" || request.method().name() == "PUT") {
      // Post或Put请求，需要处理Body
      request.bodyHandler(new Handler[Buffer] {
        override def handle(data: Buffer): Unit = {
          execute(request, data.getString(0, data.length), fun, context, request.response())
        }
      })
    } else {
      // Get或Delete请求
      execute(request, null, fun, context, request.response())
    }
  }

  private def execute(request: HttpServerRequest, body: Any, fun: Fun[_], context: EZRPCContext, response: HttpServerResponse): Unit = {
    logger.trace(s"Execute a request from ${context.remoteIP} to [${context.method}] ${context.realUri} | $body")
    EZAsyncInterceptorProcessor.process[EZRPCContext](HttpInterceptor.category, context).onSuccess {
      case interResp =>
        if (interResp) {
          val newContext = interResp.body._1
          try {
            val b = if (body != null) {
              newContext.contentType match {
                case t if t.contains("json") => JsonHelper.toObject(body, fun.requestClass)
                case t if t.contains("xml") =>
                  if (fun.requestClass == classOf[Document]) {
                    $(body.asInstanceOf[String]).document()
                  } else if (fun.requestClass == classOf[String]) {
                    body.asInstanceOf[String]
                  } else {
                    logger.warn(s"Not support return type [${fun.requestClass.getName}] by xml")
                    null
                  }
                case _ =>
                  logger.warn("Not support content type:" + newContext.contentType)
                  null
              }
            } else {
              null
            }
            EZContext.vertx.executeBlocking(new Handler[Future[Resp[Any]]] {
              override def handle(e: Future[Resp[Any]]): Unit = {
                e.complete(fun.execute(newContext.parameters, b, newContext))
              }
            }, false, new Handler[AsyncResult[Resp[Any]]] {
              override def handle(e: AsyncResult[Resp[Any]]): Unit = {
                returnContent(e.result(), response, newContext.accept, newContext.contentType)
              }
            })
          } catch {
            case e: Exception =>
              logger.error("Request content error.", e)
              returnContent(Resp.unsupportedMediaType(e.getMessage), response, newContext.accept, newContext.contentType)
          }
        } else {
          returnContent(interResp, request.response(), context.accept, context.contentType)
        }
    }
  }

  private def returnContent(result: Any, response: HttpServerResponse, accept: String, contentType: String): Unit = {
    result match {
      case r: Resp[_] if r && r.body.isInstanceOf[File] =>
        val file = r.body.asInstanceOf[File]
        val fileType = Files.probeContentType(file.toPath)
        if (FileType.TYPE_IMAGE.contains(fileType)) {
          // 显示图片
          response.putHeader("Content-Transfer-Encoding", "binary")
        } else {
          // 资源下载
          response.putHeader("Content-disposition", "attachment; filename=" + URLEncoder.encode(file.getName, "UTF-8"))
        }
        response.setStatusCode(HTTP_STATUS_200)
        response.sendFile(file.getPath)
      case r: Resp[_] if r && r.body.isInstanceOf[RespRedirect] =>
        // 重定向
        response.putHeader("Location", r.body.asInstanceOf[RespRedirect].url)
          .setStatusCode(HTTP_STATUS_302)
          .end()
      case r: Resp[_] if r && r.body.isInstanceOf[Raw] =>
        returnContent(response, accept, r.body.asInstanceOf[Raw].raw.toString)
      case r: Resp[_] if r && (r.body.isInstanceOf[JsonObject] || r.body.isInstanceOf[JsonArray]) =>
        val res = new JsonObject().put("code", r.code).put("message", r.message).put("body", r.body).encodePrettily()
        returnContent(response, accept, res)
      case r: Resp[_] =>
        val res = contentType match {
          case t if t.contains("json") =>
            // Json
            JsonHelper.toJsonString(r)
          case t if t.contains("xml") =>
            // Xml
            if (r) {
              if (r.body == null) {
                ""
              } else {
                r.body match {
                  case b: Document =>
                    $(b).toString
                  case b: String => b
                  case _ =>
                    logger.warn(s"Not support return type [${r.body.getClass.getName}] by xml")
                    s"""<?xml version="1.0" encoding="UTF-8"?>
                        |<xml>
                        | <error>
                        |  <code>-1</code>
                        |  <message>Not support return type [${r.body.getClass.getName}] by xml</message>
                        | </error>
                        |</xml>
                 """.stripMargin
                }
              }
            } else {
              s"""<?xml version="1.0" encoding="UTF-8"?>
                  |<xml>
                  | <error>
                  |  <code>${r.code}</code>
                  |  <message>${r.message}</message>
                  | </error>
                  |</xml>
                 """.stripMargin
            }
          case _ => r.toString
        }
        returnContent(response, accept, res)
      case _ => logger.warn(s"The response type [${result.getClass.getName}] not support")
    }
  }

  private def returnContent(response: HttpServerResponse, accept: String, res: String): Unit = {
    logger.trace("Response: \r\n" + res)
    // 支持CORS
    response.setStatusCode(HTTP_STATUS_200).putHeader("Content-Type", accept)
      .putHeader("Cache-Control", "no-cache")
      .putHeader("Access-Control-Allow-Origin", accessControlAllowOrigin)
      .putHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
      .putHeader("Access-Control-Allow-Headers", "Content-Type, X-Requested-With, X-authentication, X-client")
      .end(res)
  }

}
