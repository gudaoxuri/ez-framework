package com.ecfront.ez.framework.service.eventbus

import java.util.concurrent.CountDownLatch

import com.ecfront.ez.framework.core.test.MockStartupSpec


class EventBusProcessorSpec extends MockStartupSpec {

  test("EventBus测试") {

    EventBusProcessor.publish("a", "abc")
    assert(EventBusProcessor.consumer[String]("a")._1 == "abc")

    val counter = new CountDownLatch(1)
    new Thread(new Runnable {
      override def run(): Unit = {
        counter.countDown()
        assert(EventBusProcessor.consumer[String]("b")._1 == "456")
      }
    }).start()
    counter.await()
    Thread.sleep(100)
    EventBusProcessor.send("b", "456")

    new Thread(new Runnable {
      override def run(): Unit = {
        while (true) {
          EventBusProcessor.Async.consumerAdv[String]("c", {
            (message, reply) =>
              println(">>>>" + message)
              reply("pong")
          })
        }
      }
    }).start()
    new Thread(new Runnable {
      override def run(): Unit = {
        while (true) {
          EventBusProcessor.Async.sendAdv[String]("c", "ping", {
            (message, reply) =>
              println("<<<<" + message)
              Thread.sleep(60000)
              reply("ping")
          })
        }
      }
    }).start()
    new CountDownLatch(1).await()
  }

}





