package com.asto.ez.framework

import com.asto.ez.framework.auth.AuthHttpInterceptor
import com.asto.ez.framework.auth.manage.Initiator
import com.asto.ez.framework.cache.RedisProcessor
import com.asto.ez.framework.interceptor.InterceptorProcessor
import com.asto.ez.framework.mail.MailProcessor
import com.asto.ez.framework.rpc.AutoBuildingProcessor
import com.asto.ez.framework.rpc.http.{HttpClientProcessor, HttpInterceptor, HttpServerProcessor}
import com.asto.ez.framework.rpc.websocket.WebSocketServerProcessor
import com.asto.ez.framework.scheduler.SchedulerService
import com.asto.ez.framework.storage.jdbc.DBProcessor
import com.asto.ez.framework.storage.mongo.MongoProcessor
import com.typesafe.scalalogging.slf4j.LazyLogging
import io.vertx.core._
import io.vertx.core.http.{HttpServer, HttpServerOptions}
import io.vertx.core.json.JsonObject
import io.vertx.ext.jdbc.JDBCClient
import io.vertx.ext.mail.MailConfig
import io.vertx.ext.mongo.MongoClient

import scala.io.Source

/**
  * 系统启动类
  */
abstract class EZStartup extends AbstractVerticle with LazyLogging {

  protected def module: String

  protected def initiator: EZInitiator = null

  protected def preStartup() = {}

  protected def postStartup() = {}

  protected def shutdown() = {}

  /**
    * 启动入口
    */
  override def start(): Unit = {
    System.setProperty("vertx.disableFileCaching", "true")
    System.setProperty("vertx.disableFileCPResolving", "true")
    System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.SLF4JLogDelegateFactory")
    EZGlobal.vertx = if (vertx != null) vertx else Vertx.vertx()
    EZGlobal.config = if (vertx != null) config() else new JsonObject(Source.fromFile(this.getClass.getResource("/").getPath + "config.json").mkString)
    HttpClientProcessor.httpClient = EZGlobal.vertx.createHttpClient()
    preStartup()
    startEZService()
    if (initiator != null && initiator.needInitialization) initiator.initialize()
    postStartup()
  }

  private def startEZService(): Unit = {
    startRPCServer()
    startStorageConnection()
    startCacheConnection()
    startAuth()
    startMailClient()
    startScheduler()
  }

  private def startRPCServer(): Unit = {
    if (EZGlobal.ez_rpc != null) {
      val servicePath = EZGlobal.ez_rpc.getString("servicePath")
      if (servicePath.nonEmpty) {
        new Thread(new Runnable {
          override def run(): Unit = {
            AutoBuildingProcessor.autoBuilding(servicePath)
          }
        }).start()
      }
      if (EZGlobal.ez_rpc.containsKey("http")) {
        val http = EZGlobal.ez_rpc.getJsonObject("http")
        EZGlobal.vertx.createHttpServer(new HttpServerOptions().setCompressionSupported(true).setTcpKeepAlive(true))
          //注册了自定义路由器：HttpRouter
          .requestHandler(new HttpServerProcessor).websocketHandler(new WebSocketServerProcessor).listen(http.getInteger("port"), http.getString("host"), new Handler[AsyncResult[HttpServer]] {
          override def handle(event: AsyncResult[HttpServer]): Unit = {
            if (event.succeeded()) {
              logger.info(s"EZ Framework HTTP start successful. http://${http.getString("host")}:${http.getInteger("port")}/")
            } else {
              logger.error("EZ Framework HTTP start fail .", event.cause())
            }
          }
        })
      }
      //TODO cluster
    }
  }

  private def startStorageConnection(): Unit = {
    if (EZGlobal.ez_storage != null) {
      val entityPath = EZGlobal.ez_storage.getString("entityPath")
      if (entityPath.nonEmpty) {
        new Thread(new Runnable {
          override def run(): Unit = {
            //TODO
          }
        }).start()
      }
      if (EZGlobal.ez_storage.containsKey("jdbc")) {
        val jdbc = EZGlobal.ez_storage.getJsonObject("jdbc")
        DBProcessor.dbClient = JDBCClient.createShared(EZGlobal.vertx, jdbc)
        logger.info(s"EZ Framework JDBC connected. ${jdbc.getString("url")}")
      }
      if (EZGlobal.ez_storage.containsKey("mongo")) {
        val mongo = EZGlobal.ez_storage.getJsonObject("mongo")
        MongoProcessor.mongoClient = MongoClient.createShared(EZGlobal.vertx, mongo)
        logger.info(s"EZ Framework Mongo connected. ${mongo.getJsonArray("hosts")}")
      }
    }
  }

  private def startCacheConnection(): Unit = {
    if (EZGlobal.ez_cache != null) {
      if (EZGlobal.ez_cache.containsKey("redis")) {
        val redis = EZGlobal.ez_cache.getJsonObject("redis")
        RedisProcessor.init(
          EZGlobal.vertx, redis.getString("host"),
          redis.getInteger("port"),
          redis.getInteger("db"),
          redis.getString("auth"),
          EZGlobal.ez_cache.getBoolean("useCache")
        )
      }
    }
  }

  private def startAuth(): Unit = {
    if (EZGlobal.ez_auth != null) {
      if (EZGlobal.ez_auth.getBoolean("useAuth")) {
        InterceptorProcessor.register(HttpInterceptor.category, AuthHttpInterceptor)
        new Thread(new Runnable {
          override def run(): Unit = {
            AutoBuildingProcessor.autoBuilding("com.asto.ez.framework.auth")
            Initiator.init()
          }
        }).start()
      }
    }
  }

  private def startMailClient(): Unit = {
    if (EZGlobal.ez_mail != null) {
      MailProcessor.init(new MailConfig(EZGlobal.ez_mail))
      logger.info(s"EZ Framework Mail client initialized. ${EZGlobal.ez.getJsonObject("mail").getString("hostname")}")
    }
  }

  private def startScheduler(): Unit = {
    if (EZGlobal.ez_scheduler != null) {
      if (EZGlobal.ez_scheduler) {
        new Thread(new Runnable {
          override def run(): Unit = {
            if (EZGlobal.ez_storage.containsKey("jdbc")) {
              SchedulerService.init(module, useMongo = false)
            } else if (EZGlobal.ez_storage.containsKey("mongo")) {
              SchedulerService.init(module, useMongo = true)
            }
          }
        }).start()
      }
    }
  }

  /**
    * 关闭操作
    */
  override def stop(): Unit = {
    SchedulerService.shutdown()
    shutdown()
    logger.info(s"EZ Framework Stopped , Bye .")
  }

}

