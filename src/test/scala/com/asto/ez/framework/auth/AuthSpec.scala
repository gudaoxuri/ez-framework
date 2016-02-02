package com.asto.ez.framework.auth

import com.asto.ez.framework.MockStartupSpec
import com.asto.ez.framework.rpc.Method
import com.asto.ez.framework.storage.BaseModel
import com.ecfront.common.{AsyncResp, EncryptHelper, Resp}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Promise}

class AuthSpec extends MockStartupSpec {

  test("Auth Test") {

    Thread.sleep(4000)

    val resources = Await.result(EZ_Resource.find("{}"), Duration.Inf).body
    assert(
      resources.size == 32
        && resources.head.method == Method.GET
        && resources.head.uri == "/auth/manage/organization/"
        && resources.head.code == Method.GET + BaseModel.SPLIT + "/auth/manage/organization/"
        && resources.head.enable
    )

    val roles = Await.result(EZ_Role.find("{}"), Duration.Inf).body
    assert(
      roles.size == 1
        && roles.head.code == BaseModel.SPLIT + EZ_Role.SYSTEM_ROLE_CODE
        && roles.head.flag == EZ_Role.SYSTEM_ROLE_CODE
        && roles.head.name == "System Role"
        && roles.head.resource_codes.size == 32
        && roles.head.resource_codes.head == s"${Method.GET}${BaseModel.SPLIT}/auth/manage/organization/"
    )

    val accounts = Await.result(EZ_Account.find("{}"), Duration.Inf).body
    assert(
      accounts.size == 1
        && accounts.head.login_id == EZ_Account.SYSTEM_ACCOUNT_CODE
        && accounts.head.name == "System Administrator"
        && accounts.head.password == EncryptHelper.encrypt(EZ_Account.SYSTEM_ACCOUNT_CODE + "admin")
        && accounts.head.organization_code == ""
        && accounts.head.role_codes.size == 1
    )

    //login
    val loginP1 = Promise[Resp[Token_Info_VO]]()
    AuthService.doLogin(EZ_Account.SYSTEM_ACCOUNT_CODE, "errorpwd", AsyncResp(loginP1))
    assert(!Await.result(loginP1.future, Duration.Inf))
    val loginP2 = Promise[Resp[Token_Info_VO]]()
    AuthService.doLogin(EZ_Account.SYSTEM_ACCOUNT_CODE, "admin", AsyncResp(loginP2))
    val loginResp = Await.result(loginP2.future, Duration.Inf)
    assert(loginResp
      && loginResp.body.token != ""
      && loginResp.body.login_id == EZ_Account.SYSTEM_ACCOUNT_CODE
      && loginResp.body.login_name == "System Administrator"
      && loginResp.body.organization_code == ""
      && loginResp.body.organization_name == "default"
      && loginResp.body.role_info == Map(
      BaseModel.SPLIT + EZ_Role.SYSTEM_ROLE_CODE -> "System Role"
    )
      && loginResp.body.ext_id == null
      && loginResp.body.last_login_time != 0
    )
    val token = loginResp.body.token
    //get login info
    val loginInfoP1 = Promise[Resp[Token_Info_VO]]()
    AuthService.doGetLoginInfo(token, AsyncResp(loginInfoP1))
    var loginInfoResp = Await.result(loginInfoP1.future, Duration.Inf)
    assert(loginInfoResp
      && loginInfoResp.body.token != ""
      && loginInfoResp.body.login_id == EZ_Account.SYSTEM_ACCOUNT_CODE
      && loginInfoResp.body.login_name == "System Administrator"
      && loginInfoResp.body.organization_code == ""
      && loginInfoResp.body.organization_name == "default"
      && loginInfoResp.body.role_info == Map(
      BaseModel.SPLIT + EZ_Role.SYSTEM_ROLE_CODE -> "System Role"
    )
      && loginInfoResp.body.ext_id == null
      && loginInfoResp.body.last_login_time != 0
    )
    //logout
    val logoutP1 = Promise[Resp[Void]]()
    AuthService.doLogout(token, AsyncResp(logoutP1))
    assert(Await.result(logoutP1.future, Duration.Inf))
    //get login info
    val loginInfoP2 = Promise[Resp[Token_Info_VO]]()
    AuthService.doGetLoginInfo(token, AsyncResp(loginInfoP2))
    loginInfoResp = Await.result(loginInfoP2.future, Duration.Inf)
    assert(!loginInfoResp)

  }

}


