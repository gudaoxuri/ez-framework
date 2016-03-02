package com.asto.ez.framework.scheduler

import java.util.concurrent.atomic.AtomicBoolean

import com.asto.ez.framework.storage.{BaseStorage, StatusStorage}
import com.ecfront.common.JsonHelper
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.quartz.impl.StdSchedulerFactory

import scala.concurrent.ExecutionContext.Implicits.global

object SchedulerService extends LazyLogging {

  private val quartzScheduler = StdSchedulerFactory.getDefaultScheduler
  private val initialized = new AtomicBoolean(false)

  private var schedulerStorage: StatusStorage[EZ_Scheduler] = _
  private var logStorage: BaseStorage[EZ_Scheduler_Log] = _

  def save(scheduler: EZ_Scheduler): Unit = {
    scheduler.enable = true
    scheduler.parameterstr = JsonHelper.toJsonString(scheduler.parameters)
    schedulerStorage.save(scheduler).onSuccess {
      case saveResp =>
        if (saveResp) {
          JobHelper.add(scheduler.name, scheduler.cron, classOf[VertxJob], packageScheduler(scheduler), quartzScheduler)
        }
    }
  }

  def update(scheduler: EZ_Scheduler): Unit = {
    scheduler.enable = true
    scheduler.parameterstr = JsonHelper.toJsonString(scheduler.parameters)
    schedulerStorage.update(scheduler).onSuccess {
      case updateResp =>
        if (updateResp) {
          JobHelper.modify(scheduler.name, scheduler.cron, classOf[VertxJob], packageScheduler(scheduler), quartzScheduler)
        }
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

  def delete(id: String): Unit = {
    schedulerStorage.getById(id).onSuccess {
      case getResp =>
        schedulerStorage.deleteById(id).onSuccess {
          case deleteResp =>
            if (deleteResp) {
              JobHelper.remove(getResp.body.name, quartzScheduler)
            }
        }
    }
  }

  def saveLog(log: EZ_Scheduler_Log): Unit = {
    logStorage.save(log, null)
  }

  def init(module: String, useMongo: Boolean): Unit = {
    if (!initialized.getAndSet(true)) {
      logger.debug("Startup scheduling.")
      if (useMongo) {
        schedulerStorage = Mongo_EZ_Scheduler
        logStorage = Mongo_EZ_Scheduler_Log
      } else {
        schedulerStorage = JDBC_EZ_Scheduler
        logStorage = JDBC_EZ_Scheduler_Log
      }
      quartzScheduler.start()
      val enabledCond = schedulerStorage match {
        case JDBC_EZ_Scheduler => "module =?"
        case Mongo_EZ_Scheduler => s"""{"module":"$module"}"""
      }
      schedulerStorage.findEnabled(enabledCond, List(module)).onSuccess {
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
