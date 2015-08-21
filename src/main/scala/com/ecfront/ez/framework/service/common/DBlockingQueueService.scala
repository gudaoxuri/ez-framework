package com.ecfront.ez.framework.service.common

import com.ecfront.ez.framework.service.protocols.RedisService
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.redisson.core.RBlockingQueue

case class DBlockingQueueService[M](key: String) extends LazyLogging {

  private val queue: RBlockingQueue[M] = RedisService.redis.getBlockingQueue(key)

  def put(value: M) {
    queue.put(value)
  }

  def peek(): M = {
    queue.peek()
  }

  def take(): M = {
    queue.take()
  }

  def size(): Int = {
    queue.size()
  }

  def delete() = {
    queue.delete()
    this
  }

}
