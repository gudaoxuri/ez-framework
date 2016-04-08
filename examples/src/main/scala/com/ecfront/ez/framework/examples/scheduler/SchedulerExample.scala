package com.ecfront.ez.framework.examples.scheduler

import com.ecfront.common.Resp
import com.ecfront.ez.framework.core.EZContext
import com.ecfront.ez.framework.examples.ExampleStartup
import com.ecfront.ez.framework.service.scheduler.{EZ_Scheduler, ScheduleJob, SchedulerProcessor}


object SchedulerExample extends ExampleStartup {

  override protected def start(): Unit = {

    // 先判断数据库中是否存在调度记录
    if (!EZ_Scheduler.existByModule("测试").body) {
      // 不存在，添加一条记录
      // 调度任务是EZ_Scheduler对象
      val scheduler = EZ_Scheduler()
      scheduler.name = "测试" // 任务名称
      scheduler.cron = "* * * * * ?" // 调度周期
      scheduler.module = EZContext.module // 要调度的模块
      scheduler.clazz = TestScheduleJob.getClass.getName // 任务回调对应的类
      scheduler.parameters = Map("p1" -> 1, "p2" -> "1") // 任务参数
      SchedulerProcessor.save(scheduler) // 保存
    }
  }

}

/**
  * 任务回调类，基于自ScheduleJob
  */
object TestScheduleJob extends ScheduleJob {

  /**
    * 调度器回调时的执行方法
    *
    * @param scheduler 调度信息
    * @return 执行结果
    */
  override def execute(scheduler: EZ_Scheduler): Resp[Void] = {

    println("Scheduler callback:" + scheduler.name)
    assert(scheduler.clazz == TestScheduleJob.getClass.getName)
    assert(scheduler.parameters("p1") == 1)
    assert(scheduler.parameters("p2") == "1")
    Resp.success(null)

  }
}
