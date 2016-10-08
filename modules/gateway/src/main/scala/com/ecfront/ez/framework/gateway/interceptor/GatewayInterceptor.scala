package com.ecfront.ez.framework.gateway.interceptor

import com.ecfront.ez.framework.core.interceptor.EZAsyncInterceptor
import com.ecfront.ez.framework.service.rpc.foundation.EZRPCContext

trait GatewayInterceptor extends EZAsyncInterceptor[EZAPIContext] {

  override val category: String = GatewayInterceptor.category

}

object GatewayInterceptor {

  val category: String = "gateway"

}
