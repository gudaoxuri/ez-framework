package com.ecfront.ez.framework.module.schedule

import java.util.{Timer, TimerTask}

import com.ecfront.common.{JsonHelper, Req}
import com.ecfront.ez.framework.service.SyncService
import com.ecfront.ez.framework.service.common.DLockService
import com.ecfront.ez.framework.service.protocols.JDBCService
import com.typesafe.scalalogging.slf4j.LazyLogging

import scala.reflect.runtime._

object ScheduleService extends LazyLogging {

  def registerTask[T <: EZTask](taskName: String, moduleName: String, executeClass: Class[T], delay: Long, period: Long, parameters: Map[String, Any] = Map()): String = {
    logger.info(s"Register Schedule Task [$moduleName][$taskName]")
    val task = EZ_Schedule_Task()
    task.task_name = taskName
    task.module_name = moduleName
    task.task_path = executeClass.getName
    task.parameters = JsonHelper.toJsonString(parameters)
    task.delay = delay
    task.period = period
    task.is_enabled = true
    val taskId = ScheduleTaskService.__saveWithoutTransaction(task).get
    runById(taskId)
    taskId
  }

  def disableTask(taskId: String): Unit = {
    val task = ScheduleTaskService.__getById(taskId).get
    logger.info(s"Disable Schedule Task [${task.module_name}][${task.task_name}]")
    task.is_enabled = false
    ScheduleTaskService.__updateWithoutTransaction(taskId, task)
    timers(taskId).cancel()
    timers -= taskId
  }

  def enableTask(taskId: String): Unit = {
    val task = ScheduleTaskService.__getById(taskId).get
    logger.info(s"Enable Schedule Task [${task.module_name}][${task.task_name}]")
    task.is_enabled = true
    ScheduleTaskService.__updateWithoutTransaction(taskId, task)
    runById(taskId)
  }

  def unRegisterTask(taskId: String): Unit = {
    val task = ScheduleTaskService.__getById(taskId).get
    logger.info(s"UnRegister Schedule Task [${task.module_name}][${task.task_name}]")
    ScheduleTaskService.__deleteById(taskId)
    timers(taskId).cancel()
    timers -= taskId
  }

  def existTask(taskName: String, moduleName: String): Boolean = {
    ScheduleTaskService.__findByCondition("task_name =? AND module_name =? ", Some(List(taskName, moduleName))).get.nonEmpty
  }

  private val timers = collection.mutable.Map[String, Timer]()
  private val runtimeMirror = universe.runtimeMirror(getClass.getClassLoader)

  def stop(): Unit = {
    timers.foreach(_._2.cancel())
  }

  def runById(taskId: String = null): Unit = {
    run(List(ScheduleTaskService._getById(taskId).body))
  }

  def runByModuleName(moduleName: String = null): Unit = {
    run(ScheduleTaskService._findByCondition("module_name = ?  AND is_enabled = ?", Some(List(moduleName, true))).body)
  }

  private def run(tasks: List[EZ_Schedule_Task]): Unit = {
    if (tasks != null && tasks.nonEmpty) {
      tasks.foreach {
        task =>
          val timerTask = new TimerTask() {
            override def run(): Unit = {
              val lock = DLockService("schedule-" + task.task_path)
              if (lock.tryLock()) {
                val instance = if (task.task_path.endsWith("$")) {
                  runtimeMirror.reflectModule(runtimeMirror.staticModule(task.task_path)).instance.asInstanceOf[EZTask]
                } else {
                  Class.forName(task.task_path).newInstance().asInstanceOf[EZTask]
                }
                val log = EZ_Schedule_Log()
                log.task_id = task.id
                log.task_name = task.task_name
                log.module_name = task.module_name
                log.execute_start_time = System.currentTimeMillis()
                try {
                  logger.debug(s"Execute timer : [${task.id}] - ${task.task_path} ")
                  instance.execute(JsonHelper.toObject(task.parameters, classOf[Map[String, Any]]))
                  log.is_successful = true
                  log.execute_finish_message = ""
                } catch {
                  case e: Exception =>
                    log.is_successful = false
                    log.execute_finish_message = e.getMessage
                    logger.error("Schedule execute error.", e)
                } finally {
                  log.execute_finish_time = System.currentTimeMillis()
                  if (log.is_successful) {
                    task.last_execute_success_time = log.execute_finish_time
                  }
                  task.last_execute_finish_time = log.execute_finish_time
                  task.last_execute_finish_message = log.execute_finish_message
                  ScheduleLogService.__saveWithoutTransaction(log)
                  ScheduleTaskService.__updateWithoutTransaction(task.id, task)
                  lock.unLock()
                }
              }
            }
          }
          val timer = new Timer()
          timer.schedule(timerTask, task.delay, task.period)
          timers += task.id -> timer
      }
    }
  }

}

object ScheduleTaskService extends JDBCService[EZ_Schedule_Task, Req] with SyncService[EZ_Schedule_Task, Req]

object ScheduleLogService extends JDBCService[EZ_Schedule_Log, Req] with SyncService[EZ_Schedule_Log, Req]
