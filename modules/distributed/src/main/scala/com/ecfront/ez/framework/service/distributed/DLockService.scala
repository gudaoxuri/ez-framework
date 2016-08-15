package com.ecfront.ez.framework.service.distributed

import java.util.concurrent.TimeUnit

import com.ecfront.ez.framework.service.redis.RedisProcessor
import com.typesafe.scalalogging.slf4j.LazyLogging

/**
  * 分布式锁
  *
  * @param key 锁名
  */
case class DLockService(key: String) extends LazyLogging {

  private val redis = RedisProcessor.custom()
  private val lock = redis.getLock(key)

  def lockWithFun(leaseTime: Long = -1, unit: TimeUnit = TimeUnit.MILLISECONDS)(fun: => Any): Any = {
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

  def tryLockWithFun(waitTime: Long = 0, leaseTime: Long = -1, unit: TimeUnit = TimeUnit.MILLISECONDS)(fun: => Any): Any = {
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

  def lock(leaseTime: Long = -1, unit: TimeUnit = TimeUnit.MILLISECONDS): this.type = {
    this.synchronized {
      try {
        if (!redis.isShutdown && !lock.isLocked) {
          lock.lock(leaseTime, unit)
        }
      } catch {
        case e: Exception => logger.warn(s"lock [$key] error.", e)
      }
    }
    this
  }

  def tryLock(waitTime: Long = 0, leaseTime: Long = -1, unit: TimeUnit = TimeUnit.MILLISECONDS): Boolean = {
    this.synchronized {
      try {
        if (waitTime == 0) {
          if (!redis.isShutdown) {
            lock.tryLock()
          } else {
            false
          }
        } else {
          if (!redis.isShutdown) {
            lock.tryLock(waitTime, leaseTime, unit)
          } else {
            false
          }
        }
      } catch {
        case e: Exception =>
          logger.warn(s"lock [$key] error.", e)
          false
      }
    }
  }

  def unLock(): Boolean = {
    this.synchronized {
      if (!redis.isShutdown && lock.isLocked && lock.isHeldByCurrentThread) {
        try {
          lock.unlock()
          true
        } catch {
          case e: Exception =>
            logger.error("Unlock error.", e)
            true
        }
      } else {
        false
      }
    }
  }

  def isLock: Boolean = {
    !redis.isShutdown && lock.isLocked
  }

  def delete(): this.type = {
    this.synchronized {
      if (!redis.isShutdown) {
        lock.delete()
      }
      this
    }
  }

}
