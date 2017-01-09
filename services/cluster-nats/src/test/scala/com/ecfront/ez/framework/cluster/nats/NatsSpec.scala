package com.ecfront.ez.framework.cluster.nats

import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicLong

import com.ecfront.ez.framework.test.BasicSpec
import io.nats.client.{ConnectionFactory, Message, MessageHandler}

class NatsSpec extends BasicSpec {

  test("nats test") {
    new Thread(new Runnable() {
      override def run() = {
        val cf = new ConnectionFactory()
        cf.setServers(Array("nats://127.0.0.1:4222"))
        val connection = cf.createConnection()
        connection.subscribe("/test/", "111",new MessageHandler {
          override def onMessage(msg: Message) = {
            logger.info(">>" + new String(msg.getData, "UTF-8"))
            connection.publish(msg.getReplyTo, (new String(msg.getData, "UTF-8") + "_reply").getBytes("UTF-8"))
          }
        })
        new CountDownLatch(1).await()
      }
    }).start()
    new Thread(new Runnable() {
      override def run() = {
        val cf = new ConnectionFactory()
        cf.setServers(Array("nats://127.0.0.1:4222"))
        val connection = cf.createConnection()
        connection.subscribe("/test/","111", new MessageHandler {
          override def onMessage(msg: Message) = {
            logger.info(">>" + new String(msg.getData, "UTF-8"))
            connection.publish(msg.getReplyTo, (new String(msg.getData, "UTF-8") + "_reply").getBytes("UTF-8"))
          }
        })
        new CountDownLatch(1).await()
      }
    }).start()
    new Thread(new Runnable() {
      override def run() = {
        val cf = new ConnectionFactory()
        cf.setServers(Array("nats://127.0.0.1:4222"))
        val connection = cf.createConnection()
        connection.subscribe("/test/11/","111", new MessageHandler {
          override def onMessage(msg: Message) = {
            logger.info(">>" + new String(msg.getData, "UTF-8"))
            connection.publish(msg.getReplyTo, (new String(msg.getData, "UTF-8") + "_reply").getBytes("UTF-8"))
          }
        })
        new CountDownLatch(1).await()
      }
    }).start()

    val counter = new AtomicLong(0)
    new Thread(new Runnable() {
      override def run() = {
        val cf = new ConnectionFactory()
        cf.setServers(Array("nats://127.0.0.1:4222"))
        val connection = cf.createConnection()
        while (true) {
          val reply = connection.request("/test/", s"A test_mesage ${counter.getAndIncrement()}".getBytes("UTF-8"))
          logger.info("A<<" + new String(reply.getData, "UTF-8"))
        }
        new CountDownLatch(1).await()
      }
    }).start()

    new Thread(new Runnable() {
      override def run() = {
        val cf = new ConnectionFactory()
        cf.setServers(Array("nats://127.0.0.1:4222"))
        val connection = cf.createConnection()
        while (true) {
          var reply = connection.request("/test/", s"B test_mesage ${counter.getAndIncrement()}".getBytes("UTF-8"))
          logger.info("B<<" + new String(reply.getData, "UTF-8"))
          reply = connection.request("/test/11/", s"B msg ${counter.getAndIncrement()}".getBytes("UTF-8"))
          logger.info("B<<" + new String(reply.getData, "UTF-8"))
        }
        new CountDownLatch(1).await()
      }
    }).start()

    new CountDownLatch(1).await()
  }

}