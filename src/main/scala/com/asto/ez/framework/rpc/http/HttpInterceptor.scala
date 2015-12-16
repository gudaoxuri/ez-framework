package com.asto.ez.framework.rpc.http

import com.asto.ez.framework.EZContext
import com.asto.ez.framework.interceptor.EZInterceptor

trait HttpInterceptor extends EZInterceptor[EZContext] {

  override val category: String = HttpInterceptor.category

}

object HttpInterceptor {

  val category: String = "rpc_http"

}
