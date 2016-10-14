package com.ecfront.ez.framework.core.eventbus

import java.util.concurrent.CountDownLatch

import com.ecfront.ez.framework.core.{EZ, MockStartupSpec}

import scala.beans.BeanProperty

class EventBusSpec extends MockStartupSpec {

  test("EventBus测试") {
    // pub-sub
    var counter = new CountDownLatch(3)
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
            logger.info(">>>>>>>>>>>>>>>>>>>> sub")
            assert(message.f1 == "字段1" && message.f2 == 0.1)
        }
      }
    }).start()
    new Thread(new Runnable {
      override def run(): Unit = {
        EZ.eb.subscribe[TestObj]("aa") {
          (message, _) =>
            counter.countDown()
            logger.info(">>>>>>>>>>>>>>>>>>>> sub")
            assert(message.f1 == "字段1" && message.f2 == 0.1)
        }
      }
    }).start()
    EZ.eb.publish("aa", TestObj("字段1", 0.1))
    counter.await()

    // req-resp
    counter = new CountDownLatch(3)
    EZ.eb.response[String]("b") {
      (message, _) =>
        assert(message == "456")
        counter.countDown()
    }
    EZ.eb.request("b", "456")
    EZ.eb.request("bb", TestObj("字段1", 0.1))
    Thread.sleep(100)
    EZ.eb.request("bb", TestObj("字段1", 0.2))
    var executingMessages = EZ.cache.hgetAll(s"ez:eb:executing:${EZ.eb.packageAddress("REQ_RESP", "bb")}")
    assert(executingMessages.size == 2 && executingMessages.head._2 == """{"f1":"字段1","f2":0.2}""")
    new Thread(new Runnable {
      override def run(): Unit = {
        EZ.eb.response[TestObj]("bb") {
          (message, _) =>
            logger.info(">>>>>>>>>>>>>>>>>>>> resp")
            assert(message.f1 == "字段1")
            counter.countDown()
        }
      }
    }).start()
    new Thread(new Runnable {
      override def run(): Unit = {
        EZ.eb.response[TestObj]("bb") {
          (message, _) =>
            logger.info(">>>>>>>>>>>>>>>>>>>> resp")
            assert(message.f1 == "字段1")
            counter.countDown()
        }
      }
    }).start()
    counter.await()
    executingMessages = EZ.cache.hgetAll("ez:eb:executing:bb")
    assert(executingMessages.isEmpty)

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
          assert(EZ.eb.ack[String]("test", "a")._1 == "a")
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





