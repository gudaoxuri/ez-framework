package com.ecfront.ez.framework.service.auth.manage

import com.ecfront.common.Resp
import com.ecfront.ez.framework.service.auth.{EZAuthContext, EZ_Menu}
import com.ecfront.ez.framework.service.rpc.foundation.{GET, RPC}
import com.ecfront.ez.framework.service.rpc.http.HTTP
import com.ecfront.ez.framework.service.rpc.http.scaffold.SimpleHttpService
import com.ecfront.ez.framework.service.storage.foundation.BaseStorage
import com.ecfront.ez.framework.service.storage.mongo.SortEnum

@RPC("/auth/manage/menu/")
@HTTP
object MenuService extends SimpleHttpService[EZ_Menu, EZAuthContext] {

  @GET("")
  override def rpcFind(parameter: Map[String, String], context: EZAuthContext): Resp[List[EZ_Menu]] = {
    EZ_Menu.findWithOpt(s"""{}""", Map("sort" -> SortEnum.DESC))
  }

  override protected val storageObj: BaseStorage[EZ_Menu] = EZ_Menu

}