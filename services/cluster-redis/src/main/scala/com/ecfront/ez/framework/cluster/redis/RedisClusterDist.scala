package com.ecfront.ez.framework.cluster.redis

import java.util.Date
import java.util.concurrent.TimeUnit

import com.ecfront.common.JsonHelper
import com.ecfront.ez.framework.core.EZ
import com.ecfront.ez.framework.core.cluster.{ClusterDist, ILock, IMap}

import scala.concurrent.duration.TimeUnit

object RedisClusterDist extends ClusterDist {

  override def lock(key: String): ILock = Lock(key)

  override def map[M: Manifest](key: String): IMap[M] = Map[M](key)

  case class Lock(key: String) extends ILock {

    private val lockKey = "__lock_" + key

    override def lockWithFun(leaseTime: Long = -1, unit: TimeUnit = TimeUnit.MILLISECONDS)(fun: => Any): Any = {
      try {
        lock(leaseTime, unit)
        fun
      } catch {
        case e: Throwable =>
          logger.error("execute fun error with lock", e)
          throw e
      } finally {
        unLock()
      }
    }

    override def tryLockWithFun(waitTime: Long = -1, leaseTime: Long = -1, unit: TimeUnit = TimeUnit.MILLISECONDS)(fun: => Any): Any = {
      if (tryLock(waitTime, leaseTime, unit)) {
        try {
          fun
        } catch {
          case e: Throwable =>
            logger.error("execute fun error with tryLock", e)
            throw e
        } finally {
          unLock()
        }
      }
    }

    override def lock(leaseTime: Long = -1, unit: TimeUnit = TimeUnit.MILLISECONDS): this.type = {
      if (isLock) {
        throw new Exception(s"Lock key: $key exist!")
      }
      try {
        RedisClusterCache.set(lockKey, getCurrThreadId)
        if (leaseTime != -1) {
          RedisClusterCache.expire(lockKey, convertToSec(leaseTime, unit))
        }
      } catch {
        case e: Exception => logger.warn(s"lock [$key] error.", e)
      }
      this
    }

    override def tryLock(waitTime: Long = -1, leaseTime: Long = -1, unit: TimeUnit = TimeUnit.MILLISECONDS): Boolean = {
      this.synchronized {
        try {
          if (waitTime == -1) {
            if (!isLock) {
              lock()
              true
            } else {
              false
            }
          } else {
            val waitMilliSec = convertToSec(waitTime, unit) * 1000
            val now = new Date().getTime
            while (isLock && new Date().getTime - now < waitMilliSec) {
              Thread.sleep(500)
            }
            if (leaseTime == -1) {
              if (!isLock) {
                lock()
                true
              } else {
                false
              }
            } else {
              if (!isLock) {
                lock(leaseTime, unit)
                true
              } else {
                false
              }
            }
          }
        } catch {
          case e: Exception =>
            logger.warn(s"lock [$key] error.", e)
            false
        }
      }
    }

    override def unLock(): Boolean = {
      try {
        val threadId = RedisClusterCache.get(lockKey)
        if (threadId == getCurrThreadId) {
          delete()
          true
        } else {
          false
        }
      } catch {
        case e: Exception =>
          logger.error("Unlock error.", e)
          true
      }
    }

    override def isLock: Boolean = {
      RedisClusterCache.exists(lockKey)
    }

    override def delete(): this.type = {
      RedisClusterCache.del(lockKey)
      this
    }

    private def getCurrThreadId = EZ.Info.app + "_" + EZ.Info.module + "_" + EZ.Info.instance + "_" + Thread.currentThread().getId

    private def convertToSec(time: Long = -1, unit: TimeUnit = TimeUnit.MILLISECONDS): Int = {
      unit match {
        case TimeUnit.NANOSECONDS => (time / 1000000000).toInt
        case TimeUnit.MICROSECONDS => (time / 1000000).toInt
        case TimeUnit.MILLISECONDS => (time / 1000).toInt
        case TimeUnit.SECONDS => time.toInt
        case TimeUnit.MINUTES => (time * 60).toInt
        case TimeUnit.HOURS => (time * 3600).toInt
        case TimeUnit.DAYS => (time * 86400).toInt
      }
    }

  }

  case class Map[M: Manifest](key: String) extends IMap[M] {

    private val mapKey = "__map_" + key

    override def put(key: String, value: M): this.type = {
      RedisClusterCache.hset(mapKey, key, JsonHelper.toJsonString(value))
      this
    }

    override def putAsync(key: String, value: M): this.type = {
      RedisClusterCache.hset(mapKey, key, JsonHelper.toJsonString(value))
      this
    }

    override def putIfAbsent(key: String, value: M): this.type = {
      if (!contains(key)) {
        RedisClusterCache.hset(mapKey, key, JsonHelper.toJsonString(value))
      }
      this
    }

    override def contains(key: String): Boolean = {
      RedisClusterCache.hexists(mapKey, key)
    }

    override def foreach(fun: (String, M) => Unit): this.type = {
      RedisClusterCache.hgetAll(mapKey).foreach {
        item =>
          fun(item._1, JsonHelper.toObject[M](item._2))
      }
      this
    }

    override def get(key: String): M = {
      val value = RedisClusterCache.hget(mapKey, key)
      if (value != null) {
        JsonHelper.toObject[M](value)
      } else {
        null.asInstanceOf[M]
      }
    }

    override def remove(key: String): this.type = {
      RedisClusterCache.hdel(mapKey, key)
      this
    }

    override def removeAsync(key: String): this.type = {
      RedisClusterCache.hdel(mapKey, key)
      this
    }

    override def clear(): this.type = {
      RedisClusterCache.del(mapKey)
      this
    }

  }

  override def executeOneNode(key:String,fun: => Any): Any = {
      lock(key).tryLockWithFun()(fun)
  }

}
