package com.ecfront.ez.framework.service

import com.ecfront.common.JsonHelper
import com.ecfront.ez.framework.BasicSpec
import com.ecfront.ez.framework.module.schedule.{EZTask, ScheduleService}


class ScheduleServiceSpec extends BasicSpec {

  test("Schedule测试") {
    val task1Id = ScheduleService.registerTask("task1", "group1", TestTask1.getClass, 0, 1000, Map[String, Any]("name" -> "task1", "a1" -> 0))
    val task2Id = ScheduleService.registerTask("task2", "group1", classOf[TestTask2], 0, 1000, Map[String, Any]())
    assert(ScheduleService.existTask("task2", "group1"))
    assert(!ScheduleService.existTask("task3", "group1"))
    Thread.sleep(5000)
    ScheduleService.disableTask(task2Id)
    Thread.sleep(5000)
    ScheduleService.enableTask(task2Id)
    Thread.sleep(5000)
    ScheduleService.unRegisterTask(task2Id)
    Thread.sleep(5000)
  }


}

object TestTask1 extends EZTask {

  override def execute(parameters: Map[String, Any]): Unit = {
    println("task1:" + JsonHelper.toJsonString(parameters))
    Thread.sleep(5000)
  }

}

class TestTask2 extends EZTask {

  override def execute(parameters: Map[String, Any]): Unit = {
    println("task2:" + JsonHelper.toJsonString(parameters))
  }

}





