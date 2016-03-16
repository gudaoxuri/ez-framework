package com.ecfront.ez.framework.service.distributed

import com.typesafe.scalalogging.slf4j.LazyLogging
import org.redisson.core.RAtomicLong

/**
  * 分布式AtomicLong
  *
  * @param key AtomicLong名
  */
case class DAtomicLongService(key: String) extends LazyLogging {

  private val atomicLong: RAtomicLong = DistributedProcessor.redis.getAtomicLong(key)

  def set(value: Long): this.type = {
    atomicLong.set(value)
    this
  }

  def get: Long = {
    atomicLong.get()
  }

}
