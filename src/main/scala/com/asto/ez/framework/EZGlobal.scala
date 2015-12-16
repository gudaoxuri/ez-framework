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
  lazy val ez_rpc = if (ez.containsKey("rpc")) ez.getJsonObject("rpc") else null
  lazy val ez_rpc_http_public_uri_prefix_path = if (ez_rpc != null && ez_rpc.containsKey("http")&& ez_rpc.getJsonObject("http").containsKey("publicUriPrefix")) ez_rpc.getJsonObject("http").getString("publicUriPrefix") else null
  lazy val ez_rpc_http_resource_path = if (ez_rpc != null && ez_rpc.containsKey("http")) ez_rpc.getJsonObject("http").getString("resourcePath") else ""
  lazy val ez_rpc_http_resource_url = if (ez_rpc != null && ez_rpc.containsKey("http")) ez_rpc.getJsonObject("http").getString("publicUriPrefix") else ""

  lazy val ez_storage = if (ez.containsKey("storage")) ez.getJsonObject("storage") else null
  lazy val ez_cache = if (ez.containsKey("cache")) ez.getJsonObject("cache") else null
  lazy val ez_mail = if (ez.containsKey("mail")) ez.getJsonObject("mail") else null
  lazy val ez_scheduler = if (ez.containsKey("scheduler")) ez.getBoolean("scheduler") else null
  lazy val ez_auth = if (ez.containsKey("auth")) ez.getJsonObject("auth") else null

  lazy val args = config.getJsonObject("args")


}
