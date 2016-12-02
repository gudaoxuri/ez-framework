package com.ecfront.ez.framework.service.gateway.helper

import java.util.concurrent.CountDownLatch

import com.ecfront.common.Resp
import com.ecfront.ez.framework.core.logger.Logging
import io.vertx.core.{AsyncResult, Handler, Vertx}
import io.vertx.redis.{RedisClient, RedisOptions}


object AsyncRedisProcessor extends Logging {

  private var simpleRedis: RedisClient = _

  /**
    * 初始化
    *
    * @param address redis 地址
    * @param db      redids db名
    * @param auth    密码
    * @return 结果
    */
  def init(vertx: Vertx, address: List[String], db: Int, auth: String = ""): Resp[String] = {
    val c = new CountDownLatch(1)
    var resp: Resp[String] = null
    val addr = address.map(_.split(":"))
    val config = new RedisOptions()
    addr.foreach{
      ad =>
        // TODO 多地址支持
        config.setHost(ad(0))
        config.setPort(ad(1).toInt)
    }
    simpleRedis = RedisClient.create(vertx, config)
    if (auth != null && auth.nonEmpty) {
      simpleRedis.auth(auth, new Handler[AsyncResult[String]] {
        override def handle(event: AsyncResult[String]): Unit = {
          if (event.succeeded()) {
            simpleRedis.select(db, new Handler[AsyncResult[String]] {
              override def handle(event: AsyncResult[String]): Unit = {
                if (event.succeeded()) {
                  resp = Resp.success("Redis client started")
                  c.countDown()
                } else {
                  logger.error("Redis client start fail.", event.cause())
                  resp = Resp.serverError(event.cause().getMessage)
                  c.countDown()
                }
              }
            })
          } else {
            logger.error("Redis client start fail.", event.cause())
            resp = Resp.serverError(event.cause().getMessage)
            c.countDown()
          }
        }
      })
    }else{
      simpleRedis.select(db, new Handler[AsyncResult[String]] {
        override def handle(event: AsyncResult[String]): Unit = {
          if (event.succeeded()) {
            resp = Resp.success("Redis client started")
            c.countDown()
          } else {
            logger.error("Redis client start fail.", event.cause())
            resp = Resp.serverError(event.cause().getMessage)
            c.countDown()
          }
        }
      })
    }
    c.await()
    resp
  }

  def close(): Unit = {
    if (simpleRedis != null) {
      simpleRedis.close(new Handler[AsyncResult[Void]] {
        override def handle(event: AsyncResult[Void]): Unit = {
        }
      })
    }
  }

  /**
    * 暴露redis client ，用于自定义操作
    *
    * @return redis client
    */
  def client(): RedisClient = simpleRedis

}
