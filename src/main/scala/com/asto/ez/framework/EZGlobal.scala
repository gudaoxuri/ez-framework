package com.asto.ez.framework

import com.typesafe.scalalogging.slf4j.LazyLogging
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject

/**
 * 全局数据存储类
 */
object EZGlobal extends LazyLogging {

  var vertx: Vertx = _
  //config.json数据
  var config: JsonObject = _

  lazy val ez = config.getJsonObject("ez")
  lazy val args = config.getJsonObject("args")

  lazy val resource_path = ez.getJsonObject("publicServer").getString("resourcePath")
  lazy val resource_url = ez.getJsonObject("publicServer").getString("publicUriPrefix")

}
