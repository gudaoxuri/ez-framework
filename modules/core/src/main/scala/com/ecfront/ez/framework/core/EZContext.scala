package com.ecfront.ez.framework.core

import com.typesafe.scalalogging.slf4j.LazyLogging
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject

/**
  * EZ容器，用于放置全局属性
  */
object EZContext extends LazyLogging {

  // APP名称，来自配置文件
  var app: String = _
  // 模块名称，来自配置文件
  var module: String = _
  // 语言
  var language: String = _
  // 性能配置
  var perf: Map[String, Any] = _
  // Vertx实例
  var vertx: Vertx = _
  // 配置文件中的APP级参数
  var args: JsonObject = _
  // 配置文件路径
  lazy val confPath: String = findConfPath()

  private def findConfPath(): String = {
    var confPath = System.getProperty("conf")
    if (confPath == null) {
      val classPath = this.getClass.getResource("/")
      if (classPath != null) {
        confPath = classPath.getPath
      } else {
        var currentPath = this.getClass.getProtectionDomain.getCodeSource.getLocation.toURI.getPath
        currentPath = currentPath.substring(0, currentPath.lastIndexOf("/"))
        confPath = currentPath + "/config/"
      }
    }
    logger.info(s"Config path is : $confPath")
    confPath
  }

}
