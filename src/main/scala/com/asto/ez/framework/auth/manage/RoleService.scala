package com.asto.ez.framework.auth.manage

import com.asto.ez.framework.auth.EZ_Role
import com.asto.ez.framework.rpc.{HTTP, RPC}
import com.asto.ez.framework.scaffold.SimpleRPCService
import com.asto.ez.framework.storage.BaseStorage

@RPC("/auth/manage/role/")
@HTTP
object RoleService extends SimpleRPCService[EZ_Role] {

  override protected val storageObj: BaseStorage[EZ_Role] = EZ_Role

}