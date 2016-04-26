package com.ecfront.ez.framework.service.rpc.http

import com.ecfront.common.Resp
import com.ecfront.ez.framework.core.interceptor.EZAsyncInterceptorProcessor
import com.ecfront.ez.framework.core.{EZContext, EZServiceAdapter}
import com.ecfront.ez.framework.service.rpc.foundation.AutoBuildingProcessor
import com.ecfront.ez.framework.service.rpc.http.interceptor.SlowMonitorInterceptor
import io.vertx.core.http.{HttpServer, HttpServerOptions}
import io.vertx.core.json.{JsonArray, JsonObject}
import io.vertx.core.net.JksOptions
import io.vertx.core.{AsyncResult, Handler}

import scala.collection.JavaConversions._
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Promise}

object ServiceAdapter extends EZServiceAdapter[JsonObject] {

  private val DEFAULT_HTTP_PORT: Integer = 80
  private val DEFAULT_HTTPS_PORT: Integer = 443
  private val DEFAULT_SLOW_TIME: Long = 10000L

  var resourcePath: String = _
  var webUrl: String = _
  var publicUrl: String = _
  var servicePath: String = _

  override def init(parameter: JsonObject): Resp[String] = {
    val useSSL = parameter.containsKey("ssl")
    val port = parameter.getInteger("port", if (useSSL) DEFAULT_HTTPS_PORT else DEFAULT_HTTP_PORT)
    val host = parameter.getString("host", "127.0.0.1")
    resourcePath = parameter.getString("resourcePath", "/tmp/")
    publicUrl = parameter.getString("publicUrl", s"http${if (useSSL) "s" else ""}://" + host + ":" + port + "/")
    webUrl = parameter.getString("webUrl", publicUrl)
    servicePath = parameter.getString("servicePath", null)

    val opt = new HttpServerOptions()
    if (useSSL) {
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
      .listen(port, host, new Handler[AsyncResult[HttpServer]] {
        override def handle(event: AsyncResult[HttpServer]): Unit = {
          if (event.succeeded()) {
            p.success(Resp.success(
              s"""HTTP${if (useSSL) "s" else ""} start successful.
                  | http${if (useSSL) "s" else ""}://$host:$port/""".stripMargin))
          } else {
            logger.error(s"HTTP${if (useSSL) "s" else ""} start fail .", event.cause())
            p.success(Resp.serverError(s"HTTP${if (useSSL) "s" else ""} start fail : ${event.cause().getMessage}"))
          }
        }
      })
    if (parameter.containsKey("monitor")) {
      val monitor = parameter.getJsonObject("monitor")
      if (monitor.containsKey("slow")) {
        val slow = monitor.getJsonObject("slow")
        SlowMonitorInterceptor.init(
          slow.getLong("time", DEFAULT_SLOW_TIME),
          slow.getJsonArray("includes", new JsonArray()).map(_.asInstanceOf[String]).toSet,
          slow.getJsonArray("excludes", new JsonArray()).map(_.asInstanceOf[String]).toSet
        )
        EZAsyncInterceptorProcessor.register(HttpInterceptor.category, SlowMonitorInterceptor)
      }
    }

    HttpClientProcessor.init(EZContext.vertx)
    val serviceR = Await.result(p.future, Duration.Inf)
    serviceR
  }

  // 所有服务都初始化完成后调用
  override def initPost(): Unit = {
    if (servicePath != null) {
      AutoBuildingProcessor.autoBuilding[HTTP](servicePath, classOf[HTTP])
    }
  }

  override def destroy(parameter: JsonObject): Resp[String] = {
    Resp.success("")
  }

  override var serviceName: String = "rpc.http"

}


