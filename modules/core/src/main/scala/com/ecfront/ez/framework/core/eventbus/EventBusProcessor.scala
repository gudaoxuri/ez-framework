package com.ecfront.ez.framework.core.eventbus

import com.ecfront.common.{JsonHelper, Resp}
import com.ecfront.ez.framework.core.redis.RedisProcessor
import com.typesafe.scalalogging.slf4j.LazyLogging
import io.vertx.core._
import io.vertx.core.eventbus.{DeliveryOptions, EventBus, Message, MessageConsumer}
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future, Promise}

/**
  * 事件总线处理器
  */
object EventBusProcessor extends LazyLogging {

  private var eb: EventBus = _
  private var vertx: Vertx = _
  private val consumerEBs = ArrayBuffer[MessageConsumer[_]]()
  private val FLAG_QUEUE_EXECUTING = "ez:eb:executing:"

  private[core] def init(_vertx: Vertx): Future[Resp[Void]] = {
    vertx = _vertx
    val p = Promise[Resp[Void]]()
    val mgr = new HazelcastClusterManager()
    val options = new VertxOptions().setClusterManager(mgr)
    Vertx.clusteredVertx(options, new Handler[AsyncResult[Vertx]] {
      override def handle(res: AsyncResult[Vertx]): Unit = {
        if (res.succeeded()) {
          eb = res.result().eventBus()
          logger.info("[EB] Init successful.")
          p.success(Resp.success(null))
        } else {
          logger.error("[EB] Init error.", res.cause())
          p.success(Resp.serverError(res.cause().getMessage))
        }
      }
    })
    sys.addShutdownHook {
      consumerEBs.foreach {
        _.unregister()
      }
      vertx.close()
    }
    p.future
  }

  def publish(address: String, message: Any): Unit = {
    val msg = JsonHelper.toJsonString(message)
    logger.trace(s"[EB] Publish a message [$address] : $msg ")
    eb.publish(address, msg)
  }

  def request(address: String, message: Any, timeout: Long = 30 * 1000): Unit = {
    val msg = JsonHelper.toJsonString(message)
    RedisProcessor.hset(FLAG_QUEUE_EXECUTING + address, (address + msg).hashCode + "", msg)
    logger.trace(s"[EB] Send a message [$address] : $msg ")
    eb.send(address, msg, new DeliveryOptions().setSendTimeout(timeout))
  }

  def ack[E: Manifest](address: String, message: Any, timeout: Long = 30 * 1000): E = {
    val p = Promise[E]()
    val msg = JsonHelper.toJsonString(message)
    logger.trace(s"[EB] Ack send a message [$address] : $msg ")
    eb.send[String](address, msg, new DeliveryOptions().setSendTimeout(timeout), new Handler[AsyncResult[Message[String]]] {
      override def handle(event: AsyncResult[Message[String]]): Unit = {
        if (event.succeeded()) {
          logger.trace(s"[EB] Ack reply a message [$address] : ${event.result().body()} ")
          val msg = JsonHelper.toObject[E](event.result().body())
          p.success(msg)
        } else {
          logger.error(s"[EB] Ack reply a message [$address] : ${event.cause().getMessage} ", event.cause())
          throw event.cause()
        }
      }
    })
    Await.result(p.future, Duration.Inf)
  }

  def subscribe[E: Manifest](address: String)(receivedFun: E => Unit): Unit = {
    consumer(address, receivedFun, needReply = false)
  }

  def response[E: Manifest](address: String)(receivedFun: E => Unit): Unit = {
    RedisProcessor.hgetAll(FLAG_QUEUE_EXECUTING + address).foreach {
      message =>
        logger.trace(s"[EB] Send executing a message [$address] : ${message._2} ")
        eb.send(address, message._2)
    }
    consumer(address, receivedFun, needReply = false)
  }

  def reply[E: Manifest](address: String)(receivedFun: E => Any): Unit = {
    consumer(address, receivedFun, needReply = true)
  }

  private def consumer[E: Manifest](address: String, receivedFun: E => Any, needReply: Boolean): Unit = {
    val consumerEB = eb.consumer(address, new Handler[Message[String]] {
      override def handle(event: Message[String]): Unit = {
        logger.trace(s"[EB] Received a message [$address] : ${event.body()} ")
        vertx.executeBlocking(new Handler[io.vertx.core.Future[Void]] {
          override def handle(e: io.vertx.core.Future[Void]): Unit = {
            try {
              RedisProcessor.hdel(FLAG_QUEUE_EXECUTING + address, (address + event.body()).hashCode + "")
              val message = JsonHelper.toObject[E](event.body())
              val replyData = receivedFun(message)
              if (needReply) {
                event.reply(JsonHelper.toJsonString(replyData))
              }
              e.complete()
            } catch {
              case ex: Throwable =>
                logger.error("[EB] Execute error.", ex)
                e.fail(ex)
            }
          }
        }, false, new Handler[AsyncResult[Void]] {
          override def handle(event: AsyncResult[Void]): Unit = {
          }
        })
      }
    })
    consumerEB.completionHandler(new Handler[AsyncResult[Void]] {
      override def handle(event: AsyncResult[Void]): Unit = {
        if (event.succeeded()) {
          consumerEBs += consumerEB
        } else {
          logger.error(s"[EB] Register consumer [$address] error.", event.cause())
        }
      }
    })
    consumerEB.exceptionHandler(new Handler[Throwable] {
      override def handle(event: Throwable): Unit = {
        logger.error(s"[EB] Process consumer [$address] error.", event)
      }
    })
  }

}
