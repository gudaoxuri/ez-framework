package com.ecfront.ez.framework.service.scheduler

import com.ecfront.common.JsonHelper
import com.ecfront.ez.framework.service.storage.foundation.{BaseStorage, StatusStorage}
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.quartz.impl.StdSchedulerFactory

/**
  * 调度处理
  */
object SchedulerProcessor extends LazyLogging {

  private val quartzScheduler = StdSchedulerFactory.getDefaultScheduler

  private var schedulerStorage: StatusStorage[EZ_Scheduler] = _
  private var logStorage: BaseStorage[EZ_Scheduler_Log] = _

  /**
    * 保存调度任务
    *
    * @param scheduler 调度任务
    */
  def save(scheduler: EZ_Scheduler): Unit = {
    scheduler.enable = true
    scheduler.parameterstr = JsonHelper.toJsonString(scheduler.parameters)
    val saveR = schedulerStorage.save(scheduler)
    if (saveR) {
      JobHelper.add(scheduler.name, scheduler.cron, classOf[ScheduleJobProxy], packageScheduler(scheduler), quartzScheduler)
    }
  }

  /**
    * 更新调度任务
    *
    * @param scheduler 调度任务
    */
  def update(scheduler: EZ_Scheduler): Unit = {
    scheduler.enable = true
    scheduler.parameterstr = JsonHelper.toJsonString(scheduler.parameters)
    val updateR = schedulerStorage.update(scheduler)
    if (updateR) {
      JobHelper.modify(scheduler.name, scheduler.cron, classOf[ScheduleJobProxy], packageScheduler(scheduler), quartzScheduler)
    }
  }

  private def packageScheduler(scheduler: EZ_Scheduler): Map[String, Any] = {
    Map(
      "id" -> scheduler.id,
      "name" -> scheduler.name,
      "cron" -> scheduler.cron,
      "clazz" -> scheduler.clazz,
      "parameters" -> scheduler.parameterstr
    )
  }

  /**
    * 删除调度信息
    *
    * @param id 调度任务ID
    */
  def delete(id: String): Unit = {
    val getR = schedulerStorage.getById(id)
    val deleteR = schedulerStorage.deleteById(id)
    if (deleteR) {
      JobHelper.remove(getR.body.name, quartzScheduler)
    }
  }

  /**
    * 保存调度日志
    *
    * @param log 调度日志
    */
  def saveLog(log: EZ_Scheduler_Log): Unit = {
    logStorage.save(log, null)
  }

  /**
    * 初始化调度器
    *
    * @param module   当前模块，只有等于当前模块的记录才会被调度
    * @param useMongo 是否使用Mongo数据库
    */
  def init(module: String, useMongo: Boolean): Unit = {
    logger.debug("Startup scheduling.")
    val enabledCond =
      if (useMongo) {
        schedulerStorage = Mongo_EZ_Scheduler
        logStorage = Mongo_EZ_Scheduler_Log
        s"""{"module":"$module"}"""
      } else {
        schedulerStorage = JDBC_EZ_Scheduler
        logStorage = JDBC_EZ_Scheduler_Log
        "module =?"
      }
    quartzScheduler.start()
    val findR = schedulerStorage.findEnabled(enabledCond, List(module))
    if (findR) {
      findR.body.foreach {
        job =>
          job.parameters = JsonHelper.toObject[Map[String, Any]](job.parameterstr)
          JobHelper.add(job.name, job.cron, classOf[ScheduleJobProxy], packageScheduler(job), quartzScheduler)
      }
    }
  }

  /**
    * 关闭调度任务
    */
  def shutdown(): Unit = {
    quartzScheduler.shutdown()
  }

}
