package com.ecfront.ez.framework.service.distributed

import com.typesafe.scalalogging.slf4j.LazyLogging

/**
  * 分布式原子计数器
  *
  * @param key 计数器名
  */
case class DCounterService(key: String) extends LazyLogging {

  private val counter = DistributedProcessor.redis.getAtomicLong(key)

  def set(value: Long): this.type = {
    counter.set(value)
    this
  }

  def get: Long = {
    counter.get()
  }

  def inc(): Long = {
    counter.incrementAndGet()
  }

  def dec(): Long = {
    counter.decrementAndGet()
  }

  /**
    * 有上限的inc
    *
    * @param maxValue 最大值
    * @return 当前值
    */
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

  /**
    * 有上限的inc并返回是否增加了
    *
    * @param maxValue 最大值
    * @return 是否增加了，已达上限返回false
    */
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

  /**
    * 有下限的dec
    *
    * @param minValue 最小值
    * @return 当前值
    */
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

  /**
    * 有下限的inc并返回是否减少了
    *
    * @param minValue 最小值
    * @return 是否减少了，已达下限返回false
    */
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

  def delete(): this.type = {
    counter.delete()
    this
  }

}
