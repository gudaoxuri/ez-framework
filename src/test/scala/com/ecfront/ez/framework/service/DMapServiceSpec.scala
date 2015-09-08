package com.ecfront.ez.framework.service

import java.util.concurrent.CountDownLatch
import java.util.{Timer, TimerTask}

import com.ecfront.ez.framework.BasicSpec
import com.ecfront.ez.framework.service.common.DMapService


class DMapServiceSpec extends BasicSpec {

  test("DMap测试1") {

    val map = DMapService[Long]("test_map")
    map.clear()

    val timer = new Timer()
    timer.schedule(new TimerTask {
      override def run(): Unit = {
        map.put("a", System.currentTimeMillis())
      }
    }, 0, 1000)
    timer.schedule(new TimerTask {
      override def run(): Unit = {
        map.foreach({
          (k, v) =>
            println(">>a:" + v)
        })
      }
    }, 0, 10000)
    new CountDownLatch(1).await()
  }

  test("DMap测试2") {

    val map = DMapService[TestModel]("test_model_map")
    map.clear()

    val model = TestModel()
    model.id = "1"
    model.name = "abc"
    model.age = 23
    model.bool = true
    map.put("1", model)
    val newModel = map.get("1")
    assert(newModel.id == "1")
    assert(newModel.name == "abc")
    assert(newModel.age == 23)
    assert(newModel.bool)

  }

}





