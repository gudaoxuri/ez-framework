package com.ecfront.ez.framework.service.gateway

import java.util.concurrent.CountDownLatch

import com.ecfront.common.Resp
import com.ecfront.ez.framework.core.interceptor.EZAsyncInterceptorProcessor
import com.ecfront.ez.framework.core.{EZ, EZServiceAdapter}
import com.ecfront.ez.framework.service.gateway.helper.AsyncRedisProcessor
import com.ecfront.ez.framework.service.gateway.interceptor.{AntiDDoSInterceptor, AuthInterceptor, GatewayInterceptor, SlowMonitorInterceptor}
import com.ecfront.ez.framework.service.gateway.metrics.{DefaultMetricsProcessor, MetricsProcessor}
import com.fasterxml.jackson.databind.JsonNode
import io.vertx.core.http.{HttpServer, HttpServerOptions}
import io.vertx.core.net.JksOptions
import io.vertx.core.{AsyncResult, Handler, Vertx, VertxOptions}
import io.vertx.ext.dropwizard.DropwizardMetricsOptions

import scala.collection.JavaConversions._

object ServiceAdapter extends EZServiceAdapter[JsonNode] {

  private val DEFAULT_HTTP_PORT: Integer = 80
  private val DEFAULT_WS_PORT: Integer = 81
  private val DEFAULT_HTTPS_PORT: Integer = 443
  private val DEFAULT_SLOW_TIME: Long = 10000L

  var vertx: Vertx = _
  var metrics: MetricsProcessor = _
  var resourcePath: String = _
  var publicUrl: String = _

  override def init(config: JsonNode): Resp[String] = {
    vertx = initVertx(EZ.Info.config.ez.perf.toMap, EZ.Info.config.ez.isDebug)
    val publicUriPrefix = config.path("publicUriPrefix").asText("/public/")
    val useSSL = config.has("ssl")
    val port = config.path("port").asInt(if (useSSL) DEFAULT_HTTPS_PORT else DEFAULT_HTTP_PORT)
    val wsPort = config.path("wsPort").asInt(DEFAULT_WS_PORT)
    val host = config.path("host").asText("127.0.0.1")
    resourcePath = config.path("resourcePath").asText("/tmp/")
    publicUrl = config.path("publicUrl").asText(s"http${if (useSSL) "s" else ""}://" + host + ":" + port + "/")
    val opt = new HttpServerOptions()
    if (useSSL) {
      var keyPath = config.path("ssl").path("keyPath").asText()
      if (!keyPath.startsWith("/")) {
        keyPath = EZ.Info.confPath + keyPath
      }
      opt.setSsl(true).setKeyStoreOptions(
        new JksOptions().setPath(keyPath)
          .setPassword(config.path("ssl").path("keyPassword").asText())
      )
    }
    opt.setTcpKeepAlive(true).setReuseAddress(true).setCompressionSupported(true)
    if (config.has("monitor")) {
      val monitor = config.path("monitor")
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

    val cache = config.get("cache")
    val address = cache.get("address").asText().split(";")
    val db = cache.path("db").asInt(0)
    val auth = cache.path("auth").asText("")
    AsyncRedisProcessor.init(vertx, address.toList, db, auth)

    if (config.has("antiDDoS")) {
      val antiDDoS = config.get("antiDDoS")
      val reqRatePerMinute = antiDDoS.get("reqRatePerMinute").asInt()
      val illegalReqRatePerMinute = antiDDoS.get("illegalReqRatePerMinute").asInt()
      AntiDDoSInterceptor.init(reqRatePerMinute, illegalReqRatePerMinute)
    }

    val c = new CountDownLatch(2)
    val httpServer = vertx.createHttpServer(opt)
    httpServer.requestHandler(new HttpServerProcessor(resourcePath, config.path("accessControlAllowOrigin").asText("*")))
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
    val wsServer = vertx.createHttpServer(opt)
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
    initMetrics(vertx)
    if (config.has("metrics")) {
      val intervalSec = config.get("metrics").path("intervalSec").asInt(60 * 60)
      metrics.statistics(intervalSec, httpServer)
      metrics.statistics(intervalSec, wsServer)
    }
    c.await()
    Resp.success(null)
  }

  override def destroy(parameter: JsonNode): Resp[String] = {
    AsyncRedisProcessor.close()
    Resp.success("")
  }

  System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.SLF4JLogDelegateFactory")
  System.setProperty("vertx.disableFileCaching", "true")
  System.setProperty("vertx.disableFileCPResolving", "true")
  System.setProperty("hazelcast.logging.type", "slf4j")

  private val FLAG_PERF_EVENT_LOOP_POOL_SIZE = "eventLoopPoolSize"
  private val FLAG_PERF_WORKER_POOL_SIZE = "workerPoolSize"
  private val FLAG_PERF_INTERNAL_BLOCKING_POOL_SIZE = "internalBlockingPoolSize"
  private val FLAG_PERF_MAX_EVENT_LOOP_EXECUTE_TIME = "maxEventLoopExecuteTime"
  private val FLAG_PERF_WORKER_EXECUTE_TIME = "maxWorkerExecuteTime"
  private val FLAG_PERF_WARNING_EXCEPTION_TIME = "warningExceptionTime"


  /**
    * 初始Vertx
    *
    * @return vertx实例
    */
  private def initVertx(perf: Map[String, Any], isDebug: Boolean): Vertx = {
    val opt = new VertxOptions()
    if (perf.contains(FLAG_PERF_EVENT_LOOP_POOL_SIZE)) {
      opt.setEventLoopPoolSize(perf(FLAG_PERF_EVENT_LOOP_POOL_SIZE).asInstanceOf[Int])
    } else {
      opt.setEventLoopPoolSize(20)
    }
    if (perf.contains(FLAG_PERF_WORKER_POOL_SIZE)) {
      opt.setWorkerPoolSize(perf(FLAG_PERF_WORKER_POOL_SIZE).asInstanceOf[Int])
    } else {
      opt.setWorkerPoolSize(200)
    }
    if (perf.contains(FLAG_PERF_INTERNAL_BLOCKING_POOL_SIZE)) {
      opt.setInternalBlockingPoolSize(perf(FLAG_PERF_INTERNAL_BLOCKING_POOL_SIZE).asInstanceOf[Int])
    } else {
      opt.setInternalBlockingPoolSize(200)
    }
    if (perf.contains(FLAG_PERF_MAX_EVENT_LOOP_EXECUTE_TIME)) {
      opt.setMaxEventLoopExecuteTime(perf(FLAG_PERF_MAX_EVENT_LOOP_EXECUTE_TIME).asInstanceOf[Int] * 1000000L)
    }
    if (perf.contains(FLAG_PERF_WORKER_EXECUTE_TIME)) {
      opt.setMaxWorkerExecuteTime(perf(FLAG_PERF_WORKER_EXECUTE_TIME).asInstanceOf[Int] * 1000000L)
    }
    if (perf.contains(FLAG_PERF_WARNING_EXCEPTION_TIME)) {
      opt.setWarningExceptionTime(perf(FLAG_PERF_WARNING_EXCEPTION_TIME).asInstanceOf[Int] * 1000000L)
    }
    if (isDebug) opt.setWarningExceptionTime(600L * 1000 * 1000000)
    opt.setMetricsOptions(new DropwizardMetricsOptions().setEnabled(true))
    Vertx.vertx(opt)
  }

  private def initMetrics(vertx: Vertx): Resp[Void] = {
    metrics = DefaultMetricsProcessor
    metrics.register(vertx)
  }

  override var serviceName: String = "gateway"

}


