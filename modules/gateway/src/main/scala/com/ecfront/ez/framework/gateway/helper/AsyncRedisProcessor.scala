package com.ecfront.ez.framework.gateway.helper

import java.util.concurrent.CountDownLatch

import com.ecfront.common.{JsonHelper, Resp}
import com.typesafe.scalalogging.slf4j.LazyLogging
import io.vertx.core.json.{JsonArray, JsonObject}
import io.vertx.core.{AsyncResult, Handler, Vertx}
import io.vertx.redis.op.SetOptions
import io.vertx.redis.{RedisClient, RedisOptions}

import scala.collection.JavaConversions._
import scala.concurrent.{Future, Promise}


object AsyncRedisProcessor extends LazyLogging {

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
      .setHost(addr.head(0))
    simpleRedis = RedisClient.create(vertx, config)
    simpleRedis.select(db, new Handler[AsyncResult[String]] {
      override def handle(event: AsyncResult[String]): Unit = {
        if (event.succeeded()) {
          if (auth != null && auth.nonEmpty) {
            simpleRedis.auth(auth, new Handler[AsyncResult[String]] {
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
            resp = Resp.success("Redis client started")
            c.countDown()
          }
        } else {
          logger.error("Redis client start fail.", event.cause())
          resp = Resp.serverError(event.cause().getMessage)
          c.countDown()
        }
      }
    })
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

  /**
    * key是否存在
    *
    * @param key key
    * @return 是否存在
    */
  def exists(key: String): Future[Resp[Boolean]] = {
    val p = Promise[Resp[Boolean]]()
    logger.trace(s"Redis [exists] $key")
    simpleRedis.exists(key, new Handler[AsyncResult[java.lang.Long]] {
      override def handle(event: AsyncResult[java.lang.Long]): Unit = {
        if (event.succeeded()) {
          p.success(Resp.success(event.result() != 0))
        } else {
          logger.error(s"Redis [exists] error. $key", event.cause())
          p.success(Resp.serverError(s"Redis [exists] error. $key"))
        }
      }
    })
    p.future
  }

  /**
    * 获取字符串值
    *
    * @param key key
    * @return 值
    */
  def get(key: String): Future[Resp[String]] = {
    val p = Promise[Resp[String]]()
    logger.trace(s"Redis [get] $key")
    simpleRedis.get(key, new Handler[AsyncResult[String]] {
      override def handle(event: AsyncResult[String]): Unit = {
        if (event.succeeded()) {
          p.success(Resp.success(event.result()))
        } else {
          logger.error(s"Redis [get] error. $key", event.cause())
          p.success(Resp.serverError(s"Redis [get] error. $key"))
        }
      }
    })
    p.future
  }

  /**
    * 设置字符串
    *
    * @param key    key
    * @param value  value
    * @param expire 过期时间(seconds)，0表示永不过期
    * @return 是否成功
    */
  def set(key: String, value: String, expire: Int = 0): Future[Resp[Void]] = {
    val p = Promise[Resp[Void]]()
    logger.trace(s"Redis [set] $key,$value")
    val opt = new SetOptions()
    if (expire != 0) {
      opt.setEX(expire)
    }
    simpleRedis.setWithOptions(key, value, opt, new Handler[AsyncResult[String]] {
      override def handle(event: AsyncResult[String]): Unit = {
        if (event.succeeded()) {
          p.success(Resp.success(null))
        } else {
          logger.error(s"Redis [set] error. $key,$value", event.cause())
          p.success(Resp.serverError(s"Redis [set] error. $key,$value"))
        }
      }
    })
    p.future
  }

  /**
    * 设置过期时间
    *
    * @param key    key
    * @param expire 过期时间(seconds)，0表示永不过期
    */
  def expire(key: String, expire: Int = 0): Future[Resp[Void]] = {
    val p = Promise[Resp[Void]]()
    logger.trace(s"Redis [expire] $key")
    simpleRedis.expire(key, expire, new Handler[AsyncResult[java.lang.Long]] {
      override def handle(event: AsyncResult[java.lang.Long]): Unit = {
        if (event.succeeded()) {
          p.success(Resp.success(null))
        } else {
          logger.error(s"Redis [expire] error. $key", event.cause())
          p.success(Resp.serverError(s"Redis [expire] error. $key"))
        }
      }
    })
    p.future
  }

  /**
    * 删除key
    *
    * @param key key
    * @return 是否成功
    */
  def del(key: String): Future[Resp[Void]] = {
    val p = Promise[Resp[Void]]()
    logger.trace(s"Redis [del] $key")
    simpleRedis.del(key, new Handler[AsyncResult[java.lang.Long]] {
      override def handle(event: AsyncResult[java.lang.Long]): Unit = {
        if (event.succeeded()) {
          p.success(Resp.success(null))
        } else {
          logger.error(s"Redis [del] error. $key", event.cause())
          p.success(Resp.serverError(s"Redis [del] error. $key"))
        }
      }
    })
    p.future
  }

  /**
    * 设置列表
    *
    * @param key    key
    * @param values values
    * @param expire 过期时间(seconds)，0表示永不过期
    * @return 是否成功
    */
  def lmset(key: String, values: List[String], expire: Int = 0): Future[Resp[Void]] = {
    val p = Promise[Resp[Void]]()
    logger.trace(s"Redis [lmset] $key,$values")
    simpleRedis.rpushMany(key, values, new Handler[AsyncResult[java.lang.Long]] {
      override def handle(event: AsyncResult[java.lang.Long]): Unit = {
        if (event.succeeded()) {
          if (expire != 0) {
            simpleRedis.expire(key, expire, new Handler[AsyncResult[java.lang.Long]] {
              override def handle(event: AsyncResult[java.lang.Long]): Unit = {}
            })
          }
          p.success(Resp.success(null))
        } else {
          logger.error(s"Redis [lmset] error. $key,$values", event.cause())
          p.success(Resp.serverError(s"Redis [lmset] error. $key,$values"))
        }
      }
    })
    p.future
  }

  /**
    * 添加列表值
    *
    * @param key   key
    * @param value value
    * @return 是否成功
    */
  def lpush(key: String, value: String): Future[Resp[Void]] = {
    val p = Promise[Resp[Void]]()
    logger.trace(s"Redis [lpush] $key,$value")
    simpleRedis.lpush(key, value, new Handler[AsyncResult[java.lang.Long]] {
      override def handle(event: AsyncResult[java.lang.Long]): Unit = {
        if (event.succeeded()) {
          p.success(Resp.success(null))
        } else {
          logger.error(s"Redis [lpush] error. $key,$value", event.cause())
          p.success(Resp.serverError(s"Redis [lpush] error. $key,$value"))
        }
      }
    })
    p.future
  }

  /**
    * 修改列表中索引对应的值
    *
    * @param key   key
    * @param value value
    * @param index 索引
    * @return 是否成功
    */
  def lset(key: String, value: String, index: Int): Future[Resp[Void]] = {
    val p = Promise[Resp[Void]]()
    logger.trace(s"Redis [lset] $key,$value")
    simpleRedis.lset(key, index, value, new Handler[AsyncResult[String]] {
      override def handle(event: AsyncResult[String]): Unit = {
        if (event.succeeded()) {
          p.success(Resp.success(null))
        } else {
          logger.error(s"Redis [lset] error. $key,$value", event.cause())
          p.success(Resp.serverError(s"Redis [lset] error. $key,$value"))
        }
      }
    })
    p.future
  }

  /**
    * 弹出栈顶的列表值
    * 注意，Redis的列表是栈结构，先进后出
    *
    * @param key key
    * @return 栈顶的列表值
    */
  def lpop(key: String): Future[Resp[String]] = {
    val p = Promise[Resp[String]]()
    logger.trace(s"Redis [lpop] $key")
    simpleRedis.lpop(key, new Handler[AsyncResult[String]] {
      override def handle(event: AsyncResult[String]): Unit = {
        if (event.succeeded()) {
          p.success(Resp.success(event.result()))
        } else {
          logger.error(s"Redis [lpop] error. $key", event.cause())
          p.success(Resp.serverError(s"Redis [lpop] error. $key"))
        }
      }
    })
    p.future
  }

  /**
    * 获取列表中索引对应的值
    * 注意，Redis的列表是栈结构，先进后出
    *
    * @param key   key
    * @param index 索引
    * @return 索引对应的值
    */
  def lindex(key: String, index: Int): Future[Resp[String]] = {
    val p = Promise[Resp[String]]()
    logger.trace(s"Redis [lindex] $key")
    simpleRedis.lindex(key, index, new Handler[AsyncResult[String]] {
      override def handle(event: AsyncResult[String]): Unit = {
        if (event.succeeded()) {
          p.success(Resp.success(event.result()))
        } else {
          logger.error(s"Redis [lindex] error. $key", event.cause())
          p.success(Resp.serverError(s"Redis [lindex] error. $key"))
        }
      }
    })
    p.future
  }

  /**
    * 获取列表值的长度
    *
    * @param key key
    * @return 长度
    */
  def llen(key: String): Future[Resp[Long]] = {
    val p = Promise[Resp[Long]]()
    logger.trace(s"Redis [llen] $key")
    simpleRedis.llen(key, new Handler[AsyncResult[java.lang.Long]] {
      override def handle(event: AsyncResult[java.lang.Long]): Unit = {
        if (event.succeeded()) {
          p.success(Resp.success(event.result()))
        } else {
          logger.error(s"Redis [llen] error. $key", event.cause())
          p.success(Resp.serverError(s"Redis [llen] error. $key"))
        }
      }
    })
    p.future
  }

  /**
    * 获取列表中的所有值
    *
    * @param key key
    * @return 值列表
    */
  def lget(key: String): Future[Resp[List[String]]] = {
    val p = Promise[Resp[List[String]]]()
    logger.trace(s"Redis [lget] $key")
    simpleRedis.llen(key, new Handler[AsyncResult[java.lang.Long]] {
      override def handle(event: AsyncResult[java.lang.Long]): Unit = {
        if (event.succeeded()) {
          simpleRedis.lrange(key, 0, event.result(), new Handler[AsyncResult[JsonArray]] {
            override def handle(event: AsyncResult[JsonArray]): Unit = {
              if (event.succeeded()) {
                p.success(Resp.success(JsonHelper.toObject[List[String]](event.result().encode())))
              } else {
                logger.error(s"Redis [lget] error. $key", event.cause())
                p.success(Resp.serverError(s"Redis [lget] error. $key"))
              }
            }
          })
        } else {
          logger.error(s"Redis [lget] error. $key", event.cause())
          p.success(Resp.serverError(s"Redis [lget] error. $key"))
        }
      }
    })
    p.future
  }

  /**
    * 设置Hash集合
    *
    * @param key    key
    * @param values values
    * @param expire 过期时间(seconds)，0表示永不过期
    * @return 是否成功
    */
  def hmset(key: String, values: Map[String, String], expire: Int = 0): Future[Resp[Void]] = {
    val p = Promise[Resp[Void]]()
    logger.trace(s"Redis [hmset] $key,$values")
    simpleRedis.hmset(key, new JsonObject(JsonHelper.toJsonString(values)), new Handler[AsyncResult[String]] {
      override def handle(event: AsyncResult[String]): Unit = {
        if (event.succeeded()) {
          p.success(Resp.success(null))
        } else {
          logger.error(s"Redis [hmset] error. $key,$values", event.cause())
          p.success(Resp.serverError(s"Redis [hmset] error. $key,$values"))
        }
      }
    })
    p.future
  }

  /**
    * 修改Hash集合field对应的值
    *
    * @param key   key
    * @param field field
    * @param value value
    * @return 是否成功
    */
  def hset(key: String, field: String, value: String): Future[Resp[Void]] = {
    val p = Promise[Resp[Void]]()
    logger.trace(s"Redis [hset] $key,$field,$value")
    simpleRedis.hset(key, field, value, new Handler[AsyncResult[java.lang.Long]] {
      override def handle(event: AsyncResult[java.lang.Long]): Unit = {
        if (event.succeeded()) {
          p.success(Resp.success(null))
        } else {
          logger.error(s"Redis [hset] error. $key,$field,$value", event.cause())
          p.success(Resp.serverError(s"Redis [hset] error. $key,$field,$value"))
        }
      }
    })
    p.future
  }

  /**
    * 获取Hash集合field对应的值
    *
    * @param key          key
    * @param field        field
    * @param defaultValue 找不到key时使用的值
    * @return field对应的值
    */
  def hget(key: String, field: String, defaultValue: String = null): Future[Resp[String]] = {
    val p = Promise[Resp[String]]()
    logger.trace(s"Redis [hget] $key,$field")
    simpleRedis.hexists(key, field, new Handler[AsyncResult[java.lang.Long]] {
      override def handle(event: AsyncResult[java.lang.Long]): Unit = {
        if (event.succeeded()) {
          if (event.result() != 0) {
            simpleRedis.hget(key, field, new Handler[AsyncResult[String]] {
              override def handle(event: AsyncResult[String]): Unit = {
                if (event.succeeded()) {
                  p.success(Resp.success(event.result()))
                } else {
                  logger.error(s"Redis [hget] error. $key,$field", event.cause())
                  p.success(Resp.serverError(s"Redis [hget] error. $key,$field"))
                }
              }
            })
          } else {
            p.success(Resp.success(defaultValue))
          }
        } else {
          logger.error(s"Redis [hget] error. $key,$field", event.cause())
          p.success(Resp.serverError(s"Redis [hget] error. $key,$field"))
        }
      }
    })
    p.future
  }

  /**
    * 判断Hash集合field是否存在
    *
    * @param key   key
    * @param field field
    * @return 是否存在
    */
  def hexist(key: String, field: String): Future[Resp[Boolean]] = {
    val p = Promise[Resp[Boolean]]()
    logger.trace(s"Redis [hexist] $key,$field")
    simpleRedis.hexists(key, field, new Handler[AsyncResult[java.lang.Long]] {
      override def handle(event: AsyncResult[java.lang.Long]): Unit = {
        if (event.succeeded()) {
          p.success(Resp.success(event.result() != 0))
        } else {
          logger.error(s"Redis [hexist] error. $key,$field", event.cause())
          p.success(Resp.serverError(s"Redis [hexist] error. $key,$field"))
        }
      }
    })
    p.future
  }

  /**
    * 获取Hash集合的所有值
    *
    * @param key key
    * @return 所有值
    */
  def hgetall(key: String): Future[Resp[Map[String, String]]] = {
    val p = Promise[Resp[Map[String, String]]]()
    logger.trace(s"Redis [hgetall] $key")
    simpleRedis.hgetall(key, new Handler[AsyncResult[JsonObject]] {
      override def handle(event: AsyncResult[JsonObject]): Unit = {
        if (event.succeeded()) {
          p.success(Resp.success(JsonHelper.toObject[Map[String, String]](event.result().encode())))
        } else {
          logger.error(s"Redis [hgetall] error. $key", event.cause())
          p.success(Resp.serverError(s"Redis [hgetall] error. $key"))
        }
      }
    })
    p.future
  }

  /**
    * 删除Hash集合是对应的field
    *
    * @param key   key
    * @param field field
    * @return 是否成功
    */
  def hdel(key: String, field: String): Future[Resp[Void]] = {
    val p = Promise[Resp[Void]]()
    logger.trace(s"Redis [hdel] $key,$field")
    simpleRedis.hdel(key, field, new Handler[AsyncResult[java.lang.Long]] {
      override def handle(event: AsyncResult[java.lang.Long]): Unit = {
        if (event.succeeded()) {
          p.success(Resp.success(null))
        } else {
          logger.error(s"Redis [hdel] error. $key,$field", event.cause())
          p.success(Resp.serverError(s"Redis [hdel] error. $key,$field"))
        }
      }
    })
    p.future
  }

  /**
    * 原子加操作
    *
    * @param key       key，key不存在时会自动创建值为0的对象
    * @param incrValue 要增加的值，必须是Long Int Float 或 Double
    * @return 操作后的值
    */
  def incr(key: String, incrValue: Long = 1): Future[Resp[Long]] = {
    val p = Promise[Resp[Long]]()
    logger.trace(s"Redis [incr] $key,$incrValue")
    simpleRedis.incrby(key, incrValue, new Handler[AsyncResult[java.lang.Long]] {
      override def handle(event: AsyncResult[java.lang.Long]): Unit = {
        if (event.succeeded()) {
          p.success(Resp.success(event.result()))
        } else {
          logger.error(s"Redis [incr] error. $key,$incrValue", event.cause())
          p.success(Resp.serverError(s"Redis [incr] error. $key,$incrValue"))
        }
      }
    })
    p.future
  }

  /**
    * 原子减操作
    *
    * @param key       key key，key不存在时会自动创建值为0的对象
    * @param decrValue 要减少的值，必须是Long  或 Int
    * @return 操作后的值
    */
  def decr(key: String, decrValue: Long = 1): Future[Resp[Long]] = {
    val p = Promise[Resp[Long]]()
    logger.trace(s"Redis [decr] $key,$decrValue")
    simpleRedis.decrby(key, decrValue, new Handler[AsyncResult[java.lang.Long]] {
      override def handle(event: AsyncResult[java.lang.Long]): Unit = {
        if (event.succeeded()) {
          p.success(Resp.success(event.result()))
        } else {
          logger.error(s"Redis [decr] error. $key,$decrValue", event.cause())
          p.success(Resp.serverError(s"Redis [decr] error. $key,$decrValue"))
        }
      }
    })
    p.future
  }

}
