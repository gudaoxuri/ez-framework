package com.ecfront.ez.framework.service.auth.manage

import com.ecfront.common.Resp
import com.ecfront.ez.framework.core.rpc._
import com.ecfront.ez.framework.service.auth.model.EZ_Account
import com.ecfront.ez.framework.service.jdbc.scaffold.SimpleRPCService
import com.ecfront.ez.framework.service.jdbc.{BaseStorage, Page}

/**
  * 账号管理
  */
@RPC("/ez/auth/manage/account/", "EZ-账号管理", "")
object AccountService extends SimpleRPCService[EZ_Account] {

  override protected val storageObj: BaseStorage[EZ_Account] = EZ_Account

  @POST("", "保存", "",
    """
      login_id|String|登录Id|true
      |name|String|姓名|true
      |image|String|头像|false
      |password|String|密码|true
      |email|String|邮箱|true
      |role_codes|Array|所属角色编码|true
      |organization_code|String|所属组织编码|true
      |enable|Boolean|是否启用|true
      |ext_info|String|扩展信息，json格式|false
    """, "")
  override def rpcSave(parameter: Map[String, String], body: String): Resp[EZ_Account] = {
    val resp = super.rpcSave(parameter, body)
    if (resp && resp.body != null) {
      resp.body.password = null
    }
    resp
  }

  @PUT(":id/", "更新", "",
    """
      |id|String|主键|true
      |login_id|String|登录Id|true
      |name|String|姓名|true
      |image|String|头像|false
      |password|String|密码|true
      |email|String|邮箱|true
      |role_codes|Array|所属角色编码|true
      |organization_code|String|所属组织编码|true
      |enable|Boolean|是否启用|true
      |ext_info|String|扩展信息，json格式|false
    """, "")
  override def rpcUpdate(parameter: Map[String, String], body: String): Resp[EZ_Account] = {
    val resp = super.rpcUpdate(parameter, body)
    if (resp && resp.body != null) {
      resp.body.password = null
    }
    resp
  }

  @PUT("uuid/:uuid/", "根据业务主键更新", "",
    """
      |id|String|主键|true
      |login_id|String|登录Id|true
      |name|String|姓名|true
      |image|String|头像|false
      |password|String|密码|true
      |email|String|邮箱|true
      |role_codes|Array|所属角色编码|true
      |organization_code|String|所属组织编码|true
      |enable|Boolean|是否启用|true
      |ext_info|String|扩展信息，json格式|false
    """, "")
  override def rpcUpdateByUUID(parameter: Map[String, String], body: String): Resp[EZ_Account] = {
    val resp = super.rpcUpdateByUUID(parameter, body)
    if (resp && resp.body != null) {
      resp.body.password = null
    }
    resp
  }

  @GET("", "查询所有记录", "TIP: url参数`condition`表示筛选条件，限制性sql形式", "")
  override def rpcFind(parameter: Map[String, String]): Resp[List[EZ_Account]] = {
    val resp = super.rpcFind(parameter)
    if (resp && resp.body.nonEmpty) {
      resp.body.foreach {
        _.password = null
      }
    }
    resp
  }

  @GET("enable/", "查询启用的记录", "TIP: url参数`condition`表示筛选条件，限制性sql形式", "")
  override def rpcFindEnable(parameter: Map[String, String]): Resp[List[EZ_Account]] = {
    val resp = super.rpcFindEnable(parameter)
    if (resp && resp.body.nonEmpty) {
      resp.body.foreach {
        _.password = null
      }
    }
    resp
  }

  @GET("page/:pageNumber/:pageSize/", "分页查询记录", "TIP: url参数`pageNumber`表示当前页，从1开始，`pageSize`表示每页条数，`condition`表示筛选条件，限制性sql形式", "")
  override def rpcPage(parameter: Map[String, String]): Resp[Page[EZ_Account]] = {
    val resp = super.rpcPage(parameter)
    if (resp && resp.body.objects.nonEmpty) {
      resp.body.objects.foreach {
        _.password = null
      }
    }
    resp
  }

  @GET("enable/page/:pageNumber/:pageSize/", "分页查询启用的记录", "TIP: url参数`pageNumber`表示当前页，从1开始，`pageSize`表示每页条数，`condition`表示筛选条件，限制性sql形式", "")
  override def rpcPageEnable(parameter: Map[String, String]): Resp[Page[EZ_Account]] = {
    val resp = super.rpcPageEnable(parameter)
    if (resp && resp.body.objects.nonEmpty) {
      resp.body.objects.foreach {
        _.password = null
      }
    }
    resp
  }

  @GET(":id/", "获取一条记录", "", "")
  override def rpcGet(parameter: Map[String, String]): Resp[EZ_Account] = {
    val resp = super.rpcGet(parameter)
    if (resp && resp.body != null) {
      resp.body.password = null
    }
    resp
  }

  @GET("uuid/:uuid/", "根据业务主键获取一条记录", "", "")
  override def rpcGetByUUID(parameter: Map[String, String]): Resp[EZ_Account] = {
    val resp = super.rpcGetByUUID(parameter)
    if (resp && resp.body != null) {
      resp.body.password = null
    }
    resp
  }

}