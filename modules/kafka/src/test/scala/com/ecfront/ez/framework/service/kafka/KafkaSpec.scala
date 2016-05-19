package com.ecfront.ez.framework.service.kafka

import java.util.concurrent.CountDownLatch

import com.ecfront.common.Resp
import com.ecfront.ez.framework.core.test.MockStartupSpec

class KafkaSpec extends MockStartupSpec {

  test("Kafka test") {

    val counter = new CountDownLatch(1)

    val consumer = KafkaProcessor.Consumer("test", "group1")
    consumer.receive({
      (message, messageId) =>
        println(message)
        counter.countDown()
        Resp.success(null)

    })
    val producer = KafkaProcessor.Producer("test", "client1")

    producer.send("haha...")

    Thread.sleep(10000)
    counter.await()
  }

  test("Kafka ack test") {

    val counter = new CountDownLatch(1)

    val consumer = KafkaProcessor.Consumer("req1", "group1")
    consumer.receive({
      (message, messageId) =>
        println(message)
        Thread.sleep(10000)
        counter.countDown()
        Resp.success("pong...")
    }, "resp1")
    val producer = KafkaProcessor.Producer("req1", "client1")

    assert(producer.ack("ping...", "resp1").body == "pong...")

    assert(producer.ack("ping...", "resp1", 5000).message == "Timeout")

    counter.await()
  }

}


