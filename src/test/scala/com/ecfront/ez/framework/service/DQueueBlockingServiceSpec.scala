package com.ecfront.ez.framework.service

import com.ecfront.ez.framework.service.common.DBlockingQueueService


class DQueueBlockingServiceSpec extends BasicSpec {

  test("DBlockingQueue测试") {

    val queue = DBlockingQueueService[TestModel]("test_queue")
    queue.delete()

    val model1 = TestModel()
    model1.name = "张三"
    model1.bool = true
    model1.age = 14
    model1.id = "id001"
    queue.put(model1)
    val model2 = TestModel()
    model2.name = "李四"
    model2.bool = true
    model2.age = 14
    model2.id = "id001"
    queue.put(model2)

    assert(queue.size() == 2)
    assert(queue.peek().name == "张三")
    assert(queue.peek().name == "张三")
    assert(queue.size() == 2)
    assert(queue.take().name == "张三")
    assert(queue.size() == 1)
    assert(queue.take().name == "李四")
    assert(queue.size() == 0)

    new Thread(new Runnable {
      override def run(): Unit = {
        Thread.sleep(1000)
        println("-------add-------")
        val model1 = TestModel()
        model1.name = "New1"
        queue.put(model1)
        val model2 = TestModel()
        model2.name = "New2"
        queue.put(model2)
        val model3 = TestModel()
        model3.name = "New3"
        queue.put(model3)
        val model4 = TestModel()
        model4.name = "New4"
        queue.put(model4)
      }
    }).start()

    assert(queue.peek() == null)
    println("-------empty-------")

    for (i <- 1 to 4) {
      val t = new Thread(new Runnable {
        override def run(): Unit = {
          println(Thread.currentThread().getId + " -> " + queue.take().name)
        }
      })
      t.start()
      t.join()
    }

  }

}





