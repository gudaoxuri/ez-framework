package com.asto.ez.framework.helper

import java.lang

import com.ecfront.common.{JsonHelper, Resp}
import com.typesafe.scalalogging.slf4j.LazyLogging
import io.vertx.core.json.JsonObject
import io.vertx.core.{AsyncResult, Handler, Vertx}
import io.vertx.redis.op.SetOptions
import io.vertx.redis.{RedisClient, RedisOptions}

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
      } else {
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

  def custom(): RedisClient = {
    if (useCache) {
      redisClient
    } else {
      null
    }
  }

  def exists(key: String): Future[Resp[Boolean]] = {
    val p = Promise[Resp[Boolean]]()
    if (useCache) {
      redisClient.exists(key, new Handler[AsyncResult[lang.Long]] {
        override def handle(event: AsyncResult[lang.Long]): Unit = {
          if (event.succeeded()) {
            p.success(Resp.success(event.result() > 0))
          } else {
            logger.error(s"Redis exists error.[$key]", event.cause())
            p.success(Resp.serverUnavailable(s"Redis exists error.[$key]"))
          }
        }
      })
    } else {
      p.success(Resp.notImplemented("Redis service not found."))
    }
    p.future
  }

  def get(key: String): Future[Resp[String]] = {
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
      p.success(Resp.notImplemented("Redis service not found."))
    }
    p.future
  }

  /**
    * @param expire (seconds)
    */
  def set(key: String, value: String, expire: Long = 0): Future[Resp[Void]] = {
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
      p.success(Resp.notImplemented("Redis service not found."))
    }
    p.future
  }

  def hmset(key: String, values: Map[String, String], expire: Long = 0): Future[Resp[Void]] = {
    val p = Promise[Resp[Void]]()
    if (useCache) {
      redisClient.hmset(key, new JsonObject(JsonHelper.toJsonString(values)), new Handler[AsyncResult[lang.String]] {
        override def handle(event: AsyncResult[lang.String]): Unit = {
          if (event.succeeded()) {
            if (expire != 0) {
              redisClient.expire(key, expire.toInt, new Handler[AsyncResult[lang.Long]] {
                override def handle(event: AsyncResult[lang.Long]): Unit = {
                  if (event.succeeded()) {
                    p.success(Resp.success(null))
                  } else {
                    logger.error(s"Redis hmset error.[$key] $values", event.cause())
                    p.success(Resp.serverUnavailable(s"Redis hmset error.[$key] $values"))
                  }
                }
              })
            } else {
              p.success(Resp.success(null))
            }
          } else {
            logger.error(s"Redis hmset error.[$key] $values", event.cause())
            p.success(Resp.serverUnavailable(s"Redis hmset error.[$key] $values"))
          }
        }
      })
    } else {
      p.success(Resp.notImplemented("Redis service not found."))
    }
    p.future
  }

  def hset(key: String, field: String, value: String): Future[Resp[Void]] = {
    val p = Promise[Resp[Void]]()
    if (useCache) {
      redisClient.hset(key, field, value, new Handler[AsyncResult[lang.Long]] {
        override def handle(event: AsyncResult[lang.Long]): Unit = {
          if (event.succeeded()) {
            p.success(Resp.success(null))
          } else {
            logger.error(s"Redis hset error.[$key] $field $value", event.cause())
            p.success(Resp.serverUnavailable(s"Redis hset error.[$key] $field  $value"))
          }
        }
      })
    } else {
      p.success(Resp.notImplemented("Redis service not found."))
    }
    p.future
  }

  def hget(key: String, field: String): Future[Resp[String]] = {
    val p = Promise[Resp[String]]()
    if (useCache) {
      redisClient.hget(key, field, new Handler[AsyncResult[lang.String]] {
        override def handle(event: AsyncResult[lang.String]): Unit = {
          if (event.succeeded()) {
            p.success(Resp.success(event.result()))
          } else {
            logger.error(s"Redis hget error.[$key] $field", event.cause())
            p.success(Resp.serverUnavailable(s"Redis hget error.[$key] $field"))
          }
        }
      })
    } else {
      p.success(Resp.notImplemented("Redis service not found."))
    }
    p.future
  }

  def hexists(key: String, field: String): Future[Resp[Boolean]] = {
    val p = Promise[Resp[Boolean]]()
    if (useCache) {
      redisClient.hexists(key, field, new Handler[AsyncResult[lang.Long]] {
        override def handle(event: AsyncResult[lang.Long]): Unit = {
          if (event.succeeded()) {
            p.success(Resp.success(event.result() > 0))
          } else {
            logger.error(s"Redis hexists error.[$key] $field", event.cause())
            p.success(Resp.serverUnavailable(s"Redis hset error.[$key] $field"))
          }
        }
      })
    } else {
      p.success(Resp.notImplemented("Redis service not found."))
    }
    p.future
  }

  def hgetall(key: String): Future[Resp[Map[String, String]]] = {
    val p = Promise[Resp[Map[String, String]]]()
    if (useCache) {
      redisClient.hgetall(key, new Handler[AsyncResult[JsonObject]] {
        override def handle(event: AsyncResult[JsonObject]): Unit = {
          if (event.succeeded()) {
            p.success(Resp.success(JsonHelper.toObject(event.result().encode(), classOf[Map[String, String]])))
          } else {
            logger.error(s"Redis hgetall error.[$key]", event.cause())
            p.success(Resp.serverUnavailable(s"Redis hgetall error.[$key]"))
          }
        }
      })
    } else {
      p.success(Resp.notImplemented("Redis service not found."))
    }
    p.future
  }

  def incr(key: String, value: Any): Future[Resp[Void]] = {
    val p = Promise[Resp[Void]]()
    if (useCache) {
      value match {
        case v if v.isInstanceOf[Long] || v.isInstanceOf[Int] =>
          redisClient.incrby(key, v.toString.toLong, new Handler[AsyncResult[lang.Long]] {
            override def handle(event: AsyncResult[lang.Long]): Unit = {
              if (event.succeeded()) {
                p.success(Resp.success(null))
              } else {
                logger.error(s"Redis incr error.[$key] $value", event.cause())
                p.success(Resp.serverUnavailable(s"Redis incr error.[$key] $value"))
              }
            }
          })
        case v: Float =>
          redisClient.incrbyfloat(key, v, new Handler[AsyncResult[String]] {
            override def handle(event: AsyncResult[String]): Unit = {
              if (event.succeeded()) {
                p.success(Resp.success(null))
              } else {
                logger.error(s"Redis incr error.[$key] $value", event.cause())
                p.success(Resp.serverUnavailable(s"Redis incr error.[$key] $value"))
              }
            }
          })
        case _ => p.success(Resp.badRequest(s"Redis incr error. value type [$value] not support."))
      }
    } else {
      p.success(Resp.notImplemented("Redis service not found."))
    }
    p.future
  }

  def decr(key: String, value: Any): Future[Resp[Void]] = {
    val p = Promise[Resp[Void]]()
    if (useCache) {
      value match {
        case v if v.isInstanceOf[Long] || v.isInstanceOf[Int] =>
          redisClient.decrby(key, v.toString.toLong, new Handler[AsyncResult[lang.Long]] {
            override def handle(event: AsyncResult[lang.Long]): Unit = {
              if (event.succeeded()) {
                p.success(Resp.success(null))
              } else {
                logger.error(s"Redis decr error.[$key] $value", event.cause())
                p.success(Resp.serverUnavailable(s"Redis decr error.[$key] $value"))
              }
            }
          })
        case _ => p.success(Resp.badRequest(s"Redis decr error. value type [$value] not support."))
      }
    } else {
      p.success(Resp.notImplemented("Redis service not found."))
    }
    p.future
  }

  def del(key: String): Future[Resp[Void]] = {
    val p = Promise[Resp[Void]]()
    if (useCache) {
      redisClient.del(key, new Handler[AsyncResult[lang.Long]] {
        override def handle(event: AsyncResult[lang.Long]): Unit = {
          if (event.succeeded()) {
            p.success(Resp.success(null))
          } else {
            logger.error(s"Redis del error.[$key]", event.cause())
            p.success(Resp.serverUnavailable(s"Redis del error.[$key]"))
          }
        }
      })
    } else {
      p.success(Resp.notImplemented("Redis service not found."))
    }
    p.future
  }

  def hdel(key: String, field: String): Future[Resp[Void]] = {
    val p = Promise[Resp[Void]]()
    if (useCache) {
      redisClient.hdel(key, field, new Handler[AsyncResult[lang.Long]] {
        override def handle(event: AsyncResult[lang.Long]): Unit = {
          if (event.succeeded()) {
            p.success(Resp.success(null))
          } else {
            logger.error(s"Redis hdel error.[$key]", event.cause())
            p.success(Resp.serverUnavailable(s"Redis hdel error.[$key]"))
          }
        }
      })
    } else {
      p.success(Resp.notImplemented("Redis service not found."))
    }
    p.future
  }

}
