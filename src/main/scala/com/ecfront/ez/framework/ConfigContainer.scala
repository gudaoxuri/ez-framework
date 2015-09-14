package com.ecfront.ez.framework

import com.ecfront.common.{ConfigHelper, JsonHelper}
import com.fasterxml.jackson.databind.JsonNode
import com.typesafe.scalalogging.slf4j.LazyLogging


/**
 * 配置容器
 */
object ConfigContainer extends LazyLogging {

  logger.info("Load config.")

  val config: JsonNode = ConfigHelper.init(this.getClass.getResource("/config.json").getPath, classOf[JsonNode]).get

  val serversConfig: ServerConfig = JsonHelper.toObject(config.get("servers"), classOf[ServerConfig])

  val args: JsonNode = config.get("args")

}

case class ServerConfig(publicServer: WebServer, clusterServer: ClusterServer, servicePath: String, entityPath: String)

case class WebServer(host: String, port: Int, resourcePath: String, publicUriPrefix: String, authManage: Boolean)

case class ClusterServer(host: String)


