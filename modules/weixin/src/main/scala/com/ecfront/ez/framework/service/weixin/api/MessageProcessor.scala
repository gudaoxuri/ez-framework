package com.ecfront.ez.framework.service.weixin.api

import com.ecfront.common.Resp
import com.ecfront.ez.framework.service.auth.EZAuthContext
import com.ecfront.ez.framework.service.rpc.foundation.{POST, RPC}
import com.ecfront.ez.framework.service.rpc.http.HTTP
import com.ecfront.ez.framework.service.weixin.BaseProcessor

@RPC("/public/weixin/message/")
@HTTP
object MessageProcessor extends BaseProcessor {

  @POST("listening/")
  def listening(parameter: Map[String, String], body: String, context: EZAuthContext): Resp[Void] = {
    Resp.success(null)
  }

}
