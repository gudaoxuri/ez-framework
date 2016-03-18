package com.ecfront.ez.framework.service.masterslave

import com.ecfront.common.JsonHelper
import com.ecfront.ez.framework.service.redis.RedisProcessor
import com.typesafe.scalalogging.slf4j.LazyLogging

/**
  * 高可用管理
  *
  * 当Slave收到Master的消息后会将消息放入执行池（队列），
  * Slave支持同时多个任务，也就是说Kafka接收到消息后要马上commit（否则就收不到一条消息了），
  * 但这会导致执行失败后消息丢失。
  *
  * 此HA逻辑会将收到的消息暂存到Redis中
  */
object HAManager extends LazyLogging {

  private val FLAG_HA = "ez-ms-ha"

  var ha: Boolean = false
  var clusterId: String = ""
  var worker: String = ""

  def loadCacheData(): Unit = {
    val allMessages = RedisProcessor.hgetall(packageMessagesKey)
    if (allMessages && allMessages.body != null) {
      allMessages.body.foreach {
        message =>
          val taskPrepare = JsonHelper.toObject[TaskPrepareDTO](message._2)
          logger.trace(s"Load HA data : $message._2")
          ExecutorPool.addExecute(Executor(taskPrepare))
      }
    }
  }

  def saveToCache(message: TaskPrepareDTO): Unit = {
    RedisProcessor.hset(packageMessagesKey, message.instanceId, JsonHelper.toJsonString(message))
  }

  def removeInCache(instanceId: String): Unit = {
    RedisProcessor.hdel(packageMessagesKey, instanceId)
  }

  private def packageMessagesKey: String = {
    FLAG_HA + "." + clusterId + ".messages"
  }

}
