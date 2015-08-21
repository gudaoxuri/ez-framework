package com.ecfront.ez.framework.service.common

import com.ecfront.ez.framework.service.protocols.RedisService
import com.typesafe.scalalogging.slf4j.LazyLogging

case class DCounterService(key: String) extends LazyLogging {

  private val counter = RedisService.redis.getAtomicLong(key)

  def set(value: Long) = {
    counter.set(value)
    this
  }

  def inc(): Long = {
    counter.incrementAndGet()
  }

  def inc(maxValue: Long): Long = {
    this.synchronized {
      val lock = DLockService("__" + key + "_counter")
      lock.delete()
      try {
        lock.lock()
        if (counter.get() < maxValue) {
          counter.incrementAndGet()
        } else {
          counter.get()
        }
      } catch {
        case e: Exception =>
          logger.error("CounterService call [ inc ] method error.", e)
          throw e
      } finally {
        lock.unLock()
      }
    }
  }

  def incWithStatus(maxValue: Long): Boolean = {
    this.synchronized {
      val lock = DLockService("__" + key + "_counter")
      lock.delete()
      try {
        lock.lock()
        if (counter.get() < maxValue) {
          counter.incrementAndGet()
          true
        } else {
          false
        }
      } catch {
        case e: Exception =>
          logger.error("CounterService call [ inc ] method error.", e)
          throw e
      } finally {
        lock.unLock()
      }
    }
  }

  def get: Long = {
    counter.get()
  }

  def dec(): Long = {
    counter.decrementAndGet()
  }

  def dec(minValue: Long): Long = {
    this.synchronized {
      val lock = DLockService("__" + key + "_counter")
      lock.delete()
      try {
        lock.lock()
        if (counter.get() > minValue) {
          counter.decrementAndGet()
        } else {
          counter.get()
        }
      } catch {
        case e: Exception =>
          logger.error("CounterService call [ dec ] method error.", e)
          throw e
      } finally {
        lock.unLock()
      }
    }
  }

  def decWithStatus(minValue: Long): Boolean = {
    this.synchronized {
      val lock = DLockService("__" + key + "_counter")
      lock.delete()
      try {
        lock.lock()
        if (counter.get() > minValue) {
          counter.decrementAndGet()
          true
        } else {
          false
        }
      } catch {
        case e: Exception =>
          logger.error("CounterService call [ decWithStatus ] method error.", e)
          throw e
      } finally {
        lock.unLock()
      }
    }
  }

  def delete() = {
    counter.delete()
    this
  }

}
