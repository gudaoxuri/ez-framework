package com.ecfront.ez.framework.core.cache

import com.ecfront.common.Resp
import com.ecfront.ez.framework.core.EZManager
import redis.clients.jedis._

import scala.collection.JavaConversions._

/**
  * Redis处理类
  */
class RedisCacheProcessor extends CacheProcessor[JedisCommands] {

  private var redisCluster: JedisCluster = _
  private var redisPool: JedisPool = _

  /**
    * 初始化
    *
    * @param address redis 地址
    * @param db      redids db名
    * @param auth    密码
    * @return 结果
    */
  private[core] def init(address: Seq[String], db: Integer, auth: String = ""): Resp[Void] = {
    if (address.size == 1) {
      val Array(host, port) = address.head.split(":")
      redisPool = new JedisPool(
        new JedisPoolConfig(), host, port.toInt,
        Protocol.DEFAULT_TIMEOUT, if (auth == null || auth.isEmpty) null else auth, db)
    } else {
      val node = address.map {
        addr =>
          val Array(host, port) = addr.split(":")
          new HostAndPort(host, port.toInt)
      }.toSet
      redisCluster = new JedisCluster(node)
      // TODO select db & pwd
    }
    sys.addShutdownHook {
      close()
    }
    logger.info("[Redis] Init successful")
    Resp.success(null)
  }

  private def close(): Unit = {
    if (redisCluster != null) {
      redisCluster.close()
    }
    if (redisPool != null) {
      redisPool.close()
    }
  }

  /**
    * 暴露redis client ，用于自定义操作
    *
    * @return redis client
    */
  override def client(): JedisCommands = {
  if (redisPool != null) {
      if(!EZManager.isClose) {
        redisPool.getResource
      }else{
        null
      }
    } else {
      redisCluster
    }
  }


  /**
    * key是否存在
    *
    * @param key key
    * @return 是否存在
    */
  override def exists(key: String): Boolean = {
    execute[Boolean](client(), {
      _.exists(key)
    }, "exists")
  }

  /**
    * 获取字符串值
    *
    * @param key key
    * @return 值
    */
  override def get(key: String): String = {
    execute[String](client(), {
      _.get(key)
    }, "get")
  }

  /**
    * 设置字符串
    *
    * @param key    key
    * @param value  value
    * @param expire 过期时间(seconds)，0表示永不过期
    */
  override def set(key: String, value: String, expire: Int = 0): Unit = {
    execute[Unit](client(), {
      client =>
        client.set(key, value)
        if (expire != 0) {
          client.expire(key, expire)
        }
    }, "set")
  }

  /**
    * 删除key
    *
    * @param key key
    */
  override def del(key: String): Unit = {
    execute[Unit](client(), {
      _.del(key)
    }, "del")
  }

  /**
    * 设置列表
    *
    * @param key    key
    * @param values values
    * @param expire 过期时间(seconds)，0表示永不过期
    */
  override def lmset(key: String, values: List[String], expire: Int = 0): Unit = {
    execute[Unit](client(), {
      client =>
        if (values.nonEmpty) {
          values.foreach(client.lpush(key, _))
        }
        if (expire != 0) {
          client.expire(key, expire)
        }
    }, "lmset")
  }

  /**
    * 添加列表值
    *
    * @param key   key
    * @param value value
    */
  override def lpush(key: String, value: String): Unit = {
    execute[Unit](client(), {
      _.lpush(key, value)
    }, "lpush")
  }

  /**
    * 修改列表中索引对应的值
    *
    * @param key   key
    * @param value value
    * @param index 索引
    */
  override def lset(key: String, value: String, index: Long): Unit = {
    execute[Unit](client(), {
      _.lset(key, index, value)
    }, "lset")
  }

  /**
    * 弹出栈顶的列表值
    * 注意，Redis的列表是栈结构，先进后出
    *
    * @param key key
    * @return 栈顶的列表值
    */
  override def lpop(key: String): String = {
    execute[String](client(), {
      _.lpop(key)
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
  override def lindex(key: String, index: Int): String = {
    execute[String](client(), {
      _.lindex(key, index)
    }, "lindex")
  }

  /**
    * 获取列表值的长度
    *
    * @param key key
    * @return 长度
    */
  override def llen(key: String): Long = {
    execute[Long](client(), {
      _.llen(key)
    }, "llen")
  }

  /**
    * 获取列表中的所有值
    *
    * @param key key
    * @return 值列表
    */
  override def lget(key: String): List[String] = {
    execute[List[String]](client(), {
      client =>
        client.lrange(key, 0, client.llen(key)).toList
    }, "lget")
  }

  /**
    * 设置Hash集合
    *
    * @param key    key
    * @param values values
    * @param expire 过期时间(seconds)，0表示永不过期
    */
  override def hmset(key: String, values: Map[String, String], expire: Int = 0): Unit = {
    execute[Unit](client(), {
      client =>
        client.hmset(key, values)
        if (expire != 0) {
          client.expire(key, expire)
        }
    }, "hmset")
  }

  /**
    * 修改Hash集合field对应的值
    *
    * @param key   key
    * @param field field
    * @param value value
    */
  override def hset(key: String, field: String, value: String): Unit = {
    execute[Unit](client(), {
      _.hset(key, field, value)
    }, "hset")
  }

  /**
    * 获取Hash集合field对应的值
    *
    * @param key   key
    * @param field field
    * @return field对应的值
    */
  override def hget(key: String, field: String): String = {
    execute[String](client(), {
      _.hget(key, field)
    }, "hget")
  }

  /**
    * 判断Hash集合field是否存在
    *
    * @param key   key
    * @param field field
    * @return 是否存在
    */
  override def hexists(key: String, field: String): Boolean = {
    execute[Boolean](client(), {
      _.hexists(key, field)
    }, "hexists")
  }

  /**
    * 获取Hash集合的所有值
    *
    * @param key key
    * @return 所有值
    */
  override def hgetAll(key: String): Map[String, String] = {
    execute[Map[String, String]](client(), {
      _.hgetAll(key).toMap
    }, "hgetAll")
  }

  /**
    * 删除Hash集合是对应的field
    *
    * @param key   key
    * @param field field
    */
  override def hdel(key: String, field: String): Unit = {
    execute[Unit](client(), {
      _.hdel(key, field)
    }, "hdel")
  }

  /**
    * 原子加操作
    *
    * @param key       key，key不存在时会自动创建值为0的对象
    * @param incrValue 要增加的值，必须是Long Int Float 或 Double
    * @return 操作后的值
    */
  override def incr(key: String, incrValue: Long = 1): Long = {
    execute[Long](client(), {
      _.incrBy(key, incrValue)
    }, "incr")
  }

  /**
    * 原子减操作
    *
    * @param key       key key，key不存在时会自动创建值为0的对象
    * @param decrValue 要减少的值，必须是Long  或 Int
    * @return 操作后的值
    */
  override def decr(key: String, decrValue: Long = 1): Long = {
    execute[Long](client(), {
      _.decrBy(key, decrValue)
    }, "decr")
  }

  override def expire(key: String, expire: Int = 0): Unit = {
    execute[Unit](client(), {
      _.expire(key, expire)
    }, "expire")
  }

  override def flushdb(): Unit = {
    execute[Unit](client(), {
      client =>
        if (redisPool != null) {
          client.asInstanceOf[Jedis].flushDB()
        }
    }, "flushdb")
  }

  private def execute[T](client: JedisCommands, fun: JedisCommands => T, method: String): T = {
    try {
      if(client!=null) {
        fun(client)
      }else{
        logger.warn("Redis is closed.")
        null.asInstanceOf[T]
      }
    } catch {
      case e: Throwable =>
        logger.error(s"Redis $method error.", e)
        throw e
    } finally {
      if (redisPool != null && client != null) {
        client.asInstanceOf[Jedis].close()
      }
    }
  }

}