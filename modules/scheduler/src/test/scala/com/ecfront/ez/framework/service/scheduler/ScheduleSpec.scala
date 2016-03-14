package com.ecfront.ez.framework.service.scheduler

import java.util.concurrent.CountDownLatch

import com.ecfront.common.Resp
import com.ecfront.ez.framework.core.test.MockStartupSpec

class ScheduleSpec extends MockStartupSpec {

  test("Schedule Test") {

    val cdl = new CountDownLatch(1)

    val scheduler = EZ_Scheduler()
    scheduler.name = "测试"
    scheduler.cron = "* * * * * ?"
    scheduler.module = "testModule1"
    scheduler.clazz = TestScheduleJob.getClass.getName
    scheduler.parameters = Map("p1" -> 1, "p2" -> "1")
    SchedulerProcessor.save(scheduler)

    cdl.await()

  }

}

object TestScheduleJob extends ScheduleJob {

  override def execute(scheduler: EZ_Scheduler): Resp[Void] = {

    assert(scheduler.clazz == TestScheduleJob.getClass.getName)
    assert(scheduler.parameters("p1") == 1)
    assert(scheduler.parameters("p2") == "1")

    Resp.success(null)
  }
}
