package com.ecfront.ez.framework.component.perf.test2

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

  override protected val storageObj: BaseStorage[EZ_Test] = EZ_Test

}