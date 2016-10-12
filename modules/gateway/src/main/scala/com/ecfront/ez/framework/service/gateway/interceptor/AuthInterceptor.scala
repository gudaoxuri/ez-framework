package com.ecfront.ez.framework.service.gateway.interceptor

import com.ecfront.common.{AsyncResp, JsonHelper}
import com.ecfront.ez.framework.core.rpc.{OptInfo, RPCProcessor}
import com.ecfront.ez.framework.service.gateway.LocalCacheContainer
import com.ecfront.ez.framework.service.gateway.helper.AsyncRedisProcessor
import io.vertx.core.{AsyncResult, Handler}

import scala.collection.mutable

/**
  * Http 权限拦截器
  */
object AuthInterceptor extends GatewayInterceptor {

  var publicUriPrefix: String = _

  def init(_publicUriPrefix: String): Unit = {
    publicUriPrefix = _publicUriPrefix
  }

  override def before(obj: EZAPIContext, context: mutable.Map[String, Any], p: AsyncResp[EZAPIContext]): Unit = {
    obj.token =
      if (obj.parameters.contains(RPCProcessor.VIEW_TOKEN_FLAG)) {
        Some(obj.parameters(RPCProcessor.VIEW_TOKEN_FLAG))
      } else {
        None
      }
    if (obj.templateUri.startsWith(publicUriPrefix)) {
      // 可匿名访问
      p.success(obj)
    } else {
      if (obj.token.isEmpty) {
        // token不存在
        p.unAuthorized(s"【token】not exist，Request parameter must include【${RPCProcessor.VIEW_TOKEN_FLAG}】")
      } else {
        // 根据token获取EZ_Token_Info
        AsyncRedisProcessor.client().get(RPCProcessor.TOKEN_INFO_FLAG + obj.token.get, new Handler[AsyncResult[String]] {
          override def handle(event: AsyncResult[String]): Unit = {
            if (event.succeeded()) {
              if (event.result() != null) {
                obj.optInfo = Some(JsonHelper.toObject[OptInfo](event.result()))
                // 此资源需要认证
                if (LocalCacheContainer.existOrganization(obj.optInfo.get.organizationCode)) {
                  // 用户所属组织状态正常
                  if (LocalCacheContainer.existResourceByRoles(obj.method, obj.templateUri, obj.optInfo.get.roleCodes)) {
                    // 登录用户所属角色列表中存在此资源
                    p.success(obj)
                  } else {
                    p.unAuthorized(s"Account【${obj.optInfo.get.name}】in【${obj.optInfo.get.organizationCode}】" +
                      s" no access to ${obj.method}:${obj.realUri}")
                  }
                } else {
                  p.unAuthorized(s"Organization【${obj.optInfo.get.organizationCode}】 not found")
                }
              } else {
                logger.warn("Token NOT exist")
                p.unAuthorized("Token NOT exist")
              }
            } else {
              logger.error(s"Redis get error:${event.cause().getMessage}", event.cause())
              p.serverUnavailable(s"Redis get error:${event.cause().getMessage}")
            }
          }
        })
      }
    }
  }

  override def after(obj: EZAPIContext, context: mutable.Map[String, Any], p: AsyncResp[EZAPIContext]): Unit = {
    p.success(obj)
  }

}
