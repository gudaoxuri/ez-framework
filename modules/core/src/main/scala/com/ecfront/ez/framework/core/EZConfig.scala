package com.ecfront.ez.framework.core

import com.fasterxml.jackson.databind.JsonNode

/**
  * EZ配置文件对象
  *
  * @param ez   EZ服务配置项
  * @param args APP级配置参数
  */
private[core] case class EZConfig(ez: EZInfo, args: JsonNode)

// EZ服务配置项
private[core] case class EZInfo(
                   // APP名称
                   app: String,
                   // 模块名称
                   module: String,
                   // 实例名称
                   var instance: String,
                   // 缓存服务地址
                   cache: Map[String, Any],
                   // 时区
                   timezone: String,
                   // 语言
                   var language: String,
                   var isDebug: Boolean,
                   // 性能设置
                   var perf: collection.mutable.Map[String, Any],
                   // 服务配置项
                   services: Map[String, Any]
                 )
