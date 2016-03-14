package com.ecfront.ez.framework.service.rpc.http

import com.ecfront.ez.framework.core.interceptor.EZAsyncInterceptor
import com.ecfront.ez.framework.service.rpc.foundation.EZRPCContext

trait HttpInterceptor extends EZAsyncInterceptor[EZRPCContext] {

  override val category: String = HttpInterceptor.category

}

object HttpInterceptor {

  val category: String = "rpc_http"

}
