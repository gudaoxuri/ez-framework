package com.ecfront.ez.framework.service.distributed

import java.util.concurrent.{Executors, TimeUnit}

import com.ecfront.common.{JsonHelper, Resp}
import com.ecfront.ez.framework.service.redis.RedisProcessor
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.redisson.core._

import scala.collection.JavaConversions._

/**
  * 分布式消息队列
  *
  * @param key 消息队列名
  * @tparam M 消息队列项的类型
  */
case class DMQService[M](key: String) extends LazyLogging {

  private val topic: RTopic[M] = RedisProcessor.redis.getTopic(key)
  private val queue: RBlockingQueue[M] = RedisProcessor.redis.getBlockingQueue(key)
  private val executingItems: RMap[Int, M] = RedisProcessor.redis.getMap[Int, M](key + ":executing")
  private val threads = Executors.newCachedThreadPool()

  /**
    * 发布消息
    *
    * @param message 消息内容
    */
  def publish(message: M): this.type = {
    if (!RedisProcessor.redis.isShutdown && !RedisProcessor.redis.isShuttingDown) {
      logger.trace(s"Distributed public [$key] message : ${JsonHelper.toJsonString(message)}")
      topic.publish(message)
    }
    this
  }

  /**
    * 发送消息(point to point)
    *
    * @param message 消息内容
    */
  def send(message: M): this.type = {
    if (!RedisProcessor.redis.isShutdown && !RedisProcessor.redis.isShuttingDown) {
      logger.trace(s"Distributed send [$key] message : ${JsonHelper.toJsonString(message)}")
      queue.put(message)
    }
    this
  }

  /**
    * 订阅消息
    *
    * @param fun 处理函数
    */
  def subscribe(fun: => M => Resp[Void]): this.type = {
    // TODO lock
    executingItems.foreach {
      i =>
        publish(i._2)
        executingItems.remove(i._1)
    }
    topic.addListener(new MessageListener[M]() {
      override def onMessage(s: String, msg: M): Unit = {
        try {
          val strMsg = JsonHelper.toJsonString(msg)
          val id = strMsg.hashCode
          executingItems.put(id, msg)
          logger.trace(s"Distributed subscribe [$key] message : $strMsg")
          val result = fun(msg)
          if (result) {
            executingItems.remove(id)
            logger.trace(s"Distributed subscribe [$key] execute success : $strMsg")
          } else {
            logger.warn(s"Distributed subscribe [$key] execute error [${result.code}][${result.message}] : $strMsg")
          }
        } catch {
          case e: Throwable =>
            logger.error(s"Distributed subscribe [$key] process error.", e)
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
  def receive(fun: => M => Resp[Void]): this.type = {
    // TODO lock
    executingItems.foreach {
      i =>
        send(i._2)
        executingItems.remove(i._1)
    }
    threads.execute(
      new Runnable {
        override def run(): Unit = {
          while (!RedisProcessor.redis.isShutdown && !RedisProcessor.redis.isShuttingDown) {
            try {
              val msg = queue.take()
              val strMsg = JsonHelper.toJsonString(msg)
              val id = strMsg.hashCode
              executingItems.put(id, msg)
              logger.trace(s"Distributed receive [$key] message : $strMsg")
              val result = fun(msg)
              if (result) {
                executingItems.remove(id)
                logger.trace(s"Distributed receive [$key] execute success : $strMsg")
              } else {
                logger.warn(s"Distributed receive [$key] execute error [${result.code}][${result.message}] : $strMsg")
              }
            } catch {
              case e: Throwable =>
                if (!RedisProcessor.redis.isShutdown && !RedisProcessor.redis.isShuttingDown) {
                  logger.error(s"Distributed receive [$key] process error.", e)
                }
            }
          }
        }
      })
    this
  }

  /**
    * 接收消息，一条消息只由一个节点处理
    *
    * @param fun 处理函数
    */
  def subscribeOneNode(fun: => M => Resp[Void]): this.type = {
    // TODO lock
    executingItems.foreach {
      i =>
        publish(i._2)
        executingItems.remove(i._1)
    }
    topic.addListener(new MessageListener[M]() {
      override def onMessage(s: String, msg: M): Unit = {
        val strMsg = JsonHelper.toJsonString(msg)
        val id = strMsg.hashCode
        val lock = key + "_" + id
        if (RedisProcessor.redis.getAtomicLong(lock).incrementAndGet() == 1) {
          try {
            executingItems.put(id, msg)
            logger.trace(s"Distributed subscribe [$key] message in $strMsg")
            val result = fun(msg)
            if (result) {
              executingItems.remove(id)
              logger.trace(s"Distributed subscribe [$key] execute success in $strMsg")
            } else {
              logger.warn(s"Distributed subscribe [$key] execute error [${result.code}][${result.message}] in $strMsg")
            }
          } catch {
            case e: Throwable =>
              logger.error(s"Distributed subscribe [$key] process error.", e)
          } finally {
            RedisProcessor.redis.getAtomicLong(lock).expire(5, TimeUnit.SECONDS)
          }
        }
      }
    })
    this
  }

  def delete(): this.type = {
    threads.shutdown()
    // topic.delete()
    this
  }

  sys.addShutdownHook({
    this.delete()
  })

}
