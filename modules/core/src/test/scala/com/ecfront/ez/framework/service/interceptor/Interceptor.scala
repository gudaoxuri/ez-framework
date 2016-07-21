package com.ecfront.ez.framework.service.interceptor

import com.ecfront.common.Resp
import com.ecfront.ez.framework.core.interceptor.{EZInterceptor, EZInterceptorProcessor}
import com.ecfront.ez.framework.core.test.BasicSpec

import scala.collection.mutable

object Interceptor1 extends EZInterceptor[Void] {
  override val category: String = "inter1"

  override def before(obj: Void, context: mutable.Map[String, Any]): Resp[Void] = {
    println("before1")
    Resp.success(null)
  }

  override def after(obj: Void, context: mutable.Map[String, Any]): Resp[Void] = {
    println("after1")
    Resp.success(null)
  }
}

object Interceptor2 extends EZInterceptor[Void] {
  override val category: String = "inter1"

  override def before(obj: Void, context: mutable.Map[String, Any]): Resp[Void] = {
    println("before2")
    Resp.success(null)
  }

  override def after(obj: Void, context: mutable.Map[String, Any]): Resp[Void] = {
    println("after2")
    Resp.success(null)
  }
}

class InterceptorSpec extends BasicSpec {
  EZInterceptorProcessor.register("inter1", List(Interceptor1, Interceptor2))
  EZInterceptorProcessor.process[Void]("inter1", null, {
    (_, context) =>
      println("execute")
      Resp.success(null)
  })

}
