package com.ecfront.ez.framework.core.eventbus

import java.util.concurrent.CountDownLatch

import com.ecfront.ez.framework.core.{EZ, MockStartupSpec}

import scala.beans.BeanProperty

class EventBusSpec extends MockStartupSpec {

  test("EventBus测试") {
    // pub-sub
    var counter = new CountDownLatch(7)
    EZ.eb.subscribe[String]("a") {
      (message, _) =>
        counter.countDown()
        logger.info(">>>>>>>>>>>>>>>>>>>> sub")
        assert(message == "abc")
    }
    EZ.eb.publish("a", "abc")
    new Thread(new Runnable {
      override def run(): Unit = {
        EZ.eb.subscribe[TestObj]("aa") {
          (message, _) =>
            counter.countDown()
            logger.info(">>>>>>>>>>>>>>>>>>>> sub1")
            assert(message.f1 == "字段1" && message.f2 == 0.1)
        }
      }
    }).start()
    new Thread(new Runnable {
      override def run(): Unit = {
        EZ.eb.subscribe[TestObj]("aa") {
          (message, _) =>
            counter.countDown()
            logger.info(">>>>>>>>>>>>>>>>>>>> sub2")
            assert(message.f1 == "字段1" && message.f2 == 0.1)
        }
      }
    }).start()
    new Thread(new Runnable {
      override def run(): Unit = {
        EZ.eb.subscribe[TestObj]("aa") {
          (message, _) =>
            counter.countDown()
            logger.info(">>>>>>>>>>>>>>>>>>>> sub3")
            assert(message.f1 == "字段1" && message.f2 == 0.1)
        }
      }
    }).start()
    EZ.eb.publish("aa", TestObj("字段1", 0.1))
    EZ.eb.publish("aa", TestObj("字段1", 0.1))
    counter.await()

    // req-resp
    counter = new CountDownLatch(100)
    new Thread(new Runnable {
      override def run(): Unit = {
        EZ.eb.response[TestObj]("bb") {
          (message, _) =>
            logger.info(">>>>>>>>>>>>>>>>>>>> resp" + message.f2)
            assert(message.f1 == "字段1")
            counter.countDown()
        }
      }
    }).start()
    new Thread(new Runnable {
      override def run(): Unit = {
        EZ.eb.response[TestObj]("bb") {
          (message, _) =>
            logger.info(">>>>>>>>>>>>>>>>>>>> resp" + message.f2)
            assert(message.f1 == "字段1")
            counter.countDown()
        }
      }
    }).start()
    for (i <- 0 to 99) {
      EZ.eb.request("bb", TestObj("字段1", i))
    }
    counter.await()

    // ack
    new Thread(new Runnable {
      override def run(): Unit = {
        EZ.eb.reply[String]("test") {
          (message, _) =>
            (message, Map())
        }
      }
    }).start()
    new Thread(new Runnable {
      override def run(): Unit = {
        while (true) {
          val result = EZ.eb.ack[String]("test", "a")
          assert(result._1 == "a")
        }
      }
    }).start()
    new Thread(new Runnable {
      override def run(): Unit = {
        while (true) {
          assert(EZ.eb.ack[String]("test", "b")._1 == "b")
        }
      }
    }).start()

    new CountDownLatch(1).await()
  }

}

class TestObj {
  @BeanProperty var f1: String = _
  @BeanProperty var f2: BigDecimal = _
}

object TestObj {
  def apply(f1: String, f2: BigDecimal): TestObj = {
    val obj = new TestObj()
    obj.f1 = f1
    obj.f2 = f2
    obj
  }
}





