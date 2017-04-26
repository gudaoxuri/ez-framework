package com.ecfront.ez.framework.core

import java.net.InetAddress
import java.util.UUID
import java.util.concurrent.Executors

import com.ecfront.ez.framework.core.cluster.{ClusterCache, ClusterDist}
import com.ecfront.ez.framework.core.config.EZConfig
import com.ecfront.ez.framework.core.eventbus.EventBusProcessor
import com.ecfront.ez.framework.core.logger.Logging
import com.ecfront.ez.framework.core.monitor.TaskMonitor
import com.fasterxml.jackson.databind.JsonNode

/**
  * EZ容器，用于放置全局属性
  */
object EZ extends Logging {

  object Info {
    // APP名称，来自配置文件
    var app: String = _
    // 模块名称，来自配置文件
    var module: String = _
    // 时区，来自配置文件
    var timezone: String = _
    // 实例名称，来自配置文件,同一app和module下不能重复,不存在时等于(ip+工程路径).hash
    var instance: String = _
    // 语言
    var language: String = _
    // 配置文件路径
    lazy val confPath: String = findConfPath()
    // 配置参数
    lazy val args: JsonNode = config.args
    // 完整配置信息
    var config: EZConfig = _

    // 项目主机IP
    val projectIp = InetAddress.getLocalHost.getHostAddress
    // 项目主机名
    val projectHost = InetAddress.getLocalHost.getHostName
    // 项目路径
    val projectPath = {
      var currentPath = this.getClass.getProtectionDomain.getCodeSource.getLocation.toURI.getPath
      currentPath = currentPath.substring(0, currentPath.lastIndexOf("/"))
      currentPath
    }

    private def findConfPath(): String = {
      var confPath = System.getProperty("conf")
      if (confPath == null) {
        val classPath = this.getClass.getResource("/")
        if (classPath != null) {
          confPath = classPath.getPath
        } else {
          confPath = projectPath + "/config/"
        }
      }
      logger.info(s"Config path is : $confPath")
      confPath
    }

  }

  // 是否调试模式
  var isDebug: Boolean = _

  val eb = EventBusProcessor

  var dist: ClusterDist = _

  var cache: ClusterCache = _

  def context: EZContext = EZContext.getContext

  def createUUID: String = UUID.randomUUID().toString.replace("-", "")

  val execute = Executors.newCachedThreadPool()

  def newThread(fun: => Unit, needWait: Boolean = true): Unit = {
    execute.execute(new RunnableWithContext(fun, needWait, EZ.context))
  }

  class RunnableWithContext(fun: => Unit, needWait: Boolean, context: EZContext) extends Runnable {
    override def run(): Unit = {
      val taskId = if (needWait) TaskMonitor.add("Async Task") else null
      try {
        EZContext.setContext(context)
        fun
      } catch {
        case e: Throwable =>
          logger.error(s"Execute async task error:${e.getMessage}", e)
          throw e
      } finally {
        if (needWait) {
          TaskMonitor.remove(taskId)
        }
      }
    }
  }

}
