package com.ecfront.ez.framework.service.distributed

import com.ecfront.ez.framework.service.redis.RedisProcessor
import org.redisson.core.{MessageListener, RTopic}

/**
  * 分布式消息队列
  *
  * @param key 消息队列名
  * @tparam M 消息队列项的类型
  */
case class DTopicService[M](key: String) {

  private val topic: RTopic[M] = RedisProcessor.redis.getTopic(key)

  def publish(message: M): this.type = {
    topic.publish(message)
    this
  }

  def subscribe(fun: => M => Unit): this.type = {
    topic.addListener(new MessageListener[M]() {
      override def onMessage(s: String, msg: M): Unit = {
        fun(msg)
      }
    })
    this
  }

  def delete(): this.type = {
    // topic.delete()
    this
  }

}
