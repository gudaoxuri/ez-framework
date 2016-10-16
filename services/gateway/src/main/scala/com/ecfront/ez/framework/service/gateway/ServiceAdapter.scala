package com.ecfront.ez.framework.service.gateway

import java.util.concurrent.CountDownLatch

import com.ecfront.common.Resp
import com.ecfront.ez.framework.core.interceptor.EZAsyncInterceptorProcessor
import com.ecfront.ez.framework.core.{EZ, EZServiceAdapter}
import com.ecfront.ez.framework.service.gateway.helper.AsyncRedisProcessor
import com.ecfront.ez.framework.service.gateway.interceptor.{AuthInterceptor, GatewayInterceptor, SlowMonitorInterceptor}
import com.fasterxml.jackson.databind.JsonNode
import io.vertx.core.http.{HttpServer, HttpServerOptions}
import io.vertx.core.net.JksOptions
import io.vertx.core.{AsyncResult, Handler}

import scala.collection.JavaConversions._

object ServiceAdapter extends EZServiceAdapter[JsonNode] {

  private val DEFAULT_HTTP_PORT: Integer = 80
  private val DEFAULT_WS_PORT: Integer = 81
  private val DEFAULT_HTTPS_PORT: Integer = 443
  private val DEFAULT_SLOW_TIME: Long = 10000L

  var resourcePath: String = _
  var publicUrl: String = _

  override def init(parameter: JsonNode): Resp[String] = {
    val publicUriPrefix = parameter.path("publicUriPrefix").asText("/public/")
    val useSSL = parameter.has("ssl")
    val port = parameter.path("port").asInt(if (useSSL) DEFAULT_HTTPS_PORT else DEFAULT_HTTP_PORT)
    val wsPort = parameter.path("wsPort").asInt(DEFAULT_WS_PORT)
    val host = parameter.path("host").asText("127.0.0.1")
    resourcePath = parameter.path("resourcePath").asText("/tmp/")
    publicUrl = parameter.path("publicUrl").asText(s"http${if (useSSL) "s" else ""}://" + host + ":" + port + "/")
    val opt = new HttpServerOptions()
    if (useSSL) {
      var keyPath = parameter.path("ssl").path("keyPath").asText()
      if (!keyPath.startsWith("/")) {
        keyPath = EZ.Info.confPath + keyPath
      }
      opt.setSsl(true).setKeyStoreOptions(
        new JksOptions().setPath(keyPath)
          .setPassword(parameter.path("ssl").path("keyPassword").asText())
      )
    }
    opt.setTcpKeepAlive(true).setReuseAddress(true).setCompressionSupported(true)
    if (parameter.has("monitor")) {
      val monitor = parameter.path("monitor")
      if (monitor.has("slow")) {
        val slow = monitor.path("slow")
        SlowMonitorInterceptor.init(
          slow.path("time").asLong(DEFAULT_SLOW_TIME),
          if (slow.has("includes")) slow.path("includes").map(_.asInstanceOf[String]).toSet else Set(),
          if (slow.has("excludes")) slow.path("excludes").map(_.asInstanceOf[String]).toSet else Set()
        )
        EZAsyncInterceptorProcessor.register(GatewayInterceptor.category, SlowMonitorInterceptor)
      }
    }
    AuthInterceptor.init(publicUriPrefix)
    EZAsyncInterceptorProcessor.register(GatewayInterceptor.category, AuthInterceptor)

    val address = EZ.Info.config.ez.cache("address").asInstanceOf[String].split(";")
    val db = EZ.Info.config.ez.cache.getOrElse("db", 0).asInstanceOf[Int]
    val auth = EZ.Info.config.ez.cache.getOrElse("auth", "").asInstanceOf[String]
    AsyncRedisProcessor.init(EZ.vertx, address.toList, db, auth)

    val c = new CountDownLatch(2)
    val httpServer = EZ.vertx.createHttpServer(opt)
    httpServer.requestHandler(new HttpServerProcessor(resourcePath, parameter.path("accessControlAllowOrigin").asText("*")))
      .listen(port, host, new Handler[AsyncResult[HttpServer]] {
        override def handle(event: AsyncResult[HttpServer]): Unit = {
          if (event.succeeded()) {
            logger.info(
              s"""HTTP${if (useSSL) "s" else ""} start successful.
                  | http${if (useSSL) "s" else ""}://$host:$port/""".stripMargin)
            c.countDown()
          } else {
            logger.error(s"HTTP${if (useSSL) "s" else ""} start fail .", event.cause())
          }
        }
      })
    val wsServer = EZ.vertx.createHttpServer(opt)
    wsServer.websocketHandler(new WebSocketServerProcessor)
      .listen(wsPort, host, new Handler[AsyncResult[HttpServer]] {
        override def handle(event: AsyncResult[HttpServer]): Unit = {
          if (event.succeeded()) {
            logger.info(
              s"""WS start successful.
                  | ws://$host:$wsPort/""".stripMargin)
            c.countDown()
          } else {
            logger.error(s"WS start fail .", event.cause())
          }
        }
      })
    if (parameter.has("metrics")) {
      val intervalSec = parameter.get("metrics").path("intervalSec").asInt(60 * 60)
      EZ.metrics.statistics(intervalSec, httpServer)
      EZ.metrics.statistics(intervalSec, wsServer)
    }
    c.await()
    Resp.success(null)
  }

  override def destroy(parameter: JsonNode): Resp[String] = {
    AsyncRedisProcessor.close()
    Resp.success("")
  }

  override var serviceName: String = "gateway"

}


