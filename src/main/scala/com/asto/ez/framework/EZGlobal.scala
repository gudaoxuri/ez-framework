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
  lazy val appName = if (ez.containsKey("appName")) ez.getString("appName") else "EZ-Framework"
  lazy val ez_rpc = if (ez.containsKey("rpc")) ez.getJsonObject("rpc") else null
  lazy val ez_rpc_http_public_uri_prefix_path = if (ez_rpc != null && ez_rpc.containsKey("http") && ez_rpc.getJsonObject("http").containsKey("publicUriPrefix")) ez_rpc.getJsonObject("http").getString("publicUriPrefix") else null
  lazy val ez_rpc_http_resource_path = if (ez_rpc != null && ez_rpc.containsKey("http") && ez_rpc.getJsonObject("http").containsKey("resourcePath")) ez_rpc.getJsonObject("http").getString("resourcePath") else ""
  lazy val ez_rpc_http_access_control_allow_origin = if (ez_rpc != null && ez_rpc.containsKey("http") && ez_rpc.getJsonObject("http").containsKey("accessControlAllowOrigin")) ez_rpc.getJsonObject("http").getString("accessControlAllowOrigin") else "*"
  lazy val ez_rpc_http_base_url = if (ez_rpc != null && ez_rpc.containsKey("http"))
    "http://" + ez_rpc.getJsonObject("http").getString("host") + ":" + ez_rpc.getJsonObject("http").getInteger("port")
    else ""

  lazy val ez_storage = if (ez.containsKey("storage")) ez.getJsonObject("storage") else null
  lazy val ez_storage_jdbc_update_timeout = if (ez_storage != null && ez_storage.containsKey("jdbc") && ez_storage.getJsonObject("jdbc").containsKey("update_timeout")) ez_storage.getJsonObject("jdbc").getInteger("update_timeout") else null
  lazy val ez_storage_jdbc_query_timeout = if (ez_storage != null && ez_storage.containsKey("jdbc") && ez_storage.getJsonObject("jdbc").containsKey("query_timeout")) ez_storage.getJsonObject("jdbc").getInteger("query_timeout") else null

  lazy val ez_cache = if (ez.containsKey("cache")) ez.getJsonObject("cache") else null
  lazy val ez_mail = if (ez.containsKey("mail")) ez.getJsonObject("mail") else null
  lazy val ez_scheduler = if (ez.containsKey("scheduler")) ez.getBoolean("scheduler") else null
  lazy val ez_auth = if (ez.containsKey("auth")) ez.getJsonObject("auth") else null
  lazy val ez_auth_active_url = if (ez_auth!=null) ez_auth.getString("active_url") else ""
  lazy val ez_auth_rest_password_url = if (ez_auth!=null) ez_auth.getString("rest_password_url") else ""

  lazy val args = config.getJsonObject("args")


}
