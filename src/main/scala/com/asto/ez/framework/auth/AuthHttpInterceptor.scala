package com.asto.ez.framework.auth

import com.asto.ez.framework.rpc.http.HttpInterceptor
import com.asto.ez.framework.{EZContext, EZGlobal}
import com.ecfront.common.AsyncResp
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.mutable

object AuthHttpInterceptor extends HttpInterceptor {

  override def before(obj: EZContext, context: mutable.Map[String, Any], p: AsyncResp[EZContext]): Unit = {
    obj.token = if (obj.parameters.contains(EZ_Token_Info.TOKEN_FLAG)) obj.parameters(EZ_Token_Info.TOKEN_FLAG) else ""
    if (obj.templateUri.startsWith(EZGlobal.ez_rpc_http_public_uri_prefix_path)) {
      //可匿名访问
      p.success(obj)
    } else {
      if (obj.token == "") {
        p.unAuthorized(s"【token】不存在，请确认请求参数包含【${EZ_Token_Info.TOKEN_FLAG}】")
      } else {
        EZ_Token_Info.getById(obj.token).onSuccess {
          case tokenResp =>
            if (tokenResp && tokenResp.body != null) {
              obj.login_Id = tokenResp.body.login_id
              obj.organization_code = tokenResp.body.organization.code
              val resourceCode = EZ_Resource.assembleCode(obj.method, obj.templateUri)
              if (!tokenResp.body.roles.exists(_.resource_codes.contains(resourceCode))) {
                EZ_Resource.existByCond(s"""{"code":"$resourceCode"}""").onSuccess {
                  case resResp =>
                    if (resResp && !resResp.body) {
                      //所有登录用户都可以访问
                      p.success(obj)
                    } else {
                      p.unAuthorized(s"用户【${tokenResp.body.login_name}】没有权限访问资源【${obj.realUri}】")
                    }
                }
              } else {
                p.success(obj)
              }
            } else {
              p.unAuthorized("【token】不存在")
            }
        }
      }
    }
  }

  override def after(obj: EZContext, context: mutable.Map[String, Any], p: AsyncResp[EZContext]): Unit = {
    p.success(obj)
  }

}
