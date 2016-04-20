package com.ecfront.ez.framework.service.distributed

import java.util.concurrent.TimeUnit

import com.ecfront.ez.framework.service.redis.RedisProcessor
import com.typesafe.scalalogging.slf4j.LazyLogging

/**
  * 分布式CountDownLatch
  *
  * @param key CountDownLatch名
  */
case class DCountDownLatchService(key: String) extends LazyLogging {

  private val countDownLatch = RedisProcessor.redis.getCountDownLatch(key)

  def set(value: Long): this.type = {
    RedisProcessor.redis.getAtomicLong(key).set(value)
    this
  }

  def await(time: Long = -1, unit: TimeUnit = null): this.type = {
    if (time == -1) {
      countDownLatch.await()
    } else {
      countDownLatch.await(time, unit)
    }
    this
  }

  def get: Long = {
    countDownLatch.getCount
  }

  def countDown(): this.type = {
    countDownLatch.countDown()
    this
  }

  def delete(): this.type = {
    countDownLatch.delete()
    this
  }

}
