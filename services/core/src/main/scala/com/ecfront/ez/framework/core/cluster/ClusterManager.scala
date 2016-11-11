package com.ecfront.ez.framework.core.cluster

import com.ecfront.common.Resp
import com.rabbitmq.client.AMQP.BasicProperties
import com.rabbitmq.client._

import scala.collection.JavaConversions._

object ClusterManager {

  private var conn: Connection = _
  private var channel: Channel = _

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
    channel = conn.createChannel()
    sys.addShutdownHook {
      close()
    }
    Resp.success(null)
  }

  def publish(topic: String, message: String, args: Map[String, String], exchangeName: String = defaultTopicExchangeName): Unit = {
    channel.exchangeDeclare(exchangeName, "topic")
    val opt = new AMQP.BasicProperties.Builder().headers(args).build()
    channel.basicPublish(exchangeName, topic, opt, message.getBytes())
  }

  def subscribe(topic: String, exchangeName: String = defaultTopicExchangeName)(receivedFun: (String, Map[String, String]) => Unit): Unit = {
    channel.exchangeDeclare(exchangeName, "topic")
    val queueName = channel.queueDeclare().getQueue()
    channel.queueBind(queueName, exchangeName, topic)
    val consumer = new DefaultConsumer(channel) {
      override def handleDelivery(consumerTag: String, envelope: Envelope, properties: BasicProperties, body: Array[Byte]): Unit = {
        val message = new String(body, "UTF-8")
        receivedFun(message, properties.getHeaders.map {
          header =>
            header._1 -> header._2.toString
        }.toMap)
      }
    }
    channel.basicConsume(queueName, true, consumer)
  }

  def request(queueName: String, message: String, args: Map[String, String] = Map(), exchangeName: String = defaultQueueExchangeName): Unit = {
    val opt = new AMQP.BasicProperties.Builder().headers(args).build()
    channel.basicPublish(exchangeName, queueName, opt, message.getBytes())
  }

  def response(queueName: String)(receivedFun: (String, Map[String, String]) => Unit): Unit = {
    val consumer = new DefaultConsumer(channel) {
      override def handleDelivery(consumerTag: String, envelope: Envelope, properties: BasicProperties, body: Array[Byte]): Unit = {
        val message = new String(body, "UTF-8")
        receivedFun(message, properties.getHeaders.map {
          header =>
            header._1 -> header._2.toString
        }.toMap)
      }
    }
    channel.basicConsume(queueName, true, consumer)
  }

  def ack(address: String, message: String, args: Map[String, String] = Map(), timeout: Long = 30 * 1000): (String, Map[String, String]) = {
    val replyQueueName = channel.queueDeclare().getQueue
    val consumer = new QueueingConsumer(channel)
    channel.basicConsume(replyQueueName, true, consumer)
    var message: String = _
    var header: Map[String, String] = _
    val corrId = java.util.UUID.randomUUID().toString
    val opt = new BasicProperties
    .Builder()
      .correlationId(corrId)
      .headers(args)
      .replyTo(replyQueueName)
      .build()
    channel.basicPublish("", address, opt, message.getBytes())
    while (true) {
      val delivery = consumer.nextDelivery()
      if (delivery.getProperties.getCorrelationId.equals(corrId)) {
        header = delivery.getProperties.getHeaders.map {
          header =>
            header._1 -> header._2.toString
        }.toMap
        message = new String(delivery.getBody)
      }
    }
    (message, header)
  }

  def reply(address: String)(receivedFun: (String, Map[String, String]) => (String, Map[String, String])): Unit = {
    channel.queueDeclare(address, false, false, false, null)
    channel.basicQos(1)
    val consumer = new QueueingConsumer(channel)
    channel.basicConsume(address, false, consumer)
    while (true) {
      val delivery = consumer.nextDelivery()
      val props = delivery.getProperties()
      val message = new String(delivery.getBody())
      val result = receivedFun(message, delivery.getProperties.getHeaders.map {
        header =>
          header._1 -> header._2.toString
      }.toMap)
      channel.basicPublish("", props.getReplyTo(), new BasicProperties
      .Builder()
        .headers(result._2)
        .correlationId(props.getCorrelationId())
        .build(), result._1.getBytes)
      channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false)
    }
  }

  def close(): Unit = {
    channel.close()
    conn.close()
  }

}
