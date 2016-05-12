package com.ecfront.ez.framework.service.distributed

import java.util.concurrent.TimeUnit

import com.ecfront.common.JsonHelper
import com.ecfront.ez.framework.service.redis.RedisProcessor
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.redisson.core.{MessageListener, RBlockingQueue, RTopic}

/**
  * 分布式消息队列
  *
  * @param key 消息队列名
  * @tparam M 消息队列项的类型
  */
case class DMQService[M](key: String) extends LazyLogging {

  private val topic: RTopic[M] = RedisProcessor.redis.getTopic(key)
  private val queue: RBlockingQueue[M] = RedisProcessor.redis.getBlockingQueue(key)

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
    * 发送消息(point to point)
    *
    * @param message 消息内容
    */
  def send(message: M): this.type = {
    queue.put(message)
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
            logger.error(s"Distributed subscribe [$key] process error.", e)
            throw e
        }
      }
    })
    this
  }

  /**
    * 接收消息(point to point)
    *
    * @param fun 处理函数
    */
  def receive(fun: => M => Unit): this.type = {
    new Thread(new Runnable {
      override def run(): Unit = {
        while (true) {
          try {
            fun(queue.take())
          } catch {
            case e: Throwable =>
              logger.error(s"Distributed receive [$key] process error.", e)
              throw e
          }

        }
      }
    }).start()
    this
  }

  /**
    * 接收消息，一条消息只由一个节点处理
    *
    * @param fun 处理函数
    */
  def subscribeOneNode(fun: => M => Unit): this.type = {
    topic.addListener(new MessageListener[M]() {
      override def onMessage(s: String, msg: M): Unit = {
        val lock = key + "_" + JsonHelper.toJsonString(msg).hashCode
        if (RedisProcessor.redis.getAtomicLong(lock).incrementAndGet() == 1) {
          try {
            fun(msg)
          } catch {
            case e: Throwable =>
              logger.error(s"Distributed subscribe [$key] process error.", e)
              throw e
          } finally {
            RedisProcessor.redis.getAtomicLong(lock).expire(5, TimeUnit.SECONDS)
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
