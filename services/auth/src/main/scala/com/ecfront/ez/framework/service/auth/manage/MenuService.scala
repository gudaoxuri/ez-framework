package com.ecfront.ez.framework.service.auth.manage

import com.ecfront.common.Resp
import com.ecfront.ez.framework.core.rpc.{GET, RPC}
import com.ecfront.ez.framework.service.auth.model.EZ_Menu
import com.ecfront.ez.framework.service.jdbc.BaseStorage
import com.ecfront.ez.framework.service.jdbc.scaffold.SimpleRPCService

/**
  * 菜单管理
  */
@RPC("/ez/auth/manage/menu/")
object MenuService extends SimpleRPCService[EZ_Menu] {

  /**
    * 查找所有菜单记录，按 `sort` 字段倒序排
    *
    * @param parameter 请求参数，可以包含`condition` 用于筛选条件
    * @return 查找到的结果
    */
  @GET("")
  override def rpcFind(parameter: Map[String, String]): Resp[List[EZ_Menu]] = {
    EZ_Menu.findWithSort()
  }

  override protected val storageObj: BaseStorage[EZ_Menu] = EZ_Menu

}