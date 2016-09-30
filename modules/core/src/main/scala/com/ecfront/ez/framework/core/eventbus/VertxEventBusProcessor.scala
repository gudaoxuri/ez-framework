package com.ecfront.ez.framework.core.eventbus

import com.ecfront.common.{JsonHelper, Resp}
import com.ecfront.ez.framework.core.{EZ, EZContext}
import io.vertx.core._
import io.vertx.core.eventbus.{DeliveryOptions, EventBus, Message, MessageConsumer}
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future, Promise}

/**
  * 事件总线处理器
  */
class VertxEventBusProcessor extends EventBusProcessor {

  private val FLAG_QUEUE_EXECUTING = "ez:eb:executing:"
  private val FLAG_CONTEXT = "__ez_context__"

  private var eb: EventBus = _
  private var vertx: Vertx = _
  private val consumerEBs = ArrayBuffer[MessageConsumer[_]]()

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

  override def publish(address: String, message: Any, args: Map[String, String] = Map()): Unit = {
    val msg = JsonHelper.toJsonString(message)
    logger.trace(s"[EB] Publish a message [$address] : $msg ")
    val opt = new DeliveryOptions
    opt.addHeader(FLAG_CONTEXT, JsonHelper.toJsonString(EZ.context))
    args.foreach {
      arg =>
        opt.addHeader(arg._1, arg._2)
    }
    eb.publish(address, msg, opt)
  }

  override def request(address: String, message: Any, args: Map[String, String] = Map(), ha: Boolean = true): Unit = {
    val msg = JsonHelper.toJsonString(message)
    logger.trace(s"[EB] Request a message [$address] : $msg ")
    if (ha) {
      EZ.cache.hset(FLAG_QUEUE_EXECUTING + address, (address + msg).hashCode + "", msg)
    }
    val opt = new DeliveryOptions
    opt.addHeader(FLAG_CONTEXT, JsonHelper.toJsonString(EZ.context))
    args.foreach {
      arg =>
        opt.addHeader(arg._1, arg._2)
    }
    eb.send(address, msg)
  }

  override def ack[E: Manifest](address: String, message: Any, args: Map[String, String] = Map(), timeout: Long = 30 * 1000): E = {
    val p = Promise[E]()
    val msg = JsonHelper.toJsonString(message)
    logger.trace(s"[EB] Ack send a message [$address] : $msg ")
    val opt = new DeliveryOptions
    opt.addHeader(FLAG_CONTEXT, JsonHelper.toJsonString(EZ.context))
    args.foreach {
      arg =>
        opt.addHeader(arg._1, arg._2)
    }
    opt.setSendTimeout(timeout)
    eb.send[String](address, msg, opt, new Handler[AsyncResult[Message[String]]] {
      override def handle(event: AsyncResult[Message[String]]): Unit = {
        if (event.succeeded()) {
          logger.trace(s"[EB] Ack reply a message [$address] : ${event.result().body()} ")
          val msg = JsonHelper.toObject[E](event.result().body())
          EZContext.setContext(JsonHelper.toObject[EZContext](event.result().headers().get(FLAG_CONTEXT)))
          p.success(msg)
        } else {
          logger.error(s"[EB] Ack reply a message error : [$address] : ${event.cause().getMessage} ", event.cause())
          throw event.cause()
        }
      }
    })
    Await.result(p.future, Duration.Inf)
  }

  override def subscribe[E: Manifest](address: String)(receivedFun: (E, Map[String, String]) => Unit): Unit = {
    consumer("Subscribe", address, receivedFun, needReply = false)
  }

  override def response[E: Manifest](address: String)(receivedFun: (E, Map[String, String]) => Unit): Unit = {
    EZ.cache.hgetAll(FLAG_QUEUE_EXECUTING + address).foreach {
      message =>
        logger.trace(s"[EB] Request executing a message [$address] : ${message._2} ")
        eb.send(address, message._2)
    }
    consumer("Response", address, receivedFun, needReply = false)
  }

  override def reply[E: Manifest](address: String)(receivedFun: (E, Map[String, String]) => Any): Unit = {
    consumer("Reply", address, receivedFun, needReply = true)
  }

  private def consumer[E: Manifest](method: String, address: String, receivedFun: (E, Map[String, String]) => Any, needReply: Boolean): Unit = {
    val consumerEB = eb.consumer(address, new Handler[Message[String]] {
      override def handle(event: Message[String]): Unit = {
        logger.trace(s"[EB] $method a message [$address] : ${event.body()} ")
        vertx.executeBlocking(new Handler[io.vertx.core.Future[Void]] {
          override def handle(e: io.vertx.core.Future[Void]): Unit = {
            try {
              EZ.cache.hdel(FLAG_QUEUE_EXECUTING + address, (address + event.body()).hashCode + "")
              val message = JsonHelper.toObject[E](event.body())
              val headers = collection.mutable.Map[String, String]()
              EZContext.setContext(JsonHelper.toObject[EZContext](event.headers().get(FLAG_CONTEXT)))
              headers.remove(FLAG_CONTEXT)
              val it = event.headers().names().iterator()
              while (it.hasNext) {
                val key = it.next()
                headers += key -> event.headers().get(key)
              }
              val replyData = receivedFun(message, headers.toMap)
              if (needReply) {
                event.reply(JsonHelper.toJsonString(replyData))
              }
              e.complete()
            } catch {
              case ex: Throwable =>
                logger.error(s"[EB] $method [$address] Execute error.", ex)
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
        logger.error(s"[EB] $method [$address] error.", event)
        throw event.getCause
      }
    })
  }

}