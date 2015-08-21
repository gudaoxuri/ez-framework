package com.ecfront.ez.framework.service.common

import com.ecfront.ez.framework.service.protocols.RedisService
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.redisson.core.RAtomicLong

case class DAtomicLongService(key: String) extends LazyLogging {

  private val atomicLong: RAtomicLong = RedisService.redis.getAtomicLong(key)

  def set(value: Long) = {
    atomicLong.set(value)
    this
  }

  def get = {
    atomicLong.get()
  }

}
