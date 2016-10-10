package com.ecfront.ez.framework.service.scheduler

import com.typesafe.scalalogging.slf4j.LazyLogging
import org.quartz.impl.JobDetailImpl
import org.quartz.impl.triggers.CronTriggerImpl
import org.quartz.{Job, JobDataMap, JobKey, Scheduler}

/**
  * quartz辅助类
  */
object JobHelper extends LazyLogging {

  /**
    * 添加任务
    *
    * @param taskId     任务ID
    * @param cron       cron表达式
    * @param jobClass   回调的class类
    * @param parameters 任务参数
    * @param scheduler  scheduler对象
    * @return 是否成功
    */
  def add(taskId: String, cron: String, jobClass: Class[_ <: Job], parameters: Map[String, Any], scheduler: Scheduler): Boolean = {
    val jobDetail = new JobDetailImpl()
    jobDetail.setJobClass(jobClass)
    val jobKey = new JobKey(taskId)
    jobDetail.setKey(jobKey)
    val jobDataMap = new JobDataMap()
    if (null != parameters) {
      parameters.foreach {
        param =>
          jobDataMap.put(param._1, param._2)
      }
    }
    jobDetail.setJobDataMap(jobDataMap)
    val trigger = new CronTriggerImpl()
    trigger.setName(taskId)
    try {
      trigger.setCronExpression(cron)
      scheduler.scheduleJob(jobDetail, trigger)
      logger.debug(s"Register scheduling: $taskId  to ${jobClass.getSimpleName} in $cron")
      true
    } catch {
      case ex: Throwable =>
        logger.error("Register scheduling error", ex)
        false
    }
  }


  /**
    * 修改任务
    *
    * @param taskId     任务ID
    * @param cron       cron表达式
    * @param jobClass   回调的class类
    * @param parameters 任务参数
    * @param scheduler  scheduler对象
    * @return 是否成功
    */
  def modify(taskId: String, cron: String, jobClass: Class[_ <: Job], parameters: Map[String, Any], scheduler: Scheduler): Boolean = {
    try {
      remove(taskId, scheduler)
      add(taskId, cron, jobClass, parameters, scheduler)
      logger.debug(s"Modify scheduling: $taskId")
      true
    } catch {
      case ex: Throwable =>
        logger.error("Modify scheduling error", ex)
        false
    }
  }

  /**
    * 删除任务
    *
    * @param taskId    任务ID
    * @param scheduler scheduler对象
    * @return 是否成功
    */
  def remove(taskId: String, scheduler: Scheduler): Boolean = {
    try {
      scheduler.deleteJob(new JobKey(taskId))
      logger.debug(s"Remove scheduling: $taskId")
      true
    } catch {
      case ex: Throwable =>
        logger.error("Remove scheduling error", ex)
        false
    }
  }
}
