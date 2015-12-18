package com.asto.ez.framework.auth.manage

import com.asto.ez.framework.auth.EZ_Resource
import com.asto.ez.framework.rpc.{HTTP, RPC}
import com.asto.ez.framework.scaffold.SimpleRPCService
import com.asto.ez.framework.storage.BaseStorage

@RPC("/auth/manage/resource/")
@HTTP
object ResourceService extends SimpleRPCService[EZ_Resource] {

  override protected val storageObj: BaseStorage[EZ_Resource] = EZ_Resource

}