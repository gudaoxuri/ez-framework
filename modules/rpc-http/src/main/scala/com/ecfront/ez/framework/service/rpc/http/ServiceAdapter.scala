package com.ecfront.ez.framework.service.rpc.http

import com.ecfront.common.Resp
import com.ecfront.ez.framework.core.{EZContext, EZServiceAdapter}
import com.ecfront.ez.framework.service.rpc.foundation.AutoBuildingProcessor
import io.vertx.core.http.{HttpClientOptions, HttpServer, HttpServerOptions}
import io.vertx.core.json.JsonObject
import io.vertx.core.net.JksOptions
import io.vertx.core.{AsyncResult, Handler}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Promise}

object ServiceAdapter extends EZServiceAdapter[JsonObject] {

  var resourcePath: String = ""
  var webUrl: String = ""
  var publicUrl: String = ""

  override def init(parameter: JsonObject): Resp[String] = {
    resourcePath = parameter.getString("resourcePath", "/tmp/")
    publicUrl = parameter.getString("publicUrl", "http://" + parameter.getString("host") + ":" + parameter.getInteger("port") + "/")
    webUrl = parameter.getString("webUrl", publicUrl)
    val servicePath = parameter.getString("servicePath")
    AutoBuildingProcessor.autoBuilding[HTTP](servicePath, classOf[HTTP])
    val opt = new HttpServerOptions()
    if (parameter.containsKey("ssl")) {
      var keyPath = parameter.getJsonObject("ssl").getString("keyPath")
      if (!keyPath.startsWith("/")) {
        keyPath = EZContext.confPath + keyPath
      }
      opt.setSsl(true).setKeyStoreOptions(
        new JksOptions().setPath(keyPath)
          .setPassword(parameter.getJsonObject("ssl").getString("keyPassword"))
      )
    }
    val p = Promise[Resp[String]]()
    EZContext.vertx
      .createHttpServer(opt.setCompressionSupported(true)
        .setTcpKeepAlive(true))
      .requestHandler(new HttpServerProcessor(resourcePath, parameter.getString("accessControlAllowOrigin", "*")))
      .listen(parameter.getInteger("port"), parameter.getString("host"), new Handler[AsyncResult[HttpServer]] {
        override def handle(event: AsyncResult[HttpServer]): Unit = {
          if (event.succeeded()) {
            p.success(Resp.success(
              s"""HTTP${if (parameter.containsKey("ssl")) "s" else ""} start successful.
                  | http${if (parameter.containsKey("ssl")) "s" else ""}://${parameter.getString("host")}:${parameter.getInteger("port")}/""".stripMargin))
          } else {
            logger.error(s"HTTP${if (parameter.containsKey("ssl")) "s" else ""} start fail .", event.cause())
            p.success(Resp.serverError(s"HTTP${if (parameter.containsKey("ssl")) "s" else ""} start fail : ${event.cause().getMessage}"))
          }
        }
      })
    HttpClientProcessor.httpClient = EZContext.vertx.createHttpClient()
    HttpClientProcessor.httpClients = EZContext.vertx.createHttpClient(new HttpClientOptions().setSsl(true).setVerifyHost(false).setTrustAll(true))
    val serviceR = Await.result(p.future, Duration.Inf)
    serviceR
  }

  override def destroy(parameter: JsonObject): Resp[String] = {
    Resp.success("")
  }

  override var serviceName: String = "rpc.http"

}


