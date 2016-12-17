package com.ecfront.ez.framework.core.monitor

import java.util.Date
import java.util.concurrent.ConcurrentHashMap

import com.ecfront.ez.framework.core.EZ
import com.ecfront.ez.framework.core.helper.TimeHelper
import com.ecfront.ez.framework.core.logger.Logging

import scala.collection.JavaConversions._

object TaskMonitor extends Logging {

  private val tasks = new ConcurrentHashMap[String, (String, Date)]()

  def add(taskName: String): String = {
    val taskId = EZ.createUUID
    tasks += taskId -> (taskName, new Date())
    taskId
  }

  def remove(taskId: String): Unit = {
    tasks -= (taskId)
  }

  def hasTask(): Boolean = {
    tasks.nonEmpty
  }

  /**
    * 等待任务结束
    *
    * @param timeout 毫秒
    */
  def waitFinish(timeout: Long = Long.MaxValue): Unit = {
    logger.info("[Monitor]waiting task finish...")
    val waitStart = new Date().getTime
    while (tasks.nonEmpty && waitStart + timeout < new Date().getTime) {
      Thread.sleep(500)
      if (new Date().getTime - waitStart > 60 * 1000) {
        var warn = "[Monitor]has some unfinished tasks:\r\n"
        warn += tasks.map(task => s" > id:${task._1} name:${task._2._1} start time:${TimeHelper.yyyy_MM_dd_HH_mm_ss_SSS.format(task._2._2)}").mkString("\r\n")
        logger.warn(warn)
      }
    }
    if (tasks.nonEmpty) {
      var error = "[Monitor]has some unfinished tasks,but time is out:\r\n"
      error += tasks.map(task => s" > id:${task._1} name:${task._2._1} start time:${TimeHelper.yyyy_MM_dd_HH_mm_ss_SSS.format(task._2._2)}").mkString("\r\n")
      logger.error(error)
    }
    // 再等1秒
    Thread.sleep(1000)
  }

}
