package com.ecfront.ez.framework.core

import io.vertx.core.json.JsonObject

/**
  * EZ配置文件对象
  *
  * @param ez   EZ服务配置项
  * @param args APP级配置参数
  */
case class EZConfig(ez: EZInfo, args: JsonObject)

// EZ服务配置项
case class EZInfo(
                   // APP名称
                   app: String,
                   // 模块名称
                   module: String,
                   // 服务配置项
                   services: Map[String, Any]
                 )
