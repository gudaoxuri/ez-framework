package com.asto.es.framework.function

import java.util.concurrent.CountDownLatch

import com.asto.ez.framework.service.scheduler.{SchedulerService, EZ_Scheduler, ScheduleJob}

class ScheduleSpec extends DBBasicSpec {

  test("Schedule Test"){

    val cdl = new CountDownLatch(1)

    SchedulerService.init()
    val scheduler=EZ_Scheduler()
    scheduler.name="测试"
    scheduler.cron="* * * * * ?"
    scheduler.clazz=TestScheduleJob.getClass.getName
    scheduler.parameters=Map("p1"->1,"p2"->"1")
    SchedulerService.save(scheduler)

    cdl.await()

  }
}

object TestScheduleJob extends ScheduleJob{

  override def execute(scheduler: EZ_Scheduler): Unit = {

    assert(scheduler.clazz ==TestScheduleJob.getClass.getName)
    assert(scheduler.parameters("p1") ==1)
    assert(scheduler.parameters("p2") =="1")

  }
}
