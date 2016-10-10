package com.ecfront.ez.framework.service.auth.manage

import com.ecfront.common.Resp
import com.ecfront.ez.framework.core.EZ
import com.ecfront.ez.framework.core.rpc._
import com.ecfront.ez.framework.service.auth._
import com.ecfront.ez.framework.service.auth.model.EZ_Account
import com.ecfront.ez.framework.service.jdbc.scaffold.SimpleRPCService
import com.ecfront.ez.framework.service.jdbc.{BaseStorage, Page}

/**
  * 账号管理
  */
@RPC("/auth/manage/account/")
object AccountService extends SimpleRPCService[EZ_Account] {

  override protected val storageObj: BaseStorage[EZ_Account] = EZ_Account

  /**
    * 获取登录账号的信息
    *
    * @param parameter 请求参数
    * @return 登录账号的信息
    */
  @GET("bylogin/")
  def getAccountByLoginId(parameter: Map[String, String]): Resp[AccountVO] = {
    if (EZ.context.optAccCode.nonEmpty) {
      val accountR = EZ_Account.getByCode(EZ.context.optAccCode)
      if (accountR) {
        if (accountR.body != null && accountR.body.enable) {
          val account = accountR.body
          val vo = AccountVO()
          vo.id = account.id
          vo.login_id = account.login_id
          vo.name = account.name
          vo.email = account.email
          vo.image = account.image
          vo.ext_id = account.ext_id
          vo.ext_info = account.ext_info
          Resp.success(vo)
        } else {
          Resp.unAuthorized("")
        }
      } else {
        accountR
      }
    } else {
      Resp.unAuthorized("")
    }
  }

  /**
    * 更新登录账号的信息
    *
    * @param parameter 请求参数
    * @param body      账号VO
    * @return 是否成功
    */
  @PUT("bylogin/")
  def updateAccountByLoginId(parameter: Map[String, String], body: AccountVO): Resp[Void] = {
    if (EZ.context.optAccCode.nonEmpty) {
      val accountR = EZ_Account.getByCode(EZ.context.optAccCode)
      if (accountR) {
        if (accountR.body != null) {
          val account = accountR.body
          // 验证密码
          if (EZ_Account.validateEncryptPwd(account.code, body.current_password, account.password)) {
            if (body.new_password != null && body.new_password.nonEmpty) {
              account.password = body.new_password
            } else {
              account.exchange_pwd = account.password
            }
            account.name = body.name
            account.email = body.email
            account.image = body.image
            EZ_Account.update(account)
          } else {
            logger.warn(s"Old Password Error by id:${EZ.context.optInfo.get.loginId} from ${EZ.context.sourceIP}")
            Resp.conflict("Old Password Error")
          }
        } else {
          Resp.unAuthorized("")
        }
      } else {
        accountR
      }
    } else {
      logger.warn("Login Info not found")
      Resp.unAuthorized("Login Info not found")
    }
  }

  @POST("")
  override def rpcSave(parameter: Map[String, String], body: String): Resp[EZ_Account] = {
    val resp = super.rpcSave(parameter, body)
    if (resp && resp.body != null) {
      resp.body.password = null
    }
    resp
  }

  @PUT(":id/")
  override def rpcUpdate(parameter: Map[String, String], body: String): Resp[EZ_Account] = {
    val resp = super.rpcUpdate(parameter, body)
    if (resp && resp.body != null) {
      resp.body.password = null
    }
    resp
  }

  @GET("enable/")
  override def rpcFindEnable(parameter: Map[String, String]): Resp[List[EZ_Account]] = {
    val resp = super.rpcFindEnable(parameter)
    if (resp && resp.body.nonEmpty) {
      resp.body.foreach {
        _.password = null
      }
    }
    resp
  }

  @GET("")
  override def rpcFind(parameter: Map[String, String]): Resp[List[EZ_Account]] = {
    val resp = super.rpcFind(parameter)
    if (resp && resp.body.nonEmpty) {
      resp.body.foreach {
        _.password = null
      }
    }
    resp
  }

  @GET("page/:pageNumber/:pageSize/")
  override def rpcPage(parameter: Map[String, String]): Resp[Page[EZ_Account]] = {
    val resp = super.rpcPage(parameter)
    if (resp && resp.body.objects.nonEmpty) {
      resp.body.objects.foreach {
        _.password = null
      }
    }
    resp
  }

  @GET(":id/")
  override def rpcGet(parameter: Map[String, String]): Resp[EZ_Account] = {
    val resp = super.rpcGet(parameter)
    if (resp && resp.body != null) {
      resp.body.password = null
    }
    resp
  }

}