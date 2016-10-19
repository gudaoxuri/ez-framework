package com.ecfront.ez.framework.component.perf.test1

import com.ecfront.common.Resp
import com.ecfront.ez.framework.core.rpc._
import com.ecfront.ez.framework.service.jdbc.BaseStorage
import com.ecfront.ez.framework.service.jdbc.scaffold.SimpleRPCService

@RPC("/test1/")
object TestService extends SimpleRPCService[EZ_Test] {

  @POST("longtime/")
  def longTimeReq(args: Map[String, String], body: String): Resp[Void] = {
    Thread.sleep(5000)
    Resp.success(null)
  }

  @POST("normal/")
  def normal(args: Map[String, String], body: String): Resp[String] = {
    Thread.sleep(500)
    Resp.success(body)
  }

  override protected val storageObj: BaseStorage[EZ_Test] = EZ_Test

}