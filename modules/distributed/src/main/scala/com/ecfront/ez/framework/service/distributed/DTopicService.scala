package com.ecfront.ez.framework.service.distributed

import java.util.concurrent.TimeUnit

import com.ecfront.common.JsonHelper
import com.ecfront.ez.framework.service.redis.RedisProcessor
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.redisson.core.{MessageListener, RTopic}

/**
  * 分布式消息队列
  *
  * @param key 消息队列名
  * @tparam M 消息队列项的类型
  */
case class DTopicService[M](key: String) extends LazyLogging {

  private val topic: RTopic[M] = RedisProcessor.redis.getTopic(key)

  /**
    * 发布消息
    *
    * @param message 消息内容
    */
  def publish(message: M): this.type = {
    topic.publish(message)
    this
  }

  /**
    * 订阅消息
    *
    * @param fun 处理函数
    */
  def subscribe(fun: => M => Unit): this.type = {
    topic.addListener(new MessageListener[M]() {
      override def onMessage(s: String, msg: M): Unit = {
        try {
          fun(msg)
        } catch {
          case e: Throwable =>
            logger.error(s"Distributed subscribe [$topic] process error.", e)
            throw e
        }
      }
    })
    this
  }

  /**
    * 订阅消息，一条消息只由一个节点处理
    *
    * @param fun 处理函数
    */
  def subscribeOneNode(fun: => M => Unit): this.type = {
    topic.addListener(new MessageListener[M]() {
      override def onMessage(s: String, msg: M): Unit = {
        val lock = RedisProcessor.redis.getAtomicLong(key + "_" + JsonHelper.toJsonString(msg).hashCode)
        if (lock.incrementAndGet() == 1) {
          try {
            fun(msg)
          } catch {
            case e: Throwable =>
              logger.error(s"Distributed subscribe [$topic] process error.", e)
              throw e
          } finally {
            // 10天后过期，删除锁
            lock.expire(10, TimeUnit.DAYS)
          }
        }
      }
    })
    this
  }

  def delete(): this.type = {
    // topic.delete()
    this
  }

}
