package com.asto.ez.framework.auth.manage

import com.asto.ez.framework.EZContext
import com.asto.ez.framework.auth.EZ_Menu
import com.asto.ez.framework.rpc.{GET, HTTP, RPC}
import com.asto.ez.framework.scaffold.SimpleRPCService
import com.asto.ez.framework.storage.BaseStorage
import com.asto.ez.framework.storage.mongo.SortEnum
import com.ecfront.common.AsyncResp
import scala.concurrent.ExecutionContext.Implicits.global

@RPC("/auth/manage/menu/")
@HTTP
object MenuService extends SimpleRPCService[EZ_Menu] {

  @GET("")
  override def _rpc_find(parameter: Map[String, String], p: AsyncResp[List[EZ_Menu]], context: EZContext): Unit = {
    EZ_Menu.findWithOpt(s"""{}""", Map("sort" -> SortEnum.DESC)).onSuccess {
      case resp => p.resp(resp)
    }
  }

  override protected val storageObj: BaseStorage[EZ_Menu] = EZ_Menu

}