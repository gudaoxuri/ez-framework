package com.ecfront.ez.framework.service.rpc.http

import com.ecfront.ez.framework.core.interceptor.EZAsyncInterceptor
import com.ecfront.ez.framework.service.rpc.foundation.EZRPCContext

/**
  * HTTP 拦截器
  *
  * 用于权限过滤等
  */
trait HttpInterceptor extends EZAsyncInterceptor[EZRPCContext] {

  override val category: String = HttpInterceptor.category

}

object HttpInterceptor {

  val category: String = "rpc_http"

}
