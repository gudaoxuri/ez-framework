package com.ecfront.ez.framework.service.common

import com.ecfront.ez.framework.service.protocols.RedisService
import org.redisson.core.{MessageListener, RTopic}

case class DTopicService[M](key: String) {

  private val topic: RTopic[M] = RedisService.redis.getTopic(key)

  def publish(message: M) = {
    topic.publish(message)
    this
  }

  def subscribe(fun: => M => Unit) = {
    topic.addListener(new MessageListener[M]() {
      override def onMessage(s: String, msg: M): Unit = {
        fun(msg)
      }
    })
    this
  }

  def delete() = {
//    topic.delete()
    this
  }

}
