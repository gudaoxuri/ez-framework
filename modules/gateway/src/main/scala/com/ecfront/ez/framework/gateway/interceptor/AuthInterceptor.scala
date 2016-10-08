package com.ecfront.ez.framework.gateway.interceptor

import com.ecfront.common.{AsyncResp, JsonHelper}
import com.ecfront.ez.framework.core.rpc.{OptInfo, RPCProcessor}
import com.ecfront.ez.framework.gateway.LocalCacheContainer
import com.ecfront.ez.framework.gateway.helper.AsyncRedisProcessor

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Http 权限拦截器
  */
object AuthInterceptor extends GatewayInterceptor {

  // 前端传入的token标识
  val VIEW_TOKEN_FLAG = "__ez_token__"

  var publicUriPrefix: String = _

  def init(_publicUriPrefix: String): Unit = {
    publicUriPrefix = _publicUriPrefix
  }

  override def before(obj: EZAPIContext, context: mutable.Map[String, Any], p: AsyncResp[EZAPIContext]): Unit = {
    obj.token =
      if (obj.parameters.contains(VIEW_TOKEN_FLAG)) {
        Some(obj.parameters(VIEW_TOKEN_FLAG))
      } else {
        None
      }
    if (obj.templateUri.startsWith(publicUriPrefix)) {
      // 可匿名访问
      p.success(obj)
    } else {
      if (obj.token.isEmpty) {
        // token不存在
        p.unAuthorized(s"【token】not exist，Request parameter must include【$VIEW_TOKEN_FLAG】")
      } else {
        // 根据token获取EZ_Token_Info
        AsyncRedisProcessor.get(RPCProcessor.TOKEN_INFO_FLAG + obj.token.get).onSuccess {
          case existTokenInfoR =>
            if (existTokenInfoR && existTokenInfoR.body != null) {
              obj.optInfo = Some(JsonHelper.toObject[OptInfo](existTokenInfoR.body))
              // 要访问的资源编码
              val resCode = LocalCacheContainer.getResourceCode(obj.channel, obj.method, obj.templateUri)
              if (resCode != null) {
                // 此资源需要认证
                if (LocalCacheContainer.existOrganization(obj.optInfo.get.organizationCode)) {
                  // 用户所属组织状态正常
                  if (LocalCacheContainer.existResourceByRoles(obj.optInfo.get.roleCodes, resCode)) {
                    // 登录用户所属角色列表中存在此资源
                    p.success(obj)
                  } else {
                    p.unAuthorized(s"Account【${obj.optInfo.get.name}】in【${obj.optInfo.get.organizationCode}】" +
                      s" no access to ${obj.channel}:${obj.method}:${obj.realUri}")
                  }
                } else {
                  p.unAuthorized(s"Organization【${obj.optInfo.get.organizationCode}】 not found")
                }
              } else {
                // 所有登录用户都可以访问
                p.success(obj)
              }
            } else {
              logger.warn("Token NOT exist")
              p.unAuthorized("Token NOT exist")
            }
        }
      }
    }
  }

  override def after(obj: EZAPIContext, context: mutable.Map[String, Any], p: AsyncResp[EZAPIContext]): Unit = {
    p.success(obj)
  }

}
