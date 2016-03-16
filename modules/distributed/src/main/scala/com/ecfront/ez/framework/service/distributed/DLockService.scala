package com.ecfront.ez.framework.service.distributed

import java.util.concurrent.TimeUnit

import com.typesafe.scalalogging.slf4j.LazyLogging

/**
  * 分布式锁
  *
  * @param key 锁名
  */
case class DLockService(key: String) extends LazyLogging {

  private val lock = DistributedProcessor.redis.getLock(key)

  def lock(leaseTime: Long = -1, unit: TimeUnit = null): this.type = {
    this.synchronized {
      try {
        if (!lock.isLocked) {
          lock.lock(leaseTime, unit)
          //  RedisService.currentLockedNode.fastPut(key, RedisService.nodeId)
          /* } else if (!RedisService.nodeHeartbeat.containsKey(RedisService.currentLockedNode.get(key))) {
             lock.delete()
             lock.lock(leaseTime, unit)
             RedisService.currentLockedNode.fastPut(key, RedisService.nodeId)*/
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
        val isLock = if (waitTime == 0) {
          lock.tryLock()
        } else {
          lock.tryLock(waitTime, leaseTime, unit)
        }
        if (isLock) {
          // RedisService.currentLockedNode.fastPut(key, RedisService.nodeId)
          true
          /*  } else if (!RedisService.nodeHeartbeat.containsKey(RedisService.currentLockedNode.get(key))) {
              lock.delete()
              lock.lock(leaseTime, unit)
              RedisService.currentLockedNode.fastPut(key, RedisService.nodeId)
              true*/
        } else {
          false
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
      if (lock.isLocked && lock.isHeldByCurrentThread) {
        try {
          lock.unlock()
          // RedisService.currentLockedNode.remove(key)
          true
        } catch {
          case e: Exception =>
            lock.delete()
            //   RedisService.currentLockedNode.remove(key)
            logger.error("Unlock error.", e)
            true
        }
      } else {
        false
      }
    }
  }

  def isLock: Boolean = {
    lock.isLocked
  }

  def delete(): this.type = {
    this.synchronized {
      lock.delete()
      // RedisService.currentLockedNode.remove(key)
      this
    }
  }

}
