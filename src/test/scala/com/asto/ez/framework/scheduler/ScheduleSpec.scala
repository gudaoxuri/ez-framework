package com.asto.ez.framework.scheduler

import java.util.concurrent.CountDownLatch

import com.asto.ez.framework.storage.StroageSpec

class ScheduleSpec extends StroageSpec {

  test("JDBC Schedule Test") {

    val cdl = new CountDownLatch(1)

    SchedulerService.init("testModule1", JDBC_EZ_Scheduler)
    val scheduler = EZ_Scheduler()
    scheduler.name = "测试"
    scheduler.cron = "* * * * * ?"
    scheduler.module = "testModule1"
    scheduler.clazz = TestScheduleJob.getClass.getName
    scheduler.parameters = Map("p1" -> 1, "p2" -> "1")
    SchedulerService.save(scheduler)

    cdl.await()

  }

  test("Mongo Schedule Test") {

    val cdl = new CountDownLatch(1)

    SchedulerService.init("testModule1", Mongo_EZ_Scheduler)
    val scheduler = EZ_Scheduler()
    scheduler.name = "测试"
    scheduler.cron = "* * * * * ?"
    scheduler.module = "testModule1"
    scheduler.clazz = TestScheduleJob.getClass.getName
    scheduler.parameters = Map("p1" -> 1, "p2" -> "1")
    SchedulerService.save(scheduler)

    cdl.await()

  }

}

object TestScheduleJob extends ScheduleJob {

  override def execute(scheduler: EZ_Scheduler): Unit = {

    assert(scheduler.clazz == TestScheduleJob.getClass.getName)
    assert(scheduler.parameters("p1") == 1)
    assert(scheduler.parameters("p2") == "1")

  }
}
