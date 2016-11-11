package com.ecfront.ez.framework.core.config

import com.fasterxml.jackson.databind.JsonNode

/**
  * EZ配置文件对象
  *
  * @param ez   EZ服务配置项
  * @param args APP级配置参数
  */
private[core] case class EZConfig(ez: EZInfo, var args: JsonNode)

// EZ服务配置项
private[core] case class EZInfo(
                   // APP名称
                   var app: String,
                   // 模块名称
                   var module: String,
                   // 实例名称
                   var instance: String,
                   // 集群信息
                   var cluster:Map[String,Any],
                   // 缓存信息
                   var cache:Map[String,Any],
                   // RPC信息
                   var rpc: Map[String, Any],
                   // 时区
                   var timezone: String,
                   // 语言
                   var language: String,
                   var isDebug: Boolean,
                   // 性能设置
                   var perf: collection.mutable.Map[String, Any],
                   // 服务配置项
                   var services: Map[String, Any]
                 )
