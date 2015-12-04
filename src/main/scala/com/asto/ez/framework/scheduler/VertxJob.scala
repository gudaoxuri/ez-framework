package com.asto.ez.framework.scheduler

import com.ecfront.common.JsonHelper
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.quartz.{Job, JobExecutionContext}

import scala.reflect.runtime._

class VertxJob extends Job with LazyLogging {

  private val runtimeMirror = universe.runtimeMirror(getClass.getClassLoader)

  override def execute(context: JobExecutionContext): Unit = {
    val scheduler = EZ_Scheduler()
    scheduler.cron = context.getMergedJobDataMap.getString("cron")
    scheduler.clazz = context.getMergedJobDataMap.getString("clazz")
    scheduler.parameterstr = context.getMergedJobDataMap.getString("parameters")
    scheduler.parameters = JsonHelper.toGenericObject[Map[String, Any]](scheduler.parameterstr)
    logger.debug(s"Execute scheduling : [${scheduler.name}] ${scheduler.clazz} ")
    runtimeMirror.reflectModule(runtimeMirror.staticModule(scheduler.clazz)).instance.asInstanceOf[ScheduleJob].execute(scheduler)

  }
}
