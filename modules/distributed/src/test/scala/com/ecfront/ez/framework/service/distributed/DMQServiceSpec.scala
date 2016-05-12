package com.ecfront.ez.framework.service.distributed

import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicLong

import com.ecfront.ez.framework.core.test.MockStartupSpec
import com.ecfront.ez.framework.service.redis.RedisProcessor

import scala.collection.mutable.ArrayBuffer


class DMQServiceSpec extends MockStartupSpec {

  test("DTopic测试") {

    val result = ArrayBuffer[String]()
    val c = new CountDownLatch(4)

    val mq = DMQService[TestModel]("test_topic")
    mq.delete()

    mq.subscribe({
      msg =>
        println("Subscribe1..." + msg.name)
        result += msg.name
        c.countDown()
    })

    mq.subscribe({
      msg =>
        println("Subscribe2..." + msg.name)
        result += msg.name
        c.countDown()
    })

    val model1 = TestModel()
    model1.name = "张三"
    model1.bool = true
    model1.age = 14
    model1.id = "id001"
    mq.publish(model1)

    val model2 = TestModel()
    model2.name = "李四"
    model2.bool = true
    model2.age = 14
    model2.id = "id001"
    mq.publish(model2)

    c.await()

    assert(result.size == 4)
    assert(result.contains("李四"))
    assert(result.contains("张三"))

  }

  test("DTopic subscribeOneNode 测试") {

    val mq = DMQService[TestModel]("test_topic24")

    mq.subscribeOneNode({
      msg =>
        println(Thread.currentThread().getId + " subscribe..." + msg.name)
    })

    new CountDownLatch(1).await()

  }

  test("DTopic publish 测试") {

    val mq = DMQService[TestModel]("test_topic24")

    val count = new AtomicLong()
    while (true) {
      val model1 = TestModel()
      model1.name = count.incrementAndGet() + ""
      model1.id = "id001"
      mq.publish(model1)
    }

    new CountDownLatch(1).await()

  }

  test("DTopic point to point 测试") {

    val mq = DMQService[TestModel]("test_topic12")

    val container = ArrayBuffer[Long]()

    for (i <- 1 to 10) {
      new Thread(new Runnable {
        override def run(): Unit = {
          DMQService[TestModel]("test_topic12").receive({
            msg =>
              println(Thread.currentThread().getId + " receive..." + msg.name)
              container += msg.name.toLong
          })
        }
      }).start()
    }

    val count = new AtomicLong()
    for (i <- 1 to 10) {
      new Thread(new Runnable {
        override def run(): Unit = {
          while (true) {
            val model1 = TestModel()
            model1.name = count.incrementAndGet() + ""
            model1.id = "id001"
            mq.send(model1)
          }
        }
      }).start()
    }

    Thread.sleep(10000)

    println("len1=" + container.length)
    println("len2=" + container.distinct.length)

    assert(container.length == container.distinct.length)

    RedisProcessor.redis.shutdown()
  }

}





