package com.ecfront.ez.framework.core

/**
  * EZ配置文件对象
  *
  * @param ez   EZ服务配置项
  * @param args APP级配置参数
  */
case class EZConfig(ez: EZInfo, args: Map[String, Any])

// EZ服务配置项
case class EZInfo(
                   // APP名称
                   app: String,
                   // 模块名称
                   module: String,
                   // 时区
                   timezone: String,
                   // 实例名称
                   var instance: String,
                   // 语言
                   var language: String,
                   var isDebug: Boolean,
                   // 性能设置
                   var perf: collection.mutable.Map[String, Any],
                   // 服务配置项
                   services: Map[String, Any]
                 )
