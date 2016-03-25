package com.ecfront.ez.framework.service.weixin.api

import com.ecfront.common.Resp
import com.ecfront.ez.framework.service.auth.EZAuthContext
import com.ecfront.ez.framework.service.rpc.foundation.{GET, POST, RPC, Raw}
import com.ecfront.ez.framework.service.rpc.http.HTTP
import com.ecfront.ez.framework.service.weixin.BaseProcessor

@RPC("/public/weixin/message/")
@HTTP
object MessageProcessor extends BaseProcessor {

  @GET("listening/")
  def listening(parameter: Map[String, String], context: EZAuthContext): Resp[Raw] = {
    if (validity(parameter("signature"), parameter("timestamp"), parameter("nonce"))) {
      Resp.success(Raw(parameter("echostr")))
    } else {
      Resp.success(Raw(""))
    }
  }

}
