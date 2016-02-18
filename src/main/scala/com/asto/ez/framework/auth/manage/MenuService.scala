package com.asto.ez.framework.auth.manage

import com.asto.ez.framework.auth.{EZ_Menu, EZ_Resource}
import com.asto.ez.framework.rpc.{HTTP, RPC}
import com.asto.ez.framework.scaffold.SimpleRPCService
import com.asto.ez.framework.storage.BaseStorage

@RPC("/auth/manage/menu/")
@HTTP
object MenuService extends SimpleRPCService[EZ_Menu] {

  override protected val storageObj: BaseStorage[EZ_Menu] = EZ_Menu

}