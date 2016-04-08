package com.ecfront.ez.framework.service.scheduler

import com.ecfront.common.JsonHelper
import com.ecfront.ez.framework.service.storage.foundation.Page
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.quartz.impl.StdSchedulerFactory

/**
  * 调度处理
  */
object SchedulerProcessor extends LazyLogging {

  private val quartzScheduler = StdSchedulerFactory.getDefaultScheduler

  /**
    * 保存调度任务
    *
    * @param scheduler 调度任务
    */
  def save(scheduler: EZ_Scheduler): EZ_Scheduler = {
    scheduler.enable = true
    scheduler.parameterstr = JsonHelper.toJsonString(scheduler.parameters)
    val saveR = EZ_Scheduler.save(scheduler)
    if (saveR) {
      JobHelper.add(scheduler.name, scheduler.cron, classOf[ScheduleJobProxy], packageScheduler(scheduler), quartzScheduler)
    }
    saveR.body
  }

  /**
    * 更新调度任务
    *
    * @param scheduler 调度任务
    */
  def update(scheduler: EZ_Scheduler): EZ_Scheduler = {
    scheduler.enable = true
    scheduler.parameterstr = JsonHelper.toJsonString(scheduler.parameters)
    val updateR = EZ_Scheduler.update(scheduler)
    if (updateR) {
      JobHelper.modify(scheduler.name, scheduler.cron, classOf[ScheduleJobProxy], packageScheduler(scheduler), quartzScheduler)
    }
    updateR.body
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
    * @param name 调度任务名称
    */
  def delete(name: String): Unit = {
    val deleteR = EZ_Scheduler.deleteByName(name)
    if (deleteR) {
      JobHelper.remove(name, quartzScheduler)
    }
  }

  /**
    * 保存调度日志
    *
    * @param log 调度日志
    */
  def saveLog(log: EZ_Scheduler_Log): Unit = {
    EZ_Scheduler_Log.save(log, null)
  }

  /**
    * 根据调度名称分页获取日志
    *
    * @param name       调度名称
    * @param pageNumber 当前页，从1开始
    * @param pageSize   每页条数
    * @return 日志集
    */
  def pageLogsByName(name: String, pageNumber: Long, pageSize: Int): Page[EZ_Scheduler_Log] = {
    EZ_Scheduler_Log.pageByName(name, pageNumber, pageSize).body
  }

  /**
    * 初始化调度器
    *
    * @param module 当前模块，只有等于当前模块的记录才会被调度
    */
  def init(module: String): Unit = {
    logger.debug("Startup scheduling.")
    quartzScheduler.start()
    val findR = EZ_Scheduler.findByModule(module)
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
