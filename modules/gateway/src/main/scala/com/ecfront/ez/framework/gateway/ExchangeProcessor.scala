package com.ecfront.ez.framework.gateway

import com.ecfront.common.Resp
import com.ecfront.ez.framework.core.rpc.{APIDTO, RPC, SUB}
import com.fasterxml.jackson.databind.JsonNode
import com.typesafe.scalalogging.slf4j.LazyLogging
import collection.JavaConversions._

@RPC("/ez/gateway/")
object ExchangeProcessor extends LazyLogging {

  @SUB("address/add/")
  def subscribeAddAddress(args: Map[String, String], apiDTO: APIDTO): Resp[Void] = {
    LocalCacheContainer.addRouter(apiDTO.channel, apiDTO.method, apiDTO.path)
  }

  @SUB("resource/add/")
  def subscribeAddResource(args: Map[String, String], res: Map[String, String]): Resp[Void] = {
    LocalCacheContainer.addResource(res("channel"), res("method"), res("path"))
  }

  @SUB("resource/remove/")
  def subscribeRemoveResource(args: Map[String, String], res: Map[String, String]): Resp[Void] = {
    LocalCacheContainer.removeResource(res("channel"), res("method"), res("path"))
  }

  @SUB("organization/add/")
  def subscribeAddOrganization(args: Map[String, String], res: Map[String, String]): Resp[Void] = {
    LocalCacheContainer.addOrganization(res("code"))
  }

  @SUB("organization/remove/")
  def subscribeRemoveOrganization(args: Map[String, String], res: Map[String, String]): Resp[Void] = {
    LocalCacheContainer.removeOrganization(res("code"))
  }

  @SUB("role/add/")
  def subscribeAddRole(args: Map[String, String], res:JsonNode): Resp[Void] = {
    LocalCacheContainer.addRole(res.get("code").asText(),res.get("resources").map(_.asText()).toSet)
  }

  @SUB("role/remove/")
  def subscribeRemoveRole(args: Map[String, String], res:JsonNode): Resp[Void] = {
    LocalCacheContainer.removeRole(res.get("code").asText())
  }

}
