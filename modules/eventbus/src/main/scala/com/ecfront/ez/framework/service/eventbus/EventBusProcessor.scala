package com.ecfront.ez.framework.service.eventbus

import com.ecfront.common.{JsonHelper, Resp}
import com.ecfront.ez.framework.core.EZContext
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

  private val consumerEBs = ArrayBuffer[MessageConsumer[_]]()

  def init(vertx: Vertx): Resp[String] = {
    Await.result(Async.init(vertx), Duration.Inf)
  }

  def publish(address: String, message: Any): Unit = {
    Async.publish(address, message)

  }

  def send(address: String, message: Any, timeout: Long = 30 * 1000): Unit = {
    Async.send(address, message, timeout)
  }

  def sendWithReply[E: Manifest](address: String, message: Any, timeout: Long = 30 * 1000): (E, Any => Unit) = {
    Await.result(Async.sendWithReply(address, message, timeout), Duration.Inf)
  }

  def consumer[E: Manifest](address: String): (E, Any => Unit) = {
    Await.result(Async.consumer(address), Duration.Inf)
  }

  object Async {

    def init(vertx: Vertx): Future[Resp[String]] = {
      val p = Promise[Resp[String]]()
      val mgr = new HazelcastClusterManager()
      val options = new VertxOptions().setClusterManager(mgr)
      Vertx.clusteredVertx(options, new Handler[AsyncResult[Vertx]] {
        override def handle(res: AsyncResult[Vertx]): Unit = {
          if (res.succeeded()) {
            eb = res.result().eventBus()
            logger.info("EventBus init successful.")
            p.success(Resp.success(null))
          } else {
            logger.error("EventBus init error.", res.cause())
            p.success(Resp.serverError(res.cause().getMessage))
          }
        }
      })
      sys.addShutdownHook {
        consumerEBs.foreach {
          _.unregister()
        }
      }
      p.future
    }

    def publish(address: String, message: Any): Unit = {
      eb.publish(address, JsonHelper.toJsonString(message))
    }

    def send(address: String, message: Any, timeout: Long = 30 * 1000): Unit = {
      sendAdv[Void](address, message, null, timeout)
    }

    def sendWithReply[E: Manifest](address: String, message: Any, timeout: Long = 30 * 1000): Future[(E, Any => Unit)] = {
      val p = Promise[(E, Any => Unit)]()
      sendAdv[E](address, message, {
        (message, reply) =>
          p.success((message, reply))
      }, timeout)
      p.future
    }

    def sendAdv[E: Manifest](address: String, message: Any, callback: (E, Any => Unit) => Unit, timeout: Long = 30 * 1000): Unit = {
      val opt = new DeliveryOptions()
      opt.setSendTimeout(timeout)
      eb.send(address, JsonHelper.toJsonString(message), opt, new Handler[AsyncResult[Message[String]]] {
        override def handle(event: AsyncResult[Message[String]]): Unit = {
          if (event.succeeded()) {
            logger.trace(s"Received a reply [$address] : ${event.result.body()} ")
            val message = JsonHelper.toObject[E](event.result().body())
            if (callback != null) {
              EZContext.vertx.executeBlocking(new Handler[io.vertx.core.Future[Void]] {
                override def handle(e: io.vertx.core.Future[Void]): Unit = {
                  try {
                    callback(message, {
                      reply =>
                        val replyMessage = JsonHelper.toJsonString(reply)
                        logger.trace(s"Reply a message [$address] : $replyMessage ")
                        event.result.reply(replyMessage)
                    })
                    e.complete()
                  } catch {
                    case ex: Throwable =>
                      logger.error("EventBus execute error.", ex)
                      e.fail(ex)
                  }
                }
              }, false, new Handler[AsyncResult[Void]] {
                override def handle(event: AsyncResult[Void]): Unit = {
                }
              })
            }
          } else {
            logger.error(s"Receive reply [$address] error.", event.cause())
          }
        }
      })
    }

    def consumer[E: Manifest](address: String): Future[(E, Any => Unit)] = {
      val p = Promise[(E, Any => Unit)]()
      consumerAdv[E](address, {
        (message, reply) =>
          p.success((message, reply))
      })
      p.future
    }

    def consumerAdv[E: Manifest](address: String, callback: (E, Any => Unit) => Unit): Unit = {
      val consumerEB = eb.consumer(address, new Handler[Message[String]] {
        override def handle(event: Message[String]): Unit = {
          logger.trace(s"Received a message [$address] : ${event.body()} ")
          val message = JsonHelper.toObject[E](event.body())
          EZContext.vertx.executeBlocking(new Handler[io.vertx.core.Future[Void]] {
            override def handle(e: io.vertx.core.Future[Void]): Unit = {
              try {
                callback(message, {
                  reply =>
                    val replyMessage = JsonHelper.toJsonString(reply)
                    logger.trace(s"Reply a message [$address] : $replyMessage ")
                    event.reply(replyMessage)
                })
                e.complete()
              } catch {
                case ex: Throwable =>
                  logger.error("EventBus execute error.", ex)
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
            logger.error(s"Register consumer [$address] error.", event.cause())
          }
        }
      })
    }

  }

}
