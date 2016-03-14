package com.ecfront.ez.framework.service.rpc.http.test

import com.ecfront.ez.framework.service.rpc.foundation.{EZRPCContext, RPC}
import com.ecfront.ez.framework.service.rpc.http.HTTP
import com.ecfront.ez.framework.service.rpc.http.scaffold.SimpleHttpService
import com.ecfront.ez.framework.service.storage.foundation.BaseStorage

@RPC("/resource/")
@HTTP
object ResourceService extends SimpleHttpService[EZ_Resource,EZRPCContext] {

  override protected val storageObj: BaseStorage[EZ_Resource] = EZ_Resource

}