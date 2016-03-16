package com.ecfront.ez.framework.service.distributed

import com.ecfront.ez.framework.core.test.MockStartupSpec


class DQueueServiceSpec extends MockStartupSpec {

  test("DQueue测试") {

    val queue = DQueueService[TestModel]("test_queue")
    queue.delete()

    val model1 = TestModel()
    model1.name = "张三"
    model1.bool = true
    model1.age = 14
    model1.id = "id001"
    queue.add(model1)
    val model2 = TestModel()
    model2.name = "李四"
    model2.bool = true
    model2.age = 14
    model2.id = "id001"
    queue.add(model2)

    assert(queue.size() == 2)
    assert(queue.peek().name == "张三")
    assert(queue.peek().name == "张三")
    assert(queue.poll().name == "张三")
    assert(queue.poll().name == "李四")
    assert(queue.size() == 0)

  }

}





