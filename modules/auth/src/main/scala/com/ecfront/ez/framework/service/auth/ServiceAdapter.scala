package com.ecfront.ez.framework.service.auth

import com.ecfront.common.Resp
import com.ecfront.ez.framework.core.EZServiceAdapter
import com.ecfront.ez.framework.core.interceptor.EZAsyncInterceptorProcessor
import com.ecfront.ez.framework.service.rpc.foundation.AutoBuildingProcessor
import com.ecfront.ez.framework.service.rpc.http.{HTTP, HttpInterceptor}
import io.vertx.core.json.JsonObject

object ServiceAdapter extends EZServiceAdapter[JsonObject] {

  var publicUriPrefix: String = "/public/"
  var allowRegister: Boolean = false
  var loginUrl: String = ""

  override def init(parameter: JsonObject): Resp[String] = {
    publicUriPrefix = parameter.getString("publicUriPrefix", "/public/")
    allowRegister = parameter.getBoolean("allowRegister", false)
    loginUrl = parameter.getString("loginUrl", "#/auth/login")
    if (!loginUrl.toLowerCase().startsWith("http")) {
      loginUrl = com.ecfront.ez.framework.service.rpc.http.ServiceAdapter.webUrl + loginUrl
    }
    EZAsyncInterceptorProcessor.register(HttpInterceptor.category, AuthHttpInterceptor)
    AutoBuildingProcessor.autoBuilding[HTTP]("com.ecfront.ez.framework.service.auth", classOf[HTTP])
    Initiator.init()
    Resp.success("")
  }

  override def destroy(parameter: JsonObject): Resp[String] = {
    Resp.success("")
  }

  // 服务动态依赖处理方法，如果服务需要根据配置使用不同依赖请重写此方法
  override def getDynamicDependents(parameter: JsonObject): Set[String] = {
    if (parameter.containsKey("allowRegister") && parameter.getBoolean("allowRegister")) {
      Set(
        com.ecfront.ez.framework.service.rpc.http.ServiceAdapter.serviceName,
        com.ecfront.ez.framework.service.storage.mongo.ServiceAdapter.serviceName,
        com.ecfront.ez.framework.service.email.ServiceAdapter.serviceName,
        com.ecfront.ez.framework.service.redis.ServiceAdapter.serviceName
      )
    } else {
      Set(
        com.ecfront.ez.framework.service.rpc.http.ServiceAdapter.serviceName,
        com.ecfront.ez.framework.service.storage.mongo.ServiceAdapter.serviceName
      )
    }
  }

  override var serviceName: String = "auth"

}


