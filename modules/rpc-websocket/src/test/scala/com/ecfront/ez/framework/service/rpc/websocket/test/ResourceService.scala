package com.ecfront.ez.framework.service.rpc.websocket.test

import com.ecfront.ez.framework.service.rpc.foundation.scaffold.SimpleRPCService
import com.ecfront.ez.framework.service.rpc.foundation.{EZRPCContext, RPC}
import com.ecfront.ez.framework.service.rpc.websocket.WebSocket
import com.ecfront.ez.framework.service.storage.foundation.BaseStorage

@RPC("/resource/")
@WebSocket
object ResourceService extends SimpleRPCService[EZ_Resource, EZRPCContext] {

  override protected val storageObj: BaseStorage[EZ_Resource] = EZ_Resource

}