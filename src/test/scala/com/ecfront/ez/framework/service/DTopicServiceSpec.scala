package com.ecfront.ez.framework.service

import java.util.concurrent.CountDownLatch

import com.ecfront.ez.framework.BasicSpec
import com.ecfront.ez.framework.service.common.DTopicService

import scala.collection.mutable.ArrayBuffer


class DTopicServiceSpec extends BasicSpec {

  test("DTopic测试") {

    val result = ArrayBuffer[String]()
    val c = new CountDownLatch(4)

    val topic = DTopicService[TestModel]("test_topic")
    topic.delete()

    topic.subscribe({
      msg =>
        println("Subscribe..." + msg.name)
        result += msg.name
        c.countDown()
    })

    topic.subscribe({
      msg =>
        println("Subscribe..." + msg.name)
        result += msg.name
        c.countDown()
    })

    val model1 = TestModel()
    model1.name = "张三"
    model1.bool = true
    model1.age = 14
    model1.id = "id001"
    topic.publish(model1)

    val model2 = TestModel()
    model2.name = "李四"
    model2.bool = true
    model2.age = 14
    model2.id = "id001"
    topic.publish(model2)

    c.await()

    assert(result.size == 4)
    assert(result.contains("李四"))
    assert(result.contains("张三"))

  }

}





