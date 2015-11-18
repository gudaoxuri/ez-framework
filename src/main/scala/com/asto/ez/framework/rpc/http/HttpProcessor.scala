package com.asto.ez.framework.rpc.http

import java.io.File

import com.asto.ez.framework.{EZContext, EZGlobal}
import com.asto.ez.framework.helper.HttpClientHelper
import com.asto.ez.framework.rpc.{EChannel, Fun, Router}
import com.ecfront.common.{JsonHelper, Resp}
import com.typesafe.scalalogging.slf4j.LazyLogging
import io.vertx.core.Handler
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.{HttpServerFileUpload, HttpServerRequest, HttpServerResponse}

import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global

class HttpProcessor extends Handler[HttpServerRequest] with LazyLogging {

  override def handle(request: HttpServerRequest): Unit = {
    if (request.method().name() == "OPTIONS") {
      HttpClientHelper.returnContent("", request.response(), "text/html")
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
          HttpClientHelper.returnContent(s"请求处理错误：${ex.getMessage}", request.response(), "text/html")
      }
    }
  }

  private def router(request: HttpServerRequest, ip: String): Unit = {
    val accept =
      if (request.headers().contains("Accept") && request.headers().get("Accept") != "*.*") request.headers().get("Accept").split(",")(0).toLowerCase else "application/json"
    val contentType =
      if (request.headers().contains("Content-Type")) request.headers().get("Content-Type").toLowerCase else "application/json; charset=UTF-8"
    var parameters = request.params().map(entry => entry.getKey -> entry.getValue).toMap
    val result = Router.getFunction(EChannel.HTTP, request.method().name(), request.path(), parameters)
    parameters = result._3
    if (result._1) {
      if (request.headers().contains("Content-Type") && request.headers.get("Content-Type").toLowerCase.startsWith("multipart/form-data")) {
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
            val tPath = EZGlobal.resource_path + path + newName
            upload.exceptionHandler(new Handler[Throwable] {
              override def handle(e: Throwable): Unit = {
                HttpClientHelper.returnContent(Resp.serverError(e.getMessage), request.response(), accept)
              }
            })
            upload.endHandler(new Handler[Void] {
              override def handle(e: Void): Unit = {
                execute(request, parameters, path + newName, result._2, ip,request.response(), "application/json; charset=UTF-8", accept)
              }
            })
            upload.streamToFileSystem(tPath)
          }
        })
      } else if (request.method().name() == "POST" || request.method().name() == "PUT") {
        //Post或Put请求，需要处理Body
        request.bodyHandler(new Handler[Buffer] {
          override def handle(data: Buffer): Unit = {
            execute(request, parameters, data.getString(0, data.length), result._2,ip, request.response(), contentType, accept)
          }
        })
      } else {
        //Get或Delete请求
        execute(request, parameters, null, result._2,ip, request.response(), contentType, accept)
      }
    } else {
      HttpClientHelper.returnContent(result._1, request.response())
    }
  }

  private def execute(request: HttpServerRequest, parameters: Map[String, String], body: Any, fun: Fun[_], ip:String,response: HttpServerResponse, contentType: String, accept: String) {
    try {
      val b = if (body != null) {
        contentType match {
          case t if t.contains("json") => JsonHelper.toObject(body, fun.requestClass)
          case _ => logger.error("Not support content type:" + contentType)
        }
      } else {
        null
      }
      //TODO
      val context = EZContext()
      context.remoteIP = ip
      fun.execute(parameters, b, context).onSuccess {
        case excResp =>
          HttpClientHelper.returnContent(excResp, response, accept)
      }
    } catch {
      case e: Exception =>
        logger.error("Execute function error.", e)
        HttpClientHelper.returnContent(Resp.serverError(e.getMessage), response, accept)
    }
  }

}
