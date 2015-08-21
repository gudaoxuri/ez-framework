package com.ecfront.ez.framework.service.common

import com.ecfront.ez.framework.service.protocols.RedisService
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.redisson.core.RQueue

case class DQueueService[M](key: String) extends LazyLogging {

  private val queue: RQueue[M] = RedisService.redis.getQueue(key)

  def add(value: M) {
    queue.add(value)
  }

  def peek(): M = {
    queue.peek()
  }

  def poll(): M = {
    queue.poll()
  }

  def size(): Int = {
    queue.size()
  }

  def delete() = {
    queue.delete()
    this
  }

}
