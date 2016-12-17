package com.ecfront.ez.framework.core.cluster

import java.util.concurrent.{CopyOnWriteArrayList, TimeoutException}

import com.ecfront.common.Resp
import com.ecfront.ez.framework.core.EZ
import com.ecfront.ez.framework.core.logger.Logging
import com.rabbitmq.client.AMQP.BasicProperties
import com.rabbitmq.client._

import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object RabbitMQClusterManager extends Logging {

  private var conn: Connection = _
  private val channels: CopyOnWriteArrayList[Channel] = new CopyOnWriteArrayList[Channel]()

  private var defaultTopicExchangeName: String = _
  private var defaultRPCExchangeName: String = _
  private var defaultQueueExchangeName: String = _

  def init(config: Map[String, Any]): Resp[Void] = {
    val factory = new ConnectionFactory()
    if (config.contains("userName")) {
      factory.setUsername(config("userName").asInstanceOf[String])
      factory.setPassword(config("password").asInstanceOf[String])
    }
    if (config.contains("virtualHost")) {
      factory.setVirtualHost(config("virtualHost").asInstanceOf[String])
    }
    factory.setHost(config("host").asInstanceOf[String])
    factory.setPort(config("port").asInstanceOf[Int])
    if (config.contains("defaultTopicExchangeName")) {
      defaultTopicExchangeName = config("defaultTopicExchangeName").asInstanceOf[String]
    }
    if (config.contains("defaultRPCExchangeName")) {
      defaultRPCExchangeName = config("defaultRPCExchangeName").asInstanceOf[String]
    }
    if (config.contains("defaultQueueExchangeName")) {
      defaultQueueExchangeName = config("defaultQueueExchangeName").asInstanceOf[String]
    }
    conn = factory.newConnection()
    sys.addShutdownHook {
      close()
    }
    Resp.success(null)
  }

  private def getChannel(): Channel = {
    val channel = conn.createChannel()
    channels += channel
    channel
  }

  private def closeChannel(): Unit = {
    channels.foreach {
      channel =>
        if (channel.isOpen) {
          channel.close()
        }
    }
  }

  def publish(topic: String, message: String, args: Map[String, String], exchangeName: String = defaultTopicExchangeName): Unit = {
    val channel = getChannel()
    channel.exchangeDeclare(exchangeName, "direct")
    channel.queueDeclare(topic, false, false, false, null)
    val opt = new AMQP.BasicProperties.Builder().headers(args).build()
    channel.basicPublish(exchangeName, topic, opt, message.getBytes("UTF-8"))
    channel.close()
  }

  def subscribe(topic: String, exchangeName: String = defaultTopicExchangeName)(receivedFun: (String, Map[String, String]) => Unit): Unit = {
    val channel = getChannel()
    channel.exchangeDeclare(exchangeName, "direct")
    channel.queueDeclare(topic, false, false, false, null)
    val queueName = channel.queueDeclare().getQueue()
    channel.queueBind(queueName, exchangeName, topic)
    val consumer = new DefaultConsumer(channel) {
      override def handleDelivery(consumerTag: String, envelope: Envelope, properties: BasicProperties, body: Array[Byte]): Unit = {
        val header =
          if (properties.getHeaders != null) {
            properties.getHeaders.map {
              header =>
                header._1 -> header._2.toString
            }.toMap
          } else {
            Map[String, String]()
          }
        val message = new String(body, "UTF-8")
        receivedFun(message, header)
      }
    }
    channel.basicConsume(queueName, true, consumer)
  }

  def request(address: String, message: String, args: Map[String, String] = Map(), exchangeName: String = defaultQueueExchangeName): Unit = {
    val channel = getChannel()
    channel.exchangeDeclare(exchangeName, "direct")
    channel.queueDeclare(address, true, false, false, null)
    val opt = new AMQP.BasicProperties.Builder().headers(args).build()
    channel.basicPublish(exchangeName, address, opt, message.getBytes("UTF-8"))
    channel.close()
  }

  def response(address: String, exchangeName: String = defaultQueueExchangeName)(receivedFun: (String, Map[String, String]) => Unit): Unit = {
    val channel = getChannel()
    channel.exchangeDeclare(exchangeName, "direct")
    channel.queueDeclare(address, true, false, false, null)
    channel.queueBind(address, exchangeName, address)
    channel.basicQos(1)
    val consumer = new QueueingConsumer(channel)
    EZ.execute.execute(new Runnable {
      override def run(): Unit = {
        try {
          while (true) {
            val delivery = consumer.nextDelivery()
            val header =
              if (delivery.getProperties.getHeaders != null) {
                delivery.getProperties.getHeaders.map {
                  header =>
                    header._1 -> header._2.toString
                }.toMap
              } else {
                Map[String, String]()
              }
            val message = new String(delivery.getBody(), "UTF-8")
            receivedFun(message, header)
          }
        } catch {
          case e: ShutdownSignalException =>
          case e: Throwable => e.printStackTrace()
        }
      }
    })
    channel.basicConsume(address, true, consumer)
  }

  def ack(address: String, message: String, args: Map[String, String] = Map(), timeout: Long = 30 * 1000, exchangeName: String = defaultRPCExchangeName): (String, Map[String, String]) = {
    val channel = getChannel()
    channel.exchangeDeclare(exchangeName, "direct")
    channel.queueDeclare(address, false, false, false, null)
    val replyQueueName = channel.queueDeclare().getQueue
    val consumer = new QueueingConsumer(channel)
    channel.basicConsume(replyQueueName, true, consumer)
    val corrId = java.util.UUID.randomUUID().toString
    val opt = new BasicProperties
    .Builder()
      .correlationId(corrId)
      .headers(args)
      .replyTo(replyQueueName)
      .build()
    channel.basicPublish("", address, opt, message.getBytes("UTF-8"))
    var replyMessage: String = null
    var replyHeader: Map[String, String] = null
    var hasReply = false
    try {
      while (!hasReply) {
        val delivery = consumer.nextDelivery(timeout)
        if (delivery != null) {
          if (delivery.getProperties.getCorrelationId.equals(corrId)) {
            hasReply = true
            replyHeader =
              if (delivery.getProperties.getHeaders != null) {
                delivery.getProperties.getHeaders.map {
                  header =>
                    header._1 -> header._2.toString
                }.toMap
              } else {
                Map[String, String]()
              }
            replyMessage = new String(delivery.getBody, "UTF-8")
            channel.close()
          }
        } else {
          channel.close()
          throw new TimeoutException("RabbitMQ ack timeout")
        }
      }
    } catch {
      case e: ShutdownSignalException =>
      case e:AlreadyClosedException =>
      case e: Throwable => e.printStackTrace()
    }
    (replyMessage, replyHeader)
  }

  def ackAsync(address: String, message: String, args: Map[String, String] = Map(), timeout: Long = 30 * 1000, exchangeName: String = defaultRPCExchangeName)(replyFun: => (String, Map[String, String]) => Unit): Unit = {
    val channel = getChannel()
    channel.exchangeDeclare(exchangeName, "direct")
    channel.queueDeclare(address, false, false, false, null)
    val replyQueueName = channel.queueDeclare().getQueue
    val consumer = new QueueingConsumer(channel)
    channel.basicConsume(replyQueueName, true, consumer)
    val corrId = java.util.UUID.randomUUID().toString
    val opt = new BasicProperties
    .Builder()
      .correlationId(corrId)
      .headers(args)
      .replyTo(replyQueueName)
      .build()
    channel.basicPublish("", address, opt, message.getBytes("UTF-8"))
    EZ.execute.execute(new Runnable {
      override def run(): Unit = {
        var replyMessage: String = null
        var replyHeader: Map[String, String] = null
        var hasReply = false
        try {
          while (!hasReply) {
            val delivery = consumer.nextDelivery(timeout)
            if (delivery != null) {
              if (delivery.getProperties.getCorrelationId.equals(corrId)) {
                hasReply = true
                replyHeader =
                  if (delivery.getProperties.getHeaders != null) {
                    delivery.getProperties.getHeaders.map {
                      header =>
                        header._1 -> header._2.toString
                    }.toMap
                  } else {
                    Map[String, String]()
                  }
                replyMessage = new String(delivery.getBody, "UTF-8")
                channel.close()
              }
            } else {
              channel.close()
              throw new TimeoutException("RabbitMQ ack timeout")
            }
          }
        } catch {
          case e: ShutdownSignalException =>
          case e:AlreadyClosedException =>
          case e: Throwable => e.printStackTrace()
        }
        replyFun(replyMessage, replyHeader)
      }
    })
  }

  def reply(address: String, exchangeName: String = defaultRPCExchangeName)(receivedFun: (String, Map[String, String]) => (String, Map[String, String])): Unit = {
    val channel = getChannel()
    channel.exchangeDeclare(exchangeName, "direct")
    channel.queueDeclare(address, false, false, false, null)
    channel.queueBind(address, exchangeName, address)
    val consumer = new QueueingConsumer(channel)
    EZ.execute.execute(new Runnable {
      override def run(): Unit = {
        try {
          while (true) {
            val delivery = consumer.nextDelivery()
            val props = delivery.getProperties()
            val header =
              if (props.getHeaders != null) {
                props.getHeaders.map {
                  header =>
                    header._1 -> header._2.toString
                }.toMap
              } else {
                Map[String, String]()
              }
            val message = new String(delivery.getBody(), "UTF-8")
            EZ.newThread {
              val result = receivedFun(message, header)
              channel.basicPublish("", props.getReplyTo(), new BasicProperties
              .Builder()
                .headers(result._2)
                .correlationId(props.getCorrelationId())
                .build(), result._1.getBytes("UTF-8"))
            }
          }
        } catch {
          case e: ShutdownSignalException =>
          case e: Throwable => e.printStackTrace()
        }
      }
    })
    channel.basicConsume(address, true, consumer)
  }

  def replyAsync(address: String, exchangeName: String = defaultRPCExchangeName)(receivedFun: (String, Map[String, String]) => Future[(String, Map[String, String])]): Unit = {
    val channel = getChannel()
    channel.exchangeDeclare(exchangeName, "direct")
    channel.queueDeclare(address, false, false, false, null)
    channel.queueBind(address, exchangeName, address)
    val consumer = new QueueingConsumer(channel)
    EZ.execute.execute(new Runnable {
      override def run(): Unit = {
        try {
          while (true) {
            val delivery = consumer.nextDelivery()
            val props = delivery.getProperties()
            val header =
              if (props.getHeaders != null) {
                props.getHeaders.map {
                  header =>
                    header._1 -> header._2.toString
                }.toMap
              } else {
                Map[String, String]()
              }
            val message = new String(delivery.getBody(), "UTF-8")
            EZ.newThread {
              receivedFun(message, header).onSuccess {
                case result =>
                  channel.basicPublish("", props.getReplyTo(), new BasicProperties
                  .Builder()
                    .headers(result._2)
                    .correlationId(props.getCorrelationId())
                    .build(), result._1.getBytes("UTF-8"))
              }
            }
          }
        } catch {
          case e: ShutdownSignalException =>
          case e: Throwable => e.printStackTrace()
        }
      }
    })
    channel.basicConsume(address, true, consumer)
  }

  def close(): Unit = {
    closeChannel()
    conn.close()
  }

}
