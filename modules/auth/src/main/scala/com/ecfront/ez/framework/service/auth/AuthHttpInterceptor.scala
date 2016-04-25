package com.ecfront.ez.framework.service.auth

import com.ecfront.common.AsyncResp
import com.ecfront.ez.framework.service.auth.model._
import com.ecfront.ez.framework.service.rpc.foundation.EZRPCContext
import com.ecfront.ez.framework.service.rpc.http.HttpInterceptor

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Http 权限拦截器
  */
object AuthHttpInterceptor extends HttpInterceptor {

  override def before(obj: EZRPCContext, context: mutable.Map[String, Any], p: AsyncResp[EZRPCContext]): Unit = {
    val authContext: EZAuthContext = obj
    authContext.token =
      if (authContext.parameters.contains(AuthService.VIEW_TOKEN_FLAG)) {
        Some(authContext.parameters(AuthService.VIEW_TOKEN_FLAG))
      } else {
        None
      }
    if (authContext.templateUri.startsWith(ServiceAdapter.publicUriPrefix)) {
      // 可匿名访问
      p.success(authContext)
    } else {
      if (authContext.token.isEmpty) {
        // token不存在
        p.unAuthorized(s"【token】not exist，Request parameter must include【${AuthService.VIEW_TOKEN_FLAG}】")
      } else {
        // 根据token获取EZ_Token_Info
        CacheManager.getTokenInfoAsync(authContext.token.get).onSuccess {
          case tokenR =>
            if (tokenR && tokenR.body != null) {
              val tokenInfo = tokenR.body
              authContext.loginInfo = Some(tokenInfo)
              // 要访问的资源编码
              val resourceCode = EZ_Resource.assembleCode(authContext.method, authContext.templateUri)
              CacheManager.existResource(resourceCode).onSuccess {
                case existResR =>
                  if (existResR.body) {
                    // 此资源需要认证
                    CacheManager.existOrganizationAsync(tokenInfo.organization_code).onSuccess {
                      case existOrgR =>
                        if (existOrgR.body) {
                          // 用户所属组织状态正常
                          CacheManager.existResourceByRoles(tokenInfo.role_codes, resourceCode).onSuccess {
                            case existMatchResR =>
                              if (existMatchResR.body) {
                                // 登录用户所属角色列表中存在此资源
                                p.success(authContext)
                              } else {
                                p.unAuthorized(s"Account【${tokenInfo.name}】in【${tokenInfo.organization_code}】" +
                                  s" no access to ${authContext.method}:${authContext.realUri}】")
                              }
                          }
                        } else {
                          p.unAuthorized(s"Organization【${tokenInfo.organization_code}】 not found")
                        }
                    }

                  } else {
                    // 所有登录用户都可以访问
                    p.success(authContext)
                  }
              }
            } else {
              p.resp(tokenR)
            }
        }
      }
    }
  }

  override def after(obj: EZRPCContext, context: mutable.Map[String, Any], p: AsyncResp[EZRPCContext]): Unit = {
    p.success(obj)
  }

}
