package com.ecfront.ez.framework.service.scheduler

import java.util.concurrent.CountDownLatch

import com.ecfront.common.Resp
import com.ecfront.ez.framework.test.MockStartupSpec

class ScheduleSpec extends MockStartupSpec {

  test("Schedule Test") {

    val cdl = new CountDownLatch(1)

    SchedulerProcessor.delete("测试")

    var scheduler = EZ_Scheduler()
    scheduler.name = "测试"
    scheduler.cron = "* * * * * ?"
    scheduler.module = "scheduler"
    scheduler.clazz = TestScheduleJob.getClass.getName
    scheduler.parameters = Map("p1" -> 1, "p2" -> "1")
    scheduler = SchedulerProcessor.save(scheduler)

    scheduler.parameters = Map("p1" -> 22222, "p2" -> "1")
    SchedulerProcessor.update(scheduler)

    Thread.sleep(10000)

    SchedulerProcessor.delete("测试")

    val pageLogs = SchedulerProcessor.pageLogsByName("测试", 1, 10)
    assert(pageLogs.recordTotal > 0)

    cdl.await()

  }

}

object TestScheduleJob extends ScheduleJob {

  override def execute(scheduler: EZ_Scheduler): Resp[Void] = {

    assert(scheduler.clazz == TestScheduleJob.getClass.getName)
    println(">>>>" + scheduler.parameters)

    Resp.success(null)
  }
}
