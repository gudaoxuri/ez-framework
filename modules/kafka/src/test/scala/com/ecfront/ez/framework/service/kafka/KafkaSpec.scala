package com.ecfront.ez.framework.service.kafka

import java.util.concurrent.CountDownLatch

import com.ecfront.common.Resp
import com.ecfront.ez.framework.core.test.MockStartupSpec

class KafkaSpec extends MockStartupSpec {

  test("Kafka test") {

    val counter = new CountDownLatch(1)

    val consumer = KafkaProcessor.Consumer("group1", "test")
    consumer.receive(new ReceivedCallback {
      override def callback(message: String): Resp[Void] = {
        println(message)
        counter.countDown()
        Resp.success(null)
      }
    })
    val producer = KafkaProcessor.Producer("test", "client1")

    producer.send("haha...")

    counter.await()
  }

}


