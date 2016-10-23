package com.ecfront.ez.framework.core.eventbus

import java.util.concurrent.CountDownLatch

import com.ecfront.common.{JsonHelper, Resp}
import com.ecfront.ez.framework.core.rpc.RPCProcessor
import com.ecfront.ez.framework.core.{EZ, EZContext}
import io.vertx.core._
import io.vertx.core.eventbus.{DeliveryOptions, EventBus, Message, MessageConsumer}
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Promise}

/**
  * 事件总线处理器
  */
class VertxEventBusProcessor extends EventBusProcessor {

  private val FLAG_QUEUE_EXECUTING = "ez:eb:executing:"
  private[ecfront] val FLAG_CONTEXT = "__ez_context__"

  private[ecfront] var eb: EventBus = _
  private var vertx: Vertx = _
  private val consumerEBs = ArrayBuffer[MessageConsumer[_]]()

  private[core] def init(_vertx: Vertx, mgr: HazelcastClusterManager): Resp[Void] = {
    vertx = _vertx
    val c = new CountDownLatch(1)
    var resp: Resp[Void] = null
    val options = new VertxOptions().setClusterManager(mgr)
    Vertx.clusteredVertx(options, new Handler[AsyncResult[Vertx]] {
      override def handle(res: AsyncResult[Vertx]): Unit = {
        if (res.succeeded()) {
          eb = res.result().eventBus()
          logger.info("[EB] Init successful")
          resp = Resp.success(null)
        } else {
          logger.error("[EB] Init error", res.cause())
          resp = Resp.serverError(res.cause().getMessage)
        }
        c.countDown()
      }
    })
    sys.addShutdownHook {
      consumerEBs.foreach {
        _.unregister()
      }
      vertx.close()
    }
    c.await()
    resp
  }

  override protected def doPublish(address: String, message: Any, args: Map[String, String]): Unit = {
    val opt = new DeliveryOptions
    opt.addHeader(FLAG_CONTEXT, JsonHelper.toJsonString(EZ.context))
    args.foreach {
      arg =>
        opt.addHeader(arg._1, arg._2)
    }
    eb.publish(address, message, opt)
  }

  override protected def doRequest(address: String, message: Any, args: Map[String, String], ha: Boolean): Unit = {
    if (ha) {
      EZ.cache.hset(FLAG_QUEUE_EXECUTING + address, (address + message).hashCode + "", message.toString)
    }
    val opt = new DeliveryOptions
    opt.addHeader(FLAG_CONTEXT, JsonHelper.toJsonString(EZ.context))
    args.foreach {
      arg =>
        opt.addHeader(arg._1, arg._2)
    }
    eb.send(address, message,opt)
  }

  override protected def doAck[E](address: String, message: Any, args: Map[String, String], timeout: Long)
                                 (implicit e: Manifest[E]): (E, Map[String, String]) = {
    val p = Promise[(E, Map[String, String])]()
    val opt = new DeliveryOptions
    opt.addHeader(FLAG_CONTEXT, JsonHelper.toJsonString(EZ.context))
    args.foreach {
      arg =>
        opt.addHeader(arg._1, arg._2)
    }
    opt.setSendTimeout(timeout)
    eb.send[String](address, message, opt, new Handler[AsyncResult[Message[String]]] {
      override def handle(event: AsyncResult[Message[String]]): Unit = {
        if (event.succeeded()) {
          EZContext.setContext(JsonHelper.toObject[EZContext](event.result().headers().get(FLAG_CONTEXT)))
          event.result().headers().remove(FLAG_CONTEXT)
          logger.trace(s"[EB] Ack reply a message [$address] : ${RPCProcessor.cutPrintShow(event.result().body())} ")
          try {
            val msg =
              if (e != manifest[Nothing]) {
                JsonHelper.toObject[E](event.result().body())
              } else {
                null.asInstanceOf[E]
              }
            val headers = collection.mutable.Map[String, String]()
            val it = event.result().headers().names().iterator()
            while (it.hasNext) {
              val key = it.next()
              headers += key -> event.result().headers().get(key)
            }
            p.success(msg, headers.toMap)
          } catch {
            case e: Throwable =>
              logger.error(s"[EB] Ack reply a message error : [$address] : ${event.cause().getMessage} ", event.cause())
          }
        } else {
          logger.error(s"[EB] Ack reply a message error : [$address] : ${event.cause().getMessage} ", event.cause())
          throw event.cause()
        }
      }
    })
    Await.result(p.future, Duration.Inf)
  }

  override protected def doSubscribe[E: Manifest](address: String, reqClazz: Class[E])(receivedFun: (E, Map[String, String]) => Unit): Unit = {
    consumer[E]("subscribe", address, {
      (message: E, args) =>
        receivedFun(message, args)
        null
    }, needReply = false, reqClazz)
  }

  override protected def doResponse[E: Manifest](address: String, reqClazz: Class[E])(receivedFun: (E, Map[String, String]) => Unit): Unit = {
    EZ.cache.hgetAll(FLAG_QUEUE_EXECUTING + address).foreach {
      message =>
        logger.trace(s"[EB] Request executing a message [$address] : ${message._2} ")
        eb.send(address, message._2)
    }
    consumer[E]("response", address, {
      (message: E, args) =>
        receivedFun(message, args)
        null
    }, needReply = false, reqClazz)
  }

  override protected def doReply[E: Manifest](address: String, reqClazz: Class[E])
                                             (receivedFun: (E, Map[String, String]) => (Any, Map[String, String])): Unit = {
    consumer[E]("reply", address, receivedFun, needReply = true, reqClazz)
  }

  private def consumer[E: Manifest](method: String, address: String, receivedFun: (E, Map[String, String]) => (Any, Map[String, String]),
                                    needReply: Boolean, reqClazz: Class[E] = null): Unit = {
    val consumerEB = eb.consumer(address, new Handler[Message[String]] {
      override def handle(event: Message[String]): Unit = {
        vertx.executeBlocking(new Handler[io.vertx.core.Future[Void]] {
          override def handle(e: io.vertx.core.Future[Void]): Unit = {
            val headers = collection.mutable.Map[String, String]()
            EZContext.setContext(JsonHelper.toObject[EZContext](event.headers().get(FLAG_CONTEXT)))
            event.headers.remove(FLAG_CONTEXT)
            val it = event.headers().names().iterator()
            while (it.hasNext) {
              val key = it.next()
              headers += key -> event.headers().get(key)
            }
            logger.trace(s"[EB] Received a $method message [$address] : $headers > ${RPCProcessor.cutPrintShow(event.body())} ")
            try {
              EZ.cache.hdel(FLAG_QUEUE_EXECUTING + address, (address + event.body()).hashCode + "")
              val message = toObject[E](event.body(), reqClazz)
              val replyData = receivedFun(message, headers.toMap)
              if (needReply) {
                val opt = new DeliveryOptions()
                if (replyData._2 != null && replyData._2.nonEmpty) {
                  replyData._2.foreach {
                    item =>
                      opt.addHeader(item._1, item._2)
                  }
                }
                opt.addHeader(FLAG_CONTEXT, JsonHelper.toJsonString(EZ.context))
                event.reply(JsonHelper.toJsonString(replyData._1), opt)
              }
              e.complete()
            } catch {
              case ex: Throwable =>
                logger.error(s"[EB] $method [$address] Execute error", ex)
                if (needReply) {
                  val opt = new DeliveryOptions()
                  opt.addHeader(FLAG_CONTEXT, JsonHelper.toJsonString(EZ.context))
                  event.reply(JsonHelper.toJsonString(Resp.serverError(s"[EB] $method [$address] Execute error : ${ex.getMessage}")), opt)
                }
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
          logger.error(s"[EB] Register consumer [$address] error", event.cause())
        }
      }
    })
    consumerEB.exceptionHandler(new Handler[Throwable] {
      override def handle(event: Throwable): Unit = {
        logger.error(s"[EB] $method [$address] error", event)
        throw event.getCause
      }
    })
  }

}
