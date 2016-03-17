package com.ecfront.ez.framework.service.rpc.websocket.test

import com.ecfront.common.Resp
import com.ecfront.ez.framework.service.rpc.foundation.{REQUEST, EZRPCContext, POST, RPC}
import com.ecfront.ez.framework.service.rpc.websocket.WebSocket
import com.typesafe.scalalogging.slf4j.LazyLogging

@RPC("/resource/")
@WebSocket
object ResourceService extends LazyLogging {

  @REQUEST("")
  def save(parameter: Map[String, String], body: EZ_Resource, context: EZRPCContext): Resp[EZ_Resource] = {
    println(body)
    Resp.success(body)
  }

}