package com.asto.ez.framework.auth.manage

import com.asto.ez.framework.auth.EZ_Account
import com.asto.ez.framework.rpc.{HTTP, RPC}
import com.asto.ez.framework.scaffold.SimpleRPCService
import com.asto.ez.framework.storage.BaseStorage

@RPC("/auth/manage/account/")
@HTTP
object AccountService extends SimpleRPCService[EZ_Account] {

  override protected val storageObj: BaseStorage[EZ_Account] = EZ_Account

}