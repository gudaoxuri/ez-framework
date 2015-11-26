package com.asto.ez.framework

import com.asto.ez.framework.helper.{HttpClientHelper, RedisHelper}
import com.asto.ez.framework.rpc.AutoBuildingProcessor
import com.asto.ez.framework.rpc.http.HttpProcessor
import com.asto.ez.framework.rpc.websocket.WebSocketProcessor
import com.asto.ez.framework.service.scheduler.SchedulerService
import com.asto.ez.framework.storage.jdbc.DBHelper
import com.asto.ez.framework.storage.mongo.MongoHelper
import com.typesafe.scalalogging.slf4j.LazyLogging
import io.vertx.core._
import io.vertx.core.http.{HttpServer, HttpServerOptions}
import io.vertx.core.json.JsonObject
import io.vertx.ext.jdbc.JDBCClient
import io.vertx.ext.mongo.MongoClient

import scala.io.Source

/**
  * 系统启动类
  */
abstract class EZStartup extends AbstractVerticle with LazyLogging {

  protected  def module:String

  protected def preStartup() = {}

  protected def postStartup() = {}

  protected def shutdown() = {}

  /**
    * 启动入口
    */
  override def start(): Unit = {
    EZGlobal.vertx = if (vertx != null) vertx else Vertx.vertx()
    EZGlobal.config =if (vertx != null) config() else new JsonObject(Source.fromFile(this.getClass.getResource("/").getPath + "config.json").mkString)
    HttpClientHelper.httpClient = EZGlobal.vertx.createHttpClient()
    preStartup()
    buildServiceContainer()
    startHttpServer()
    startJDBCClient()
    startMongoClient()
    startRedisClient()
    startScheduler()
    postStartup()
  }

  def buildServiceContainer(): Unit = {
    val servicePath = EZGlobal.ez.getString("servicePath")
    val entityPath = EZGlobal.ez.getString("entityPath")
    if (servicePath.nonEmpty) {
      AutoBuildingProcessor.autoBuilding(servicePath)
    }
    //TODO
  }

  def startHttpServer(): Unit = {
    if (EZGlobal.ez.containsKey("publicServer")) {
      val server = EZGlobal.ez.getJsonObject("publicServer")
      EZGlobal.vertx.createHttpServer(new HttpServerOptions().setCompressionSupported(true).setTcpKeepAlive(true))
        //注册了自定义路由器：HttpRouter
        .requestHandler(new HttpProcessor).websocketHandler(new WebSocketProcessor).listen(server.getInteger("port"), server.getString("host"), new Handler[AsyncResult[HttpServer]] {
        override def handle(event: AsyncResult[HttpServer]): Unit = {
          if (event.succeeded()) {
            logger.info(s"EZ Framework HTTP start successful. http://${server.getString("host")}:${server.getInteger("port")}/")
          } else {
            logger.error("EZ Framework HTTP start fail .", event.cause())
          }
        }
      })
    }
  }


  def startJDBCClient(): Unit = {
    if (EZGlobal.ez.containsKey("jdbc")) {
      val jdbc = EZGlobal.ez.getJsonObject("jdbc")
      DBHelper.dbClient = JDBCClient.createShared(EZGlobal.vertx, jdbc)
      logger.info(s"EZ Framework JDBC connected. ${jdbc.getString("jdbc")}")
    }
  }

  def startMongoClient(): Unit = {
    if (EZGlobal.ez.containsKey("mongo")) {
      val mongo = EZGlobal.ez.getJsonObject("mongo")
      MongoHelper.mongoClient = MongoClient.createShared(EZGlobal.vertx, mongo)
      logger.info(s"EZ Framework Mongo connected.")
    }
  }

  def startRedisClient(): Unit = {
    if (EZGlobal.ez.containsKey("redis")) {
      val redis = EZGlobal.ez.getJsonObject("redis")
      RedisHelper.init(
        EZGlobal.vertx, redis.getString("host"),
        redis.getInteger("port"),
        redis.getInteger("db"),
        redis.getString("auth"),
        EZGlobal.ez.getBoolean("useCache")
      )
    }
  }

  def startScheduler(): Unit = {
    SchedulerService.init(module)
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

