package com.ecfront.ez.framework.service.distributed

import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicLong

import com.ecfront.common.Resp
import com.ecfront.ez.framework.core.test.MockStartupSpec
import com.ecfront.ez.framework.service.redis.RedisProcessor
import io.vertx.core.json.JsonObject

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
        Resp.success(null)
    })

    mq.subscribe({
      msg =>
        println("Subscribe2..." + msg.name)
        result += msg.name
        c.countDown()
        Resp.success(null)
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
        Resp.success(null)
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

  test("DTopic point to point 测试2") {
    val counter = new CountDownLatch(1)
    val mq = DMQService[JsonObject]("test_topic134")
    mq.send(new JsonObject().put("1", "1111"))
    mq.receive {
      resp =>
        assert(resp.getString("1") == "1111")
        counter.countDown()
        Resp.success(null)
    }
    counter.await()
  }

  test("DTopic point to point 测试") {

    val mq = DMQService[TestModel]("test_topic123")

    val container = ArrayBuffer[Long]()

    for (i <- 1 to 10) {
      new Thread(new Runnable {
        override def run(): Unit = {
          DMQService[TestModel]("test_topic123").receive({
            msg =>
              println(Thread.currentThread().getId + " receive..." + msg.name)
              container += msg.name.toLong
              Resp.success(null)
          })
        }
      }).start()
    }

    val count = new AtomicLong()
    for (i <- 1 to 10) {
      new Thread(new Runnable {
        override def run(): Unit = {
          while (count.get() <= 10000) {
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

  test("DTopic point to point 错误测试——part1") {
    val counter = new CountDownLatch(2)
    val mq = DMQService[JsonObject]("test_topic1111")
    mq.send(new JsonObject().put("1", "1111"))
    mq.send(new JsonObject().put("2", "2222"))
    mq.receive {
      resp =>
        counter.countDown()
        Resp.badRequest(null)
    }
    counter.await()
  }

  test("DTopic point to point 错误测试——part2") {
    val counter = new CountDownLatch(2)
    val mq = DMQService[JsonObject]("test_topic1111")
    mq.receive {
      resp =>
        counter.countDown()
        Resp.badRequest(null)
    }
    counter.await()
  }

  test("DTopic point to point 错误测试——part3") {
    val counter = new CountDownLatch(2)
    val mq = DMQService[JsonObject]("test_topic1111")
    mq.receive {
      resp =>
        counter.countDown()
        Resp.success(null)
    }
    counter.await()
  }

}





