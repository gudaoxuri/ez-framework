package com.ecfront.ez.framework.service.distributed

import com.ecfront.ez.framework.service.redis.RedisProcessor
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.redisson.core.RQueue

/**
  * 分布式队列（不阻塞）
  *
  * @param key 队列名
  * @tparam M 队列项的类型
  */
case class DQueueService[M](key: String) extends LazyLogging {

  private val queue: RQueue[M] = RedisProcessor.custom().getQueue(key)

  def add(value: M): this.type = {
    queue.add(value)
    this
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

  def delete(): this.type = {
    queue.delete()
    this
  }

}
