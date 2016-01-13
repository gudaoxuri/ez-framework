package com.asto.ez.framework.scheduler

import java.util.concurrent.atomic.AtomicBoolean

import com.asto.ez.framework.storage.StatusStorage
import com.ecfront.common.JsonHelper
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.quartz.impl.StdSchedulerFactory

import scala.concurrent.ExecutionContext.Implicits.global

object SchedulerService extends LazyLogging {

  private val quartzScheduler = StdSchedulerFactory.getDefaultScheduler
  private val initialized = new AtomicBoolean(false)

  private var stroage: StatusStorage[EZ_Scheduler] = _

  def save(scheduler: EZ_Scheduler): Unit = {
    scheduler.enable = true
    scheduler.parameterstr = JsonHelper.toJsonString(scheduler.parameters)
    stroage.save(scheduler).onSuccess {
      case saveResp =>
        if (saveResp) {
          JobHelper.add(scheduler.name, scheduler.cron, classOf[VertxJob], packageScheduler(scheduler), quartzScheduler)
        }
    }
  }

  def update(scheduler: EZ_Scheduler): Unit = {
    scheduler.enable = true
    scheduler.parameterstr = JsonHelper.toJsonString(scheduler.parameters)
    stroage.update(scheduler).onSuccess {
      case updateResp =>
        if (updateResp) {
          JobHelper.modify(scheduler.name, scheduler.cron, classOf[VertxJob], packageScheduler(scheduler), quartzScheduler)
        }
    }
  }

  private def packageScheduler(scheduler: EZ_Scheduler): Map[String, Any] = {
    Map(
      "id" -> scheduler.id,
      "cron" -> scheduler.cron,
      "clazz" -> scheduler.clazz,
      "parameters" -> scheduler.parameterstr
    )
  }

  def delete(name: String): Unit = {
    stroage.deleteByCond(" name =? ", List(name)).onSuccess {
      case deleteResp =>
        if (deleteResp) {
          JobHelper.remove(name, quartzScheduler)
        }
    }
  }

  def init(module: String, _stroage: StatusStorage[EZ_Scheduler]): Unit = {
    if (!initialized.getAndSet(true)) {
      logger.debug("Startup scheduling.")
      stroage = _stroage
      quartzScheduler.start()
     val enabledCond= _stroage match {
        case JDBC_EZ_Scheduler =>"module =?"
        case Mongo_EZ_Scheduler =>s"""{"module":"$module"}"""
      }
      stroage.findEnabled(enabledCond, List(module)).onSuccess {
        case findResp =>
          if (findResp) {
            findResp.body.foreach {
              job =>
                job.parameters = JsonHelper.toGenericObject[Map[String, Any]](job.parameterstr)
                JobHelper.add(job.name, job.cron, classOf[VertxJob], packageScheduler(job), quartzScheduler)
            }
          }
      }
    }
  }

  def shutdown(): Unit = {
    quartzScheduler.shutdown()
  }

}
