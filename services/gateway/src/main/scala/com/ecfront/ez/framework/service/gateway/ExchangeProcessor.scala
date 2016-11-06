package com.ecfront.ez.framework.service.gateway

import com.ecfront.common.Resp
import com.ecfront.ez.framework.core.EZ
import com.ecfront.ez.framework.core.logger.Logging
import com.ecfront.ez.framework.core.rpc._
import com.fasterxml.jackson.databind.JsonNode

import scala.collection.JavaConversions._

@RPC("/ez/auth/","","")
object ExchangeProcessor extends Logging {

  @SUB("/ez/gateway/address/add/")
  def subscribeAddAddress(args: Map[String, String], apiDTO: APIDTO): Resp[Void] = {
    LocalCacheContainer.addRouter(apiDTO.method, apiDTO.path)
  }

  @REPLY("/ez/gateway/auth/flush/")
  def flushCache(args: Map[String, String],body: String): Resp[Void] = {
    LocalCacheContainer.flushAuth()
  }

  @SUB("rbac/organization/add/")
  def subscribeAddOrganization(args: Map[String, String], org: Map[String, String]): Resp[Void] = {
    LocalCacheContainer.addOrganization(org("code"))
  }

  @SUB("rbac/organization/remove/")
  def subscribeRemoveOrganization(args: Map[String, String], org: Map[String, String]): Resp[Void] = {
    LocalCacheContainer.removeOrganization(org("code"))
  }

  @SUB("rbac/resource/add/")
  def subscribeAddResource(args: Map[String, String], res: Map[String, String]): Resp[Void] = {
    LocalCacheContainer.addResource(res("method"), res("uri"))
  }

  @SUB("rbac/resource/remove/")
  def subscribeRemoveResource(args: Map[String, String], res: Map[String, String]): Resp[Void] = {
    val code = res("code")
    val Array(method, uri) = code.split(EZ.eb.ADDRESS_SPLIT_FLAG)
    LocalCacheContainer.removeResource(method, uri)
  }

  @SUB("rbac/role/add/")
  def subscribeAddRole(args: Map[String, String], res: JsonNode): Resp[Void] = {
    LocalCacheContainer.addRole(res.get("code").asText(), res.get("resource_codes").map(_.asText()).toSet)
  }

  @SUB("rbac/role/remove/")
  def subscribeRemoveRole(args: Map[String, String], res: JsonNode): Resp[Void] = {
    LocalCacheContainer.removeRole(res.get("code").asText())
  }

}
