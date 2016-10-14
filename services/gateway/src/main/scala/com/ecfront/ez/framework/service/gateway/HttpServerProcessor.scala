package com.ecfront.ez.framework.service.gateway

import java.io.File
import java.net.URLEncoder
import java.nio.file.Files

import com.ecfront.common.{JsonHelper, Resp}
import com.ecfront.ez.framework.core.helper.FileType
import com.ecfront.ez.framework.core.rpc._
import com.ecfront.ez.framework.service.gateway.interceptor.EZAPIContext
import io.vertx.core.Handler
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.{HttpServerFileUpload, HttpServerRequest, HttpServerResponse}

import scala.collection.JavaConversions._

/**
  * HTTP 服务操作
  *
  * @param resourcePath             路径根路径
  * @param accessControlAllowOrigin 允许跨域的域名
  */
class HttpServerProcessor(resourcePath: String, accessControlAllowOrigin: String)
  extends Handler[HttpServerRequest] with GatewayProcessor {

  override def handle(request: HttpServerRequest): Unit = {
    if (request.method().name() == "OPTIONS") {
      returnContent(request.response(), "text/html", "")
    } else if (request.path() != "/favicon.ico") {
      val ip =
        if (request.headers().contains(FLAG_PROXY) && request.getHeader(FLAG_PROXY).nonEmpty) {
          request.getHeader(FLAG_PROXY)
        } else {
          request.remoteAddress().host()
        }
      logger.trace(s"Receive a request [${request.method().name()}][${request.uri()}] , from $ip ")
      try {
        router(request, ip)
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
    val result = LocalCacheContainer.getRouter(request.method().name(), request.path(),
      request.params().map(entry => entry.getKey -> entry.getValue).toMap, ip)
    val context = new EZAPIContext()
    context.remoteIP = ip
    context.method = request.method().name()
    context.templateUri = result._3
    context.realUri = request.uri()
    context.parameters = result._2
    context.accept = accept.toLowerCase()
    context.contentType = contentType.toLowerCase()
    if (result._1) {
      execute(request, context)
    } else {
      returnContent(result._1, request, context.accept, context.contentType)
    }
  }

  private def execute(request: HttpServerRequest, context: EZAPIContext): Unit = {
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
              returnContent(Resp.serverError(e.getMessage), request, context.accept, context.contentType)
            }
          })
          upload.endHandler(new Handler[Void] {
            override def handle(e: Void): Unit = {
              context.contentType = "application/json; charset=utf-8"
              execute(request, newName, context)
            }
          })
          upload.streamToFileSystem(tPath)
        }
      })
    } else if (request.method().name() == "POST" || request.method().name() == "PUT") {
      // Post或Put请求，需要处理Body
      request.bodyHandler(new Handler[Buffer] {
        override def handle(data: Buffer): Unit = {
          execute(request, data.getString(0, data.length), context)
        }
      })
    } else {
      // Get或Delete请求
      execute(request, null, context)
    }
  }

  private def execute(request: HttpServerRequest, body: String, context: EZAPIContext): Unit = {
    execute(body, context, {
      resp =>
        if (resp) {
          val context = resp.body._1
          returnContent(context.executeResult, request, context.accept, context.contentType)
        } else {
          returnContent(resp, request, context.accept, context.contentType)
        }
    })
  }

  private def returnContent(result: Any, request: HttpServerRequest, accept: String, contentType: String): Unit = {
    val response = request.response()
    try {
      result match {
        case r: Resp[_] if r && r.body.isInstanceOf[DownloadFile] =>
          val fileInfo = r.body.asInstanceOf[DownloadFile]
          val fileType = Files.probeContentType(new File(fileInfo.file).toPath)
          if (FileType.TYPE_IMAGE.contains(fileType)) {
            // 显示图片
            response.putHeader("Content-Transfer-Encoding", "binary")
          } else {
            // 资源下载
            val ua = if (request.headers().contains("User-Agent")) request.getHeader("User-Agent") else ""
            ua match {
              case u if u.toLowerCase().contains("firefox") =>
                response.putHeader("Content-disposition", "attachment; filename*=UTF-8''" + URLEncoder.encode(fileInfo.fileName, "UTF-8"))
              case _ =>
                response.putHeader("Content-disposition", "attachment; filename=" + URLEncoder.encode(fileInfo.fileName, "UTF-8"))
            }
          }
          response.setStatusCode(HTTP_STATUS_200)
          response.sendFile(fileInfo.file)
        case r: Resp[_] if r && r.body.isInstanceOf[RespRedirect] =>
          // 重定向
          response.putHeader("Location", r.body.asInstanceOf[RespRedirect].url)
            .setStatusCode(HTTP_STATUS_302)
            .end()
        case r: Resp[_] if r && r.body.isInstanceOf[Raw] =>
          returnContent(response, accept, r.body.asInstanceOf[Raw].raw.toString)
        case r: Resp[_] =>
          // gateway 本身生成的对象，如认证失败
          returnContent(response, accept, JsonHelper.toJsonString(r))
        case _ =>
          val ret = contentType match {
            case t if t.contains("xml") =>
              // Xml
              val resp = JsonHelper.toObject[Resp[String]](result.asInstanceOf[String])
              if (resp) {
                if (resp.body == null) {
                  ""
                } else {
                  resp.body
                }
              } else {
                s"""<?xml version="1.0" encoding="UTF-8"?>
                    |<xml>
                    | <error>
                    |  <code>${resp.code}</code>
                    |  <message>${resp.message}</message>
                    | </error>
                    |</xml>
                 """.stripMargin
              }
            case _ =>
              // Json
              result.asInstanceOf[String]
          }
          returnContent(response, accept, ret)
      }
    } catch {
      case e: Throwable =>
        logger.error("Http process error.", e)
        returnContent(request.response(), "text/html", s"Request process error：${e.getMessage}")
    }
  }

  private def returnContent(response: HttpServerResponse, accept: String, res: String): Unit = {
    logger.trace("Response: \r\n" + RPCProcessor.cutPrintShow(res))
    // 支持CORS
    response.setStatusCode(HTTP_STATUS_200).putHeader("Content-Type", accept)
      .putHeader("Cache-Control", "no-cache")
      .putHeader("Access-Control-Allow-Origin", accessControlAllowOrigin)
      .putHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
      .putHeader("Access-Control-Allow-Headers", "Content-Type, X-Requested-With, X-authentication, X-client")
      .end(res)
  }

}
