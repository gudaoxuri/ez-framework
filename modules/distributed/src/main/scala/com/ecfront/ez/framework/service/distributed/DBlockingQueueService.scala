package com.ecfront.ez.framework.service.distributed

import com.ecfront.ez.framework.service.redis.RedisProcessor
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.redisson.core.RBlockingQueue

/**
  * 分布式阻塞队列
  *
  * @param key 队列名
  * @tparam M 队列项的类型
  */
case class DBlockingQueueService[M](key: String) extends LazyLogging {

  private val queue: RBlockingQueue[M] = RedisProcessor.redis.getBlockingQueue(key)

  def put(value: M): this.type = {
    queue.put(value)
    this
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

  def delete(): this.type = {
    queue.delete()
    this
  }

}
