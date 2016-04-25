package com.ecfront.ez.framework.service.auth.manage

import com.ecfront.common.Resp
import com.ecfront.ez.framework.service.auth.EZAuthContext
import com.ecfront.ez.framework.service.auth.model.EZ_Menu
import com.ecfront.ez.framework.service.rpc.foundation.{GET, RPC}
import com.ecfront.ez.framework.service.rpc.http.HTTP
import com.ecfront.ez.framework.service.rpc.http.scaffold.SimpleHTTPService
import com.ecfront.ez.framework.service.storage.foundation.BaseStorage

/**
  * 菜单管理
  */
@RPC("/auth/manage/menu/")
@HTTP
object MenuService extends SimpleHTTPService[EZ_Menu, EZAuthContext] {

  /**
    * 查找所有菜单记录，按 `sort` 字段倒序排
    *
    * @param parameter 请求参数，可以包含`condition` 用于筛选条件
    * @param context   PRC上下文
    * @return 查找到的结果
    */
  @GET("")
  override def rpcFind(parameter: Map[String, String], context: EZAuthContext): Resp[List[EZ_Menu]] = {
    EZ_Menu.findWithSort()
  }

  override protected val storageObj: BaseStorage[EZ_Menu] = EZ_Menu

}