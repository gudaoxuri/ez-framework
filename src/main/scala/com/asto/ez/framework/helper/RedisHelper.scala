package com.asto.ez.framework.helper

import java.lang

import com.ecfront.common.Resp
import com.typesafe.scalalogging.slf4j.LazyLogging
import io.vertx.core.{AsyncResult, Handler, Vertx}
import io.vertx.redis.op.SetOptions
import io.vertx.redis.{RedisClient, RedisOptions}
import scala.async.Async.{async, await}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}

/**
  * Redis 异步操作辅助类
  */
object RedisHelper extends LazyLogging {

  private var redisClient: RedisClient = _
  private var useCache = true

  def init(vertx: Vertx, host: String, port: Int, db: Integer, auth: String = null, _useCache: Boolean = true): Unit = {
    useCache = _useCache
    if (useCache) {
      redisClient = RedisClient.create(vertx, new RedisOptions().setHost(host).setPort(port))
      if (auth != null && auth.nonEmpty) {
        redisClient.auth(auth, new Handler[AsyncResult[String]] {
          override def handle(event: AsyncResult[String]): Unit = {
            if (event.succeeded()) {
              redisClient.select(db, new Handler[AsyncResult[String]] {
                override def handle(event: AsyncResult[String]): Unit = {
                  if (event.succeeded()) {
                    logger.info(s"DOP core app redis connected $host:$port")
                  } else {
                    logger.error("Redis connection error.", event.cause())
                  }
                }
              })
            } else {
              logger.error("Redis connection error.", event.cause())
            }
          }
        })
      }else{
        redisClient.select(db, new Handler[AsyncResult[String]] {
          override def handle(event: AsyncResult[String]): Unit = {
            if (event.succeeded()) {
              logger.info(s"DOP core app redis connected $host:$port")
            } else {
              logger.error("Redis connection error.", event.cause())
            }
          }
        })
      }
    }
  }

  /**
    * @param expire (seconds)
    * @return
    */
  def set(key: String, value: String, expire: Long = 0): Future[Resp[Void]] = async {
    val p = Promise[Resp[Void]]()
    if (useCache) {
      if (expire == 0) {
        redisClient.set(key, value, new Handler[AsyncResult[Void]] {
          override def handle(event: AsyncResult[Void]): Unit = {
            if (event.succeeded()) {
              p.success(Resp.success(null))
            } else {
              logger.error(s"Redis set error.[$key] $value", event.cause())
              p.success(Resp.serverUnavailable(s"Redis set error.[$key] $value"))
            }
          }
        })
      } else {
        redisClient.setWithOptions(key, value, new SetOptions().setEX(expire), new Handler[AsyncResult[Void]] {
          override def handle(event: AsyncResult[Void]): Unit = {
            if (event.succeeded()) {
              p.success(Resp.success(null))
            } else {
              logger.error(s"Redis set error.[$key] $value", event.cause())
              p.success(Resp.serverUnavailable(s"Redis set error.[$key] $value"))
            }
          }
        })
      }
    } else {
      p.success(Resp.success(null))
    }
    await(p.future)
  }

  def exists(key: String): Future[Resp[Boolean]] = async{
    val p = Promise[Resp[Boolean]]()
    if (useCache) {
      redisClient.exists(key, new Handler[AsyncResult[lang.Long]] {
        override def handle(event: AsyncResult[lang.Long]): Unit = {
          if (event.succeeded()) {
            if (event.result() > 0) {
              p.success(Resp.success(true))
            } else {
              p.success(Resp.success(false))
            }
          } else {
            logger.error(s"Redis exists error.[$key]", event.cause())
            p.success(Resp.serverUnavailable(s"Redis exists error.[$key]"))
          }
        }
      })
    } else {
      p.success(Resp.success(false))
    }
    await(p.future)
  }

  def get(key: String): Future[Resp[String]] = async {
    val p = Promise[Resp[String]]()
    if (useCache) {
      redisClient.get(key, new Handler[AsyncResult[String]] {
        override def handle(event: AsyncResult[String]): Unit = {
          if (event.succeeded()) {
            p.success(Resp.success(event.result()))
          } else {
            logger.error(s"Redis get error.[$key]", event.cause())
            p.success(Resp.serverUnavailable(s"Redis get error.[$key]"))
          }
        }
      })
    } else {
      p.success(Resp.success(null))
    }
    await(p.future)
  }

}
