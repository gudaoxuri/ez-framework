package com.ecfront.ez.framework.service.redis

import java.lang

import com.ecfront.common.{JsonHelper, Resp}
import com.typesafe.scalalogging.slf4j.LazyLogging
import io.vertx.core.json.{JsonArray, JsonObject}
import io.vertx.core.{AsyncResult, Handler, Vertx}
import io.vertx.redis.op.SetOptions
import io.vertx.redis.{RedisClient, RedisOptions}

import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future, Promise}

/**
  * Redis处理类
  */
object RedisProcessor extends LazyLogging {

  private var redisClient: RedisClient = _

  /**
    * 初始化
    *
    * @param vertx Vertx实例
    * @param host  redis主机
    * @param port  redis端口
    * @param db    redids db名
    * @param auth  密码
    * @return 结果
    */
  def init(vertx: Vertx, host: String, port: Int, db: Integer, auth: String = null): Resp[String] = {
    val p = Promise[Resp[String]]()
    redisClient = RedisClient.create(vertx, new RedisOptions().setHost(host).setPort(port))
    if (auth != null && auth.nonEmpty) {
      redisClient.auth(auth, new Handler[AsyncResult[String]] {
        override def handle(event: AsyncResult[String]): Unit = {
          if (event.succeeded()) {
            redisClient.select(db, new Handler[AsyncResult[String]] {
              override def handle(event: AsyncResult[String]): Unit = {
                if (event.succeeded()) {
                  p.success(Resp.success(s"Redis connected $host:$port"))
                } else {
                  logger.error("Redis connection error.", event.cause())
                  p.success(Resp.serverUnavailable(s"Redis connection error : ${event.cause().getMessage}"))
                }
              }
            })
          } else {
            logger.error("Redis connection error.", event.cause())
            p.success(Resp.serverUnavailable(s"Redis connection error : ${event.cause().getMessage}"))
          }
        }
      })
    } else {
      redisClient.select(db, new Handler[AsyncResult[String]] {
        override def handle(event: AsyncResult[String]): Unit = {
          if (event.succeeded()) {
            p.success(Resp.success(s"Redis connected $host:$port"))
          } else {
            logger.error("Redis connection error.", event.cause())
            p.success(Resp.serverUnavailable(s"Redis connection error : ${event.cause().getMessage}"))
          }
        }
      })
    }
    Await.result(p.future, Duration.Inf)
  }

  /**
    * 暴露redis client ，用于自定义操作
    *
    * @return redis client
    */
  def custom(): RedisClient = redisClient

  /**
    * key是否存在
    *
    * @param key key
    * @return 是否存在
    */
  def exists(key: String): Resp[Boolean] = {
    Await.result(Async.exists(key), Duration.Inf)
  }

  /**
    * 获取字符串值
    *
    * @param key key
    * @return 字符串值
    */
  def get(key: String): Resp[String] = {
    Await.result(Async.get(key), Duration.Inf)
  }

  /**
    * 设置字符串
    *
    * @param key    key
    * @param value  value
    * @param expire 过期时间(seconds)，0表示永不过期
    * @return 是否成功
    */
  def set(key: String, value: String, expire: Long = 0): Resp[Void] = {
    Await.result(Async.set(key, value, expire), Duration.Inf)
  }

  /**
    * 删除key
    *
    * @param key key
    * @return 是否成功
    */
  def del(key: String): Resp[Void] = {
    Await.result(Async.del(key), Duration.Inf)
  }

  /**
    * 设置列表
    *
    * @param key    key
    * @param values values
    * @param expire 过期时间(seconds)，0表示永不过期
    * @return 是否成功
    */
  def lmset(key: String, values: List[String], expire: Long = 0): Resp[Void] = {
    Await.result(Async.lmset(key, values, expire), Duration.Inf)
  }

  /**
    * 添加列表值
    *
    * @param key   key
    * @param value value
    * @return 是否成功
    */
  def lpush(key: String, value: String): Resp[Void] = {
    Await.result(Async.lpush(key, value), Duration.Inf)
  }

  /**
    * 修改列表中索引对应的值
    *
    * @param key   key
    * @param value value
    * @param index 索引
    * @return 是否成功
    */
  def lset(key: String, value: String, index: Long): Resp[Void] = {
    Await.result(Async.lset(key, value, index), Duration.Inf)
  }

  /**
    * 弹出栈顶的列表值
    * 注意，Redis的列表是栈结构，先进后出
    *
    * @param key key
    * @return 栈顶的列表值
    */
  def lpop(key: String): Resp[String] = {
    Await.result(Async.lpop(key), Duration.Inf)
  }

  /**
    * 获取列表中索引对应的值
    * 注意，Redis的列表是栈结构，先进后出
    *
    * @param key   key
    * @param index 索引
    * @return 索引对应的值
    */
  def lindex(key: String, index: Long): Resp[String] = {
    Await.result(Async.lindex(key, index), Duration.Inf)
  }

  /**
    * 获取列表值的长度
    *
    * @param key key
    * @return 长度
    */
  def llen(key: String): Resp[Long] = {
    Await.result(Async.llen(key), Duration.Inf)
  }

  /**
    * 获取列表中的所有值
    *
    * @param key key
    * @return 值列表
    */
  def lget(key: String): Resp[List[String]] = {
    Await.result(Async.lget(key), Duration.Inf)
  }

  /**
    * 设置Hash集合
    *
    * @param key    key
    * @param values values
    * @param expire 过期时间(seconds)，0表示永不过期
    * @return 是否成功
    */
  def hmset(key: String, values: Map[String, String], expire: Long = 0): Resp[Void] = {
    Await.result(Async.hmset(key, values, expire), Duration.Inf)
  }

  /**
    * 修改Hash集合field对应的值
    *
    * @param key   key
    * @param field field
    * @param value value
    * @return 是否成功
    */
  def hset(key: String, field: String, value: String): Resp[Void] = {
    Await.result(Async.hset(key, field, value), Duration.Inf)
  }

  /**
    * 获取Hash集合field对应的值
    *
    * @param key   key
    * @param field field
    * @return field对应的值
    */
  def hget(key: String, field: String): Resp[String] = {
    Await.result(Async.hget(key, field), Duration.Inf)
  }

  /**
    * 判断Hash集合field是否存在
    *
    * @param key   key
    * @param field field
    * @return 是否存在
    */
  def hexists(key: String, field: String): Resp[Boolean] = {
    Await.result(Async.hexists(key, field), Duration.Inf)
  }

  /**
    * 获取Hash集合的所有值
    *
    * @param key key
    * @return 所有值
    */
  def hgetall(key: String): Resp[Map[String, String]] = {
    Await.result(Async.hgetall(key), Duration.Inf)
  }

  /**
    * 删除Hash集合是对应的field
    *
    * @param key   key
    * @param field field
    * @return 是否成功
    */
  def hdel(key: String, field: String): Resp[Void] = {
    Await.result(Async.hdel(key, field), Duration.Inf)
  }

  /**
    * 原子加操作
    *
    * @param key       key，key不存在时会自动创建值为0的对象
    * @param incrValue 要增加的值，必须是Long Int Float 或 Double
    * @return 是否成功
    */
  def incr(key: String, incrValue: Any): Resp[Void] = {
    Await.result(Async.incr(key, incrValue), Duration.Inf)
  }

  /**
    * 原子减操作
    *
    * @param key       key key，key不存在时会自动创建值为0的对象
    * @param decrValue 要减少的值，必须是Long  或 Int
    * @return 是否成功
    */
  def decr(key: String, decrValue: Any): Resp[Void] = {
    Await.result(Async.decr(key, decrValue), Duration.Inf)
  }

  object Async {

    /**
      * key是否存在
      *
      * @param key key
      * @return 是否存在
      */
    def exists(key: String): Future[Resp[Boolean]] = {
      val p = Promise[Resp[Boolean]]()
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
      p.future
    }

    /**
      * 获取字符串值
      *
      * @param key key
      * @return 字符串值
      */
    def get(key: String): Future[Resp[String]] = {
      val p = Promise[Resp[String]]()
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
    def set(key: String, value: String, expire: Long = 0): Future[Resp[Void]] = {
      val p = Promise[Resp[Void]]()
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
    def lmset(key: String, values: List[String], expire: Long = 0): Future[Resp[Void]] = {
      val p = Promise[Resp[Void]]()
      redisClient.lpushMany(key, values, new Handler[AsyncResult[lang.Long]] {
        override def handle(event: AsyncResult[lang.Long]): Unit = {
          if (event.succeeded()) {
            if (expire != 0) {
              redisClient.expire(key, expire.toInt, new Handler[AsyncResult[lang.Long]] {
                override def handle(event: AsyncResult[lang.Long]): Unit = {
                  if (event.succeeded()) {
                    p.success(Resp.success(null))
                  } else {
                    logger.error(s"Redis lmset error.[$key] $values", event.cause())
                    p.success(Resp.serverUnavailable(s"Redis lmset error.[$key] $values"))
                  }
                }
              })
            } else {
              p.success(Resp.success(null))
            }
          } else {
            logger.error(s"Redis lmset error.[$key] $values", event.cause())
            p.success(Resp.serverUnavailable(s"Redis lmset error.[$key] $values"))
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
      redisClient.lpush(key, value, new Handler[AsyncResult[lang.Long]] {
        override def handle(event: AsyncResult[lang.Long]): Unit = {
          if (event.succeeded()) {
            p.success(Resp.success(null))
          } else {
            logger.error(s"Redis lpush error.[$key] $value", event.cause())
            p.success(Resp.serverUnavailable(s"Redis lpush error.[$key] $value"))
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
    def lset(key: String, value: String, index: Long): Future[Resp[Void]] = {
      val p = Promise[Resp[Void]]()
      redisClient.lset(key, index, value, new Handler[AsyncResult[lang.String]] {
        override def handle(event: AsyncResult[lang.String]): Unit = {
          if (event.succeeded()) {
            p.success(Resp.success(null))
          } else {
            logger.error(s"Redis lset error.[$key] $value", event.cause())
            p.success(Resp.serverUnavailable(s"Redis lset error.[$key] $value"))
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
      redisClient.lpop(key, new Handler[AsyncResult[lang.String]] {
        override def handle(event: AsyncResult[lang.String]): Unit = {
          if (event.succeeded()) {
            p.success(Resp.success(event.result()))
          } else {
            logger.error(s"Redis lpop error.[$key]", event.cause())
            p.success(Resp.serverUnavailable(s"Redis lpop error.[$key]"))
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
    def lindex(key: String, index: Long): Future[Resp[String]] = {
      val p = Promise[Resp[String]]()
      redisClient.lindex(key, index.toInt, new Handler[AsyncResult[lang.String]] {
        override def handle(event: AsyncResult[lang.String]): Unit = {
          if (event.succeeded()) {
            p.success(Resp.success(event.result()))
          } else {
            logger.error(s"Redis lindex error.[$key]", event.cause())
            p.success(Resp.serverUnavailable(s"Redis lindex error.[$key]"))
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
      redisClient.llen(key, new Handler[AsyncResult[lang.Long]] {
        override def handle(event: AsyncResult[lang.Long]): Unit = {
          if (event.succeeded()) {
            p.success(Resp.success(event.result()))
          } else {
            logger.error(s"Redis llen error.[$key]", event.cause())
            p.success(Resp.serverUnavailable(s"Redis llen error.[$key]"))
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
      llen(key).onSuccess {
        case lenResp =>
          if (lenResp) {
            redisClient.lrange(key, 0, lenResp.body, new Handler[AsyncResult[JsonArray]] {
              override def handle(event: AsyncResult[JsonArray]): Unit = {
                if (event.succeeded()) {
                  p.success(Resp.success(JsonHelper.toObject(event.result().encode(), classOf[List[String]])))
                } else {
                  logger.error(s"Redis lget error.[$key]", event.cause())
                  p.success(Resp.serverUnavailable(s"Redis lget error.[$key]"))
                }
              }
            })
          } else {
            p.success(lenResp)
          }
      }
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
    def hmset(key: String, values: Map[String, String], expire: Long = 0): Future[Resp[Void]] = {
      val p = Promise[Resp[Void]]()
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
      p.future
    }

    /**
      * 获取Hash集合field对应的值
      *
      * @param key   key
      * @param field field
      * @return field对应的值
      */
    def hget(key: String, field: String): Future[Resp[String]] = {
      val p = Promise[Resp[String]]()
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
      p.future
    }

    /**
      * 判断Hash集合field是否存在
      *
      * @param key   key
      * @param field field
      * @return 是否存在
      */
    def hexists(key: String, field: String): Future[Resp[Boolean]] = {
      val p = Promise[Resp[Boolean]]()
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
      p.future
    }

    /**
      * 原子加操作
      *
      * @param key       key，key不存在时会自动创建值为0的对象
      * @param incrValue 要增加的值，必须是Long Int Float 或 Double
      * @return 是否成功
      */
    def incr(key: String, incrValue: Any): Future[Resp[Void]] = {
      val p = Promise[Resp[Void]]()
      incrValue match {
        case v if v.isInstanceOf[Long] || v.isInstanceOf[Int] =>
          redisClient.incrby(key, v.toString.toLong, new Handler[AsyncResult[lang.Long]] {
            override def handle(event: AsyncResult[lang.Long]): Unit = {
              if (event.succeeded()) {
                p.success(Resp.success(null))
              } else {
                logger.error(s"Redis incr error.[$key] $incrValue", event.cause())
                p.success(Resp.serverUnavailable(s"Redis incr error.[$key] $incrValue"))
              }
            }
          })
        case v if v.isInstanceOf[Float] || v.isInstanceOf[Double] =>
          redisClient.incrbyfloat(key, v.asInstanceOf[Double], new Handler[AsyncResult[String]] {
            override def handle(event: AsyncResult[String]): Unit = {
              if (event.succeeded()) {
                p.success(Resp.success(null))
              } else {
                logger.error(s"Redis incr error.[$key] $incrValue", event.cause())
                p.success(Resp.serverUnavailable(s"Redis incr error.[$key] $incrValue"))
              }
            }
          })
        case _ => p.success(Resp.badRequest(s"Redis incr error. value type [$incrValue] not support."))
      }
      p.future
    }

    /**
      * 原子减操作
      *
      * @param key       key key，key不存在时会自动创建值为0的对象
      * @param decrValue 要减少的值，必须是Long  或 Int
      * @return 是否成功
      */
    def decr(key: String, decrValue: Any): Future[Resp[Void]] = {
      val p = Promise[Resp[Void]]()
      decrValue match {
        // TODO add float & double support.
        case v if v.isInstanceOf[Long] || v.isInstanceOf[Int] =>
          redisClient.decrby(key, v.toString.toLong, new Handler[AsyncResult[lang.Long]] {
            override def handle(event: AsyncResult[lang.Long]): Unit = {
              if (event.succeeded()) {
                p.success(Resp.success(null))
              } else {
                logger.error(s"Redis decr error.[$key] $decrValue", event.cause())
                p.success(Resp.serverUnavailable(s"Redis decr error.[$key] $decrValue"))
              }
            }
          })
        case _ => p.success(Resp.badRequest(s"Redis decr error. value type [$decrValue] not support."))
      }
      p.future
    }

  }

}
