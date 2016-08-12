package com.ecfront.ez.framework.service.redis

import java.util.concurrent.TimeUnit

import com.ecfront.common.Resp
import com.ecfront.ez.framework.core.EZContext
import com.typesafe.scalalogging.slf4j.LazyLogging
import io.vertx.core.{AsyncResult, Handler}
import org.redisson.{Config, Redisson, RedissonClient}

import scala.collection.JavaConversions._
import scala.concurrent.{Future, Promise}

/**
  * Redis处理类
  */
object RedisProcessor extends LazyLogging {

  private[ecfront] var redis: RedissonClient = _

  /**
    * 初始化
    *
    * @param address redis 地址
    * @param db      redids db名
    * @param auth    密码
    * @param mode    集群模式
    * @return 结果
    */
  def init(address: List[String], db: Integer, auth: String = null, mode: String = "single"): Resp[String] = {
    val config = new Config()
    val matchMode = mode.toUpperCase match {
      case "SINGLE" =>
        val conf = config.useSingleServer()
          .setAddress(address.head)
          .setTimeout(10000)
          .setDatabase(db)
        if (auth != null && auth.nonEmpty) {
          conf.setPassword(auth)
        }
        true
      case "CLUSTER" =>
        // TODO select db
        val cluster = config.useClusterServers()
        if (auth != null && auth.nonEmpty) {
          cluster.setPassword(auth)
        }
        address.foreach(cluster.addNodeAddress(_))
        cluster.setTimeout(10000)
        true
      case _ =>
        false
    }
    if (matchMode) {
      redis = Redisson.create(config)
      Resp.success("Distributed started")
    } else {
      logger.error("Only support [ single ] or [ cluster ] mode")
      Resp.notImplemented("Only support [ single ] or [ cluster ] mode")
    }
  }

  def close(): Unit = {
    if (redis != null) {
      redis.shutdown()
    }

  }

  /**
    * 暴露redis client ，用于自定义操作
    *
    * @return redis client
    */
  def custom(): RedissonClient = redis

  /**
    * key是否存在
    *
    * @param key key
    * @return 是否存在
    */
  def exists(key: String): Resp[Boolean] = {
    execute[Boolean]({
      redis.getBucket[String](key).isExists
    }, "exists")
  }

  /**
    * 获取字符串值
    *
    * @param key key
    * @return 值
    */
  def get(key: String): Resp[Any] = {
    execute[Any]({
      redis.getBucket(key).get().asInstanceOf[Any]
    }, "get")
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
    execute[Void]({
      if (expire == 0) {
        redis.getBucket[String](key).set(value)
      } else {
        redis.getBucket[String](key).set(value, expire, TimeUnit.SECONDS)
      }
      null
    }, "del")
  }

  /**
    * 删除key
    *
    * @param key key
    * @return 是否成功
    */
  def del(key: String): Resp[Void] = {
    execute[Void]({
      redis.getBucket[String](key).delete()
      null
    }, "del")
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
    execute[Void]({
      redis.getList[String](key).addAll(values)
      if (expire != 0) {
        redis.getList[String](key).expire(expire, TimeUnit.SECONDS)
      }
      null
    }, "lmset")
  }

  /**
    * 添加列表值
    *
    * @param key   key
    * @param value value
    * @return 是否成功
    */
  def lpush(key: String, value: String): Resp[Void] = {
    execute[Void]({
      redis.getDeque[String](key).addFirst(value)
      null
    }, "lpush")
  }

  /**
    * 修改列表中索引对应的值
    *
    * @param key   key
    * @param value value
    * @param index 索引
    * @return 是否成功
    */
  def lset(key: String, value: String, index: Int): Resp[Void] = {
    execute[Void]({
      redis.getList[String](key).fastSet(index, value)
      null
    }, "lset")
  }

  /**
    * 弹出栈顶的列表值
    * 注意，Redis的列表是栈结构，先进后出
    *
    * @param key key
    * @return 栈顶的列表值
    */
  def lpop(key: String): Resp[String] = {
    execute[String]({
      redis.getQueue[String](key).poll()
    }, "lpop")
  }

  /**
    * 获取列表中索引对应的值
    * 注意，Redis的列表是栈结构，先进后出
    *
    * @param key   key
    * @param index 索引
    * @return 索引对应的值
    */
  def lindex(key: String, index: Int): Resp[String] = {
    execute[String]({
      redis.getList[String](key).get(index)
    }, "lindex")
  }

  /**
    * 获取列表值的长度
    *
    * @param key key
    * @return 长度
    */
  def llen(key: String): Resp[Long] = {
    execute[Long]({
      redis.getList[String](key).size()
    }, "llen")
  }

  /**
    * 获取列表中的所有值
    *
    * @param key key
    * @return 值列表
    */
  def lget(key: String): Resp[List[String]] = {
    execute[List[String]]({
      redis.getList[String](key).readAll().toList
    }, "lget")
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
    execute[Void]({
      redis.getMap[String, String](key).putAll(values)
      if (expire != 0) {
        redis.getMap[String, String](key).expire(expire, TimeUnit.SECONDS)
      }
      null
    }, "hmset")
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
    execute[Void]({
      redis.getMap[String, String](key).put(field, value)
      null
    }, "hset")
  }

  /**
    * 获取Hash集合field对应的值
    *
    * @param key          key
    * @param field        field
    * @param defaultValue 找不到key时使用的值
    * @return field对应的值
    */
  def hget(key: String, field: String, defaultValue: String = null): Resp[String] = {
    execute[String]({
      redis.getMap[String, String](key).getOrDefault(field, defaultValue)
    }, "hget")
  }

  /**
    * 判断Hash集合field是否存在
    *
    * @param key   key
    * @param field field
    * @return 是否存在
    */
  def hexist(key: String, field: String): Resp[Boolean] = {
    execute[Boolean]({
      redis.getMap[String, String](key).containsKey(field)
    }, "hexist")
  }

  /**
    * 获取Hash集合的所有值
    *
    * @param key key
    * @return 所有值
    */
  def hgetall(key: String): Resp[Map[String, String]] = {
    execute[Map[String, String]]({
      redis.getMap[String, String](key).readAllEntrySet().map(i => i.getKey -> i.getValue).toMap
    }, "hgetall")
  }

  /**
    * 删除Hash集合是对应的field
    *
    * @param key   key
    * @param field field
    * @return 是否成功
    */
  def hdel(key: String, field: String): Resp[Void] = {
    execute[Void]({
      redis.getMap[String, String](key).remove(field)
      null
    }, "hdel")
  }

  /**
    * 原子加操作
    *
    * @param key       key，key不存在时会自动创建值为0的对象
    * @param incrValue 要增加的值，必须是Long Int Float 或 Double
    * @return 操作后的值
    */
  def incr(key: String, incrValue: Long = 1): Resp[Long] = {
    execute[Long]({
      redis.getAtomicLong(key).addAndGet(incrValue)
    }, "incr")
  }

  /**
    * 原子减操作
    *
    * @param key       key key，key不存在时会自动创建值为0的对象
    * @param decrValue 要减少的值，必须是Long  或 Int
    * @return 操作后的值
    */
  def decr(key: String, decrValue: Long = 1): Resp[Long] = {
    execute[Long]({
      redis.getAtomicLong(key).addAndGet(-decrValue)
    }, "decr")
  }

  private def execute[T](fun: => T, method: String): Resp[T] = {
    try {
      logger.trace(s"Redis execute [$method]")
      Resp.success(fun)
    } catch {
      case e: Throwable =>
        logger.error(s"Redis execute [$method] error.", e)
        Resp.serverError(s"Redis execute [$method] error " + e.getMessage)
    }
  }


  object Async {

    /**
      * key是否存在
      *
      * @param key key
      * @return 是否存在
      */
    def exists(key: String): Future[Resp[Boolean]] = {
      execute[Boolean]({
        RedisProcessor.exists(key)
      })
    }

    /**
      * 获取字符串值
      *
      * @param key key
      * @return 值
      */
    def get(key: String): Future[Resp[Any]] = {
      execute[Any]({
        RedisProcessor.get(key)
      })
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
      execute[Void]({
        RedisProcessor.set(key, value, expire)
      })
    }

    /**
      * 删除key
      *
      * @param key key
      * @return 是否成功
      */
    def del(key: String): Future[Resp[Void]] = {
      execute[Void]({
        RedisProcessor.del(key)
      })
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
      execute[Void]({
        RedisProcessor.lmset(key, values, expire)
      })
    }

    /**
      * 添加列表值
      *
      * @param key   key
      * @param value value
      * @return 是否成功
      */
    def lpush(key: String, value: String): Future[Resp[Void]] = {
      execute[Void]({
        RedisProcessor.lpush(key, value)
      })
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
      execute[Void]({
        RedisProcessor.lset(key, value, index)
      })
    }

    /**
      * 弹出栈顶的列表值
      * 注意，Redis的列表是栈结构，先进后出
      *
      * @param key key
      * @return 栈顶的列表值
      */
    def lpop(key: String): Future[Resp[String]] = {
      execute[String]({
        RedisProcessor.lpop(key)
      })
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
      execute[String]({
        RedisProcessor.lindex(key, index)
      })
    }

    /**
      * 获取列表值的长度
      *
      * @param key key
      * @return 长度
      */
    def llen(key: String): Future[Resp[Long]] = {
      execute[Long]({
        RedisProcessor.llen(key)
      })
    }

    /**
      * 获取列表中的所有值
      *
      * @param key key
      * @return 值列表
      */
    def lget(key: String): Future[Resp[List[String]]] = {
      execute[List[String]]({
        RedisProcessor.lget(key)
      })
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
      execute[Void]({
        RedisProcessor.hmset(key, values, expire)
      })
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
      execute[Void]({
        RedisProcessor.hset(key, field, value)
      })
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
      execute[String]({
        RedisProcessor.hget(key, field, defaultValue)
      })
    }

    /**
      * 判断Hash集合field是否存在
      *
      * @param key   key
      * @param field field
      * @return 是否存在
      */
    def hexist(key: String, field: String): Future[Resp[Boolean]] = {
      execute[Boolean]({
        RedisProcessor.hexist(key, field)
      })
    }

    /**
      * 获取Hash集合的所有值
      *
      * @param key key
      * @return 所有值
      */
    def hgetall(key: String): Future[Resp[Map[String, String]]] = {
      execute[Map[String, String]]({
        RedisProcessor.hgetall(key)
      })
    }

    /**
      * 删除Hash集合是对应的field
      *
      * @param key   key
      * @param field field
      * @return 是否成功
      */
    def hdel(key: String, field: String): Future[Resp[Void]] = {
      execute[Void]({
        RedisProcessor.hdel(key, field)
      })
    }

    /**
      * 原子加操作
      *
      * @param key       key，key不存在时会自动创建值为0的对象
      * @param incrValue 要增加的值，必须是Long Int Float 或 Double
      * @return 操作后的值
      */
    def incr(key: String, incrValue: Long = 1): Future[Resp[Long]] = {
      execute[Long]({
        RedisProcessor.decr(key, incrValue)
      })
    }

    /**
      * 原子减操作
      *
      * @param key       key key，key不存在时会自动创建值为0的对象
      * @param decrValue 要减少的值，必须是Long  或 Int
      * @return 操作后的值
      */
    def decr(key: String, decrValue: Long = 1): Future[Resp[Long]] = {
      execute[Long]({
        RedisProcessor.decr(key, decrValue)
      })
    }

    private def execute[T](fun: => Resp[T]): Future[Resp[T]] = {
      val p = Promise[Resp[T]]()
      EZContext.vertx.executeBlocking(new Handler[io.vertx.core.Future[Resp[T]]] {
        override def handle(event: io.vertx.core.Future[Resp[T]]): Unit = {
          event.complete(fun)
        }
      }, false, new Handler[AsyncResult[Resp[T]]] {
        override def handle(event: AsyncResult[Resp[T]]): Unit = {
          p.success(event.result())
        }
      })
      p.future
    }

  }

}
