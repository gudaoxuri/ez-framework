package com.ecfront.ez.framework.service.rpc.http

import java.io.File
import java.net.URLDecoder

import com.ecfront.common.{JsonHelper, Resp}
import com.ecfront.ez.framework.core.EZContext
import com.ecfront.ez.framework.core.interceptor.EZAsyncInterceptorProcessor
import com.ecfront.ez.framework.service.rpc.foundation.{EZRPCContext, Fun, RespRedirect, Router}
import com.typesafe.scalalogging.slf4j.LazyLogging
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.{HttpServerFileUpload, HttpServerRequest, HttpServerResponse}
import io.vertx.core.{AsyncResult, Future, Handler}

import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * HTTP 服务操作
  *
  * @param resourcePath             路径根路径
  * @param accessControlAllowOrigin 允许跨域的域名
  */
class HttpServerProcessor(resourcePath: String, accessControlAllowOrigin: String = "*") extends Handler[HttpServerRequest] with LazyLogging {

  override def handle(request: HttpServerRequest): Unit = {
    if (request.method().name() == "OPTIONS") {
      returnContent("", request.response(), "text/html", "text/html")
    } else if (request.path() != "/favicon.ico") {
      val ip =
        if (request.headers().contains("X-Forwarded-For") && request.getHeader("X-Forwarded-For").nonEmpty) {
          request.getHeader("X-Forwarded-For")
        } else {
          request.remoteAddress().host()
        }
      logger.trace(s"Receive a request [${request.uri()}] , from $ip ")
      try {
        router(request, ip)
      } catch {
        case ex: Throwable =>
          logger.error("Http process error.", ex)
          returnContent(s"Request process error：${ex.getMessage}", request.response(), "text/html", "text/html")
      }
    }
  }

  private def router(request: HttpServerRequest, ip: String): Unit = {
    val accept =
      if (request.headers().contains("Accept") && request.headers().get("Accept") != "*.*") {
        request.headers().get("Accept").split(",")(0).toLowerCase
      } else {
        "application/json; charset=UTF-8"
      }
    val contentType =
      if (request.headers().contains("Content-Type")) {
        request.headers().get("Content-Type").toLowerCase
      } else {
        "application/json; charset=UTF-8"
      }
    val result = Router.getFunction("HTTP", request.method().name(), request.path(),
      request.params().map(entry => entry.getKey -> entry.getValue).toMap)
    val parameters = result._3
    val context = new EZRPCContext()
    context.remoteIP = ip
    context.method = request.method().name()
    context.templateUri = result._4
    context.realUri = request.path()
    context.parameters = parameters.map { i => i._1 -> URLDecoder.decode(i._2, "UTF-8") }
    context.accept = accept.toLowerCase()
    context.contentType = contentType.toLowerCase()
    if (result._1) {
      execute(request, result._2, context)
    } else {
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
              returnContent(Resp.serverError(e.getMessage), request.response(), context.accept, context.contentType)
            }
          })
          upload.endHandler(new Handler[Void] {
            override def handle(e: Void): Unit = {
              context.contentType = "application/json; charset=UTF-8"
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
    EZAsyncInterceptorProcessor.process[EZRPCContext](HttpInterceptor.category, context).onSuccess {
      case interResp =>
        if (interResp) {
          val newContext = interResp.body._1
          try {
            val b = if (body != null) {
              newContext.contentType match {
                case t if t.contains("json") => JsonHelper.toObject(body, fun.requestClass)
                case _ => logger.error("Not support content type:" + newContext.contentType)
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
              logger.error("Execute function error.", e)
              returnContent(Resp.serverError(e.getMessage), response, newContext.accept, newContext.contentType)
          }
        } else {
          returnContent(interResp, request.response(), context.accept, context.contentType)
        }
    }
  }

  private def returnContent(result: Any, response: HttpServerResponse, accept: String, contentType: String): Unit = {
    result match {
      case value: Resp[_] if value && value.body.isInstanceOf[File] =>
        val file = value.body.asInstanceOf[File]
        response.setStatusCode(200)
          .putHeader("Content-disposition", "attachment; filename=" + file.getName)
        response.sendFile(file.getPath)
      case value: Resp[_] if value && value.body.isInstanceOf[RespRedirect] =>
        response.putHeader("Location", value.body.asInstanceOf[RespRedirect].url)
          .setStatusCode(302)
          .end()
      case _ =>
        // 支持CORS
        val res = result match {
          case r: String => r
          case _ => JsonHelper.toJsonString(result)
        }
        logger.trace("Response: \r\n" + res)
        response.setStatusCode(200).putHeader("Content-Type", accept)
          .putHeader("Cache-Control", "no-cache")
          .putHeader("Access-Control-Allow-Origin", accessControlAllowOrigin)
          .putHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
          .putHeader("Access-Control-Allow-Headers", "Content-Type, X-Requested-With, X-authentication, X-client")
          .end(res)
    }
  }

}
