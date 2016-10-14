package com.ecfront.ez.framework.service.gateway.interceptor

import com.ecfront.ez.framework.core.interceptor.EZAsyncInterceptor

trait GatewayInterceptor extends EZAsyncInterceptor[EZAPIContext] {

  override val category: String = GatewayInterceptor.category

}

object GatewayInterceptor {

  val category: String = "gateway"

}
