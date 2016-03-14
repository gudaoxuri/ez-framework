package com.ecfront.ez.framework.service.auth

import com.ecfront.common.EncryptHelper
import com.ecfront.ez.framework.core.test.MockStartupSpec
import com.ecfront.ez.framework.service.rpc.foundation.Method
import com.ecfront.ez.framework.service.storage.foundation.BaseModel

class AuthSpec extends MockStartupSpec {

  test("Auth Test") {

    val resources = EZ_Resource.find("{}").body
    assert(
      resources.size == 57
        && resources.head.method == Method.GET
        && resources.head.uri == "/auth/manage/organization/"
        && resources.head.code == Method.GET + BaseModel.SPLIT + "/auth/manage/organization/"
        && resources.head.enable
    )

    val roles = EZ_Role.find("{}").body
    assert(
      roles.size == 2
        && roles.head.code == BaseModel.SPLIT + EZ_Role.SYSTEM_ROLE_CODE
        && roles.head.flag == EZ_Role.SYSTEM_ROLE_CODE
        && roles.head.name == "System"
        && roles.head.resource_codes.size == 57
        && roles.head.resource_codes.head == s"${Method.GET}${BaseModel.SPLIT}/auth/manage/organization/"
    )

    val accounts = EZ_Account.find("").body
    assert(
      accounts.size == 1
        && accounts.head.login_id == EZ_Account.SYSTEM_ACCOUNT_CODE
        && accounts.head.name == "Sys Admin"
        && accounts.head.password == EncryptHelper.encrypt(EZ_Account.SYSTEM_ACCOUNT_CODE + "admin")
        && accounts.head.organization_code == ""
        && accounts.head.role_codes.size == 1
    )

    // login
    assert(!AuthService.doLogin(EZ_Account.SYSTEM_ACCOUNT_CODE, "errorpwd"))
    val loginResp = AuthService.doLogin(EZ_Account.SYSTEM_ACCOUNT_CODE, "admin")
    assert(loginResp
      && loginResp.body.token != ""
      && loginResp.body.login_id == EZ_Account.SYSTEM_ACCOUNT_CODE
      && loginResp.body.login_name == "Sys Admin"
      && loginResp.body.organization_code == ""
      && loginResp.body.organization_name == "default"
      && loginResp.body.role_info == Map(
      BaseModel.SPLIT + EZ_Role.SYSTEM_ROLE_CODE -> "System"
    )
      && loginResp.body.ext_id == ""
      && loginResp.body.last_login_time != 0
    )
    val token = loginResp.body.token
    // get login info
    val loginInfoResp = AuthService.doGetLoginInfo(token)
    assert(loginInfoResp
      && loginInfoResp.body.token != ""
      && loginInfoResp.body.login_id == EZ_Account.SYSTEM_ACCOUNT_CODE
      && loginInfoResp.body.login_name == "Sys Admin"
      && loginInfoResp.body.organization_code == ""
      && loginInfoResp.body.organization_name == "default"
      && loginInfoResp.body.role_info == Map(
      BaseModel.SPLIT + EZ_Role.SYSTEM_ROLE_CODE -> "System"
    )
      && loginInfoResp.body.ext_id == ""
      && loginInfoResp.body.last_login_time != 0
    )
    // logout
    assert(AuthService.doLogout(token))
    // get login info
    assert(!AuthService.doGetLoginInfo(token))

  }

}


