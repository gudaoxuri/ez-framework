package com.ecfront.ez.framework.service.gateway

import com.ecfront.common.Resp
import com.ecfront.ez.framework.core.EZ
import com.ecfront.ez.framework.core.logger.Logging
import com.ecfront.ez.framework.core.rpc._
import com.fasterxml.jackson.databind.JsonNode

import scala.collection.JavaConversions._

@RPC("/ez/auth/", "网关组件内部处理", "")
object ExchangeProcessor extends Logging {

  @SUB("/ez/gateway/address/add/","接收发布的资源地址","","","")
  def subscribeAddAddress(args: Map[String, String], apiDTO: APIDTO): Resp[Void] = {
    LocalCacheContainer.addRouter(apiDTO.method, apiDTO.path)
  }

  @SUB("/ez/gateway/auth/flush/","刷新权限数据","","","")
  def flushCache(args: Map[String, String], body: String): Resp[Void] = {
    LocalCacheContainer.flushAuth()
  }

  @SUB("rbac/organization/add/","添加组织信息","",
    """
      |code|string|组织编码|true
    ""","")
  def subscribeAddOrganization(args: Map[String, String], org: Map[String, String]): Resp[Void] = {
    LocalCacheContainer.addOrganization(org("code"))
  }

  @SUB("rbac/organization/remove/","删除组织信息","",
    """
      |code|string|组织编码|true
    ""","")
  def subscribeRemoveOrganization(args: Map[String, String], org: Map[String, String]): Resp[Void] = {
    LocalCacheContainer.removeOrganization(org("code"))
  }

  @SUB("rbac/resource/add/","添加资源信息","",
    """
      |method|string|资源方法|true
      |uri|string|资源路径|true
    ""","")
  def subscribeAddResource(args: Map[String, String], res: Map[String, String]): Resp[Void] = {
    LocalCacheContainer.addResource(res("method"), res("uri"))
  }

  @SUB("rbac/resource/remove/","删除资源信息","",
    """
      |code|string|资源编码|true
    ""","")
  def subscribeRemoveResource(args: Map[String, String], res: Map[String, String]): Resp[Void] = {
    val code = res("code")
    val Array(method, uri) = code.split(EZ.eb.ADDRESS_SPLIT_FLAG)
    LocalCacheContainer.removeResource(method, uri)
  }

  @SUB("rbac/role/add/","添加角色信息","",
    """
      |code|string|角色编码|true
      |resource_codes|array|此角色对应的资源|true
    ""","")
  def subscribeAddRole(args: Map[String, String], res: JsonNode): Resp[Void] = {
    LocalCacheContainer.addRole(res.get("code").asText(), res.get("resource_codes").map(_.asText()).toSet)
  }

  @SUB("rbac/role/remove/","删除角色信息","",
    """
      |code|string|角色编码|true
    ""","")
  def subscribeRemoveRole(args: Map[String, String], res: JsonNode): Resp[Void] = {
    LocalCacheContainer.removeRole(res.get("code").asText())
  }

}
