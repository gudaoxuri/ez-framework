package com.asto.ez.framework.scheduler

import java.util.Date

import com.asto.ez.framework.helper.TimeHelper
import com.ecfront.common.{AsyncResp, JsonHelper, Resp}
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.quartz.{Job, JobExecutionContext}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Promise
import scala.reflect.runtime._

class VertxJob extends Job with LazyLogging {

  private val runtimeMirror = universe.runtimeMirror(getClass.getClassLoader)

  override def execute(context: JobExecutionContext): Unit = {
    val scheduler = EZ_Scheduler()
    scheduler.id = context.getMergedJobDataMap.getString("id")
    scheduler.name = context.getMergedJobDataMap.getString("name")
    scheduler.cron = context.getMergedJobDataMap.getString("cron")
    scheduler.clazz = context.getMergedJobDataMap.getString("clazz")
    scheduler.parameterstr = context.getMergedJobDataMap.getString("parameters")
    scheduler.parameters = JsonHelper.toGenericObject[Map[String, Any]](scheduler.parameterstr)
    logger.debug(s"Start execute scheduling : [${scheduler.name}] ${scheduler.clazz} ")
    val p = Promise[Resp[Void]]()
    val log = EZ_Scheduler_Log()
    log.scheduler_name = scheduler.name
    log.start_time = TimeHelper.msf.format(new Date()).toLong
    try {
      runtimeMirror.reflectModule(runtimeMirror.staticModule(scheduler.clazz)).instance.asInstanceOf[ScheduleJob].execute(scheduler, AsyncResp(p))
    } catch {
      case e: Throwable =>
        logger.error("Execute scheduling error.", e)
        log.end_time = TimeHelper.msf.format(new Date()).toLong
        log.success = false
        log.desc = e.getMessage
        SchedulerService.saveLog(log)
    }
    p.future.onSuccess {
      case resp: Resp[Void] =>
        log.end_time = TimeHelper.msf.format(new Date()).toLong
        log.success = resp
        log.desc = resp.message
        SchedulerService.saveLog(log)
        logger.debug(s"Finish execute scheduling  : [${scheduler.name}] ${scheduler.clazz} result ${log.success}")
    }
  }
}
