
package com.ecfront.ez.framework.core.rabbitmq

import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicLong

import com.ecfront.ez.framework.core.logger.Logging
import com.rabbitmq.client.AMQP.BasicProperties
import com.rabbitmq.client.{ConnectionFactory, QueueingConsumer}
import org.scalatest.{BeforeAndAfter, FunSuite}


class RabbitMQSpec extends FunSuite with BeforeAndAfter with Logging {

  test("rabbitmq test") {
    val p = new AtomicLong(0)
    val c = new AtomicLong(0)

    val factory = new ConnectionFactory()
    factory.setUsername("user")
    factory.setPassword("password")
    factory.setHost("127.0.0.1")
    val connection = factory.newConnection()
    // produce
    val produceThreads = for (i <- 0 until 50)
      yield new Thread(new Runnable {
        override def run(): Unit = {
          val channel = connection.createChannel()
          val replyQueueName = channel.queueDeclare().getQueue
          val replyConsumer = new QueueingConsumer(channel)
          channel.basicConsume(replyQueueName, true, replyConsumer)
          val corrId = java.util.UUID.randomUUID().toString
          val opt = new BasicProperties.Builder().correlationId(corrId).replyTo(replyQueueName).build()
          channel.basicPublish("", "a", opt, s"test${p.incrementAndGet()}".getBytes())
          var delivery = replyConsumer.nextDelivery()
          while (true) {
            if (delivery.getProperties.getCorrelationId.equals(corrId)) {
              logger.info(s"reply " + new String(delivery.getBody))
            }
            delivery = replyConsumer.nextDelivery()
          }
          channel.close()
        }
      })
    produceThreads.foreach(_.start())

    // consumer
    new Thread(new Runnable {
      override def run(): Unit = {
        val channel = connection.createChannel()
        channel.queueDeclare("a", false, false, false, null)
        val consumer = new QueueingConsumer(channel)
        channel.basicConsume("a", true, consumer)
        while (true) {
          val delivery = consumer.nextDelivery()
          val props = delivery.getProperties()
          val message = new String(delivery.getBody())
          new Thread(new Runnable {
            override def run(): Unit = {
              Thread.sleep(10000)
              logger.info(s"receive 1 [${c.incrementAndGet()}] " + message)
              channel.basicPublish("", props.getReplyTo(), new BasicProperties.Builder().correlationId(props.getCorrelationId()).build(), message.getBytes)
            }
          }).start()
        }
      }
    }).start()
   /* new Thread(new Runnable {
      override def run(): Unit = {
        val channel = connection.createChannel()
        channel.queueDeclare("a", false, false, false, null)
        val consumer = new QueueingConsumer(channel)
        channel.basicConsume("a", true, consumer)
        while (true) {
          val delivery = consumer.nextDelivery()
          val props = delivery.getProperties()
          val message = new String(delivery.getBody())
          logger.info(s"receive 2 [${c.incrementAndGet()}] " + message)
          channel.basicPublish("", props.getReplyTo(), new BasicProperties.Builder().correlationId(props.getCorrelationId()).build(), message.getBytes)
        }
      }
    }).start()*/

    new CountDownLatch(1).await()
  }
}

