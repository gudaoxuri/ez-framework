package com.ecfront.ez.framework.service.scheduler

import java.util.Date

import com.ecfront.common.JsonHelper
import com.ecfront.ez.framework.core.helper.TimeHelper
import com.ecfront.ez.framework.core.logger.Logging
import org.quartz.{Job, JobExecutionContext}

import scala.reflect.runtime._

/**
  * 调度回调处理代理类
  */
class ScheduleJobProxy extends Job with Logging {

  private val runtimeMirror = universe.runtimeMirror(getClass.getClassLoader)

  override def execute(context: JobExecutionContext): Unit = {
    val scheduler = EZ_Scheduler()
    scheduler.id = context.getMergedJobDataMap.getString("id")
    scheduler.name = context.getMergedJobDataMap.getString("name")
    scheduler.cron = context.getMergedJobDataMap.getString("cron")
    scheduler.clazz = context.getMergedJobDataMap.getString("clazz")
    scheduler.parameterstr = context.getMergedJobDataMap.getString("parameters")
    scheduler.parameters = JsonHelper.toObject[Map[String, Any]](scheduler.parameterstr)
    logger.debug(s"Start execute scheduling : [${scheduler.name}] ${scheduler.clazz} ")
    val log = EZ_Scheduler_Log()
    log.scheduler_name = scheduler.name
    log.start_time = TimeHelper.msf.format(new Date()).toLong
    try {
      // 执行真正的调度回调类
      val resp = runtimeMirror.reflectModule(runtimeMirror.staticModule(scheduler.clazz)).instance.asInstanceOf[ScheduleJob].execute(scheduler)
      if (resp) {
        log.end_time = TimeHelper.msf.format(new Date()).toLong
        log.success = resp
        log.message = resp.message
        SchedulerProcessor.saveLog(log)
        logger.debug(s"Finish execute scheduling  : [${scheduler.name}] ${scheduler.clazz} result ${log.success}")
      }
    } catch {
      case e: Throwable =>
        log.end_time = TimeHelper.msf.format(new Date()).toLong
        log.success = false
        log.message = e.getMessage
        SchedulerProcessor.saveLog(log)
        logger.error("Execute scheduling error.", e)
    }
  }
}
