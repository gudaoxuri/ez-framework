package com.ecfront.ez.framework.service.auth

import com.ecfront.ez.framework.core.test.MockStartupSpec
import com.ecfront.ez.framework.service.auth.model.{EZ_Account, EZ_Resource, EZ_Role}
import com.ecfront.ez.framework.service.rpc.foundation.Method
import com.ecfront.ez.framework.service.storage.foundation.BaseModel

class AuthSpec extends MockStartupSpec {

  test("Auth Test") {

    val resources = EZ_Resource.find("").body
    assert(
      resources.size == 55
        && resources.head.method == Method.GET
        && resources.head.uri == "/auth/manage/organization/"
        && resources.head.code == Method.GET + BaseModel.SPLIT + "/auth/manage/organization/"
        && resources.head.enable
    )

    val roles = EZ_Role.find("").body
    assert(
      roles.size == 2
        && roles.last.code == BaseModel.SPLIT + EZ_Role.SYSTEM_ROLE_FLAG
        && roles.last.flag == EZ_Role.SYSTEM_ROLE_FLAG
        && roles.last.name == "System"
        && roles.last.resource_codes.size == 55
        && roles.last.resource_codes.head == s"${Method.GET}${BaseModel.SPLIT}/auth/manage/organization/"
    )

    val accounts = EZ_Account.find("").body
    assert(
      accounts.size == 2
        && accounts.last.login_id == EZ_Account.SYSTEM_ACCOUNT_LOGIN_ID
        && accounts.last.name == "Sys Admin"
        && EZ_Account.validateEncryptPwd(accounts.last.code, "admin", accounts.last.password)
        && accounts.last.organization_code == ServiceAdapter.defaultOrganizationCode
        && accounts.last.role_codes.size == 1
    )

    // login
    assert(!AuthService.doLogin(EZ_Account.SYSTEM_ACCOUNT_LOGIN_ID, "errorpwd", "", "", new EZAuthContext))
    val loginResp = AuthService.doLogin(EZ_Account.SYSTEM_ACCOUNT_LOGIN_ID, "admin", "", "", new EZAuthContext)
    assert(loginResp
      && loginResp.body.token != ""
      && loginResp.body.login_id == EZ_Account.SYSTEM_ACCOUNT_LOGIN_ID
      && loginResp.body.name == "Sys Admin"
      && loginResp.body.organization_code == ""
      && loginResp.body.role_codes == List(
      BaseModel.SPLIT + EZ_Role.SYSTEM_ROLE_FLAG
    )
    )
    val token = loginResp.body.token
    // get login info
    val loginInfoResp = CacheManager.getTokenInfo(token)
    assert(loginInfoResp
      && loginInfoResp.body.token != ""
      && loginInfoResp.body.login_id == EZ_Account.SYSTEM_ACCOUNT_LOGIN_ID
      && loginInfoResp.body.organization_code == ""
      && loginInfoResp.body.role_codes == List(
      BaseModel.SPLIT + EZ_Role.SYSTEM_ROLE_FLAG
    )
    )
    // logout
    assert(AuthService.doLogout(token))
    // get login info
    assert(!CacheManager.getTokenInfo(token))

  }

}


