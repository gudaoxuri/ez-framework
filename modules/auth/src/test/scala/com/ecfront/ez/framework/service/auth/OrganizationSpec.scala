package com.ecfront.ez.framework.service.auth

import com.ecfront.common.{JsonHelper, Resp, StandardCode}
import com.ecfront.ez.framework.core.test.MockStartupSpec
import com.ecfront.ez.framework.service.auth.model.{EZ_Account, EZ_Organization, EZ_Resource, EZ_Role}
import com.ecfront.ez.framework.service.rpc.foundation.Method
import com.ecfront.ez.framework.service.rpc.http.HttpClientProcessor

class OrganizationSpec extends MockStartupSpec {

  test("Organization test") {

    EZ_Organization.deleteByCode("org1")
    EZ_Resource.deleteByCode("GET@/org/1/foo/")
    EZ_Role.deleteByCode("org1@user1")
    EZ_Account.deleteByLoginId("u1")

    EZ_Organization.save(EZ_Organization("org1", "组织1"))
    EZ_Resource.save(EZ_Resource(Method.GET, "/org/1/foo/", s"Fetch org1 Info"))

    val role = EZ_Role()
    role.flag = "user1"
    role.name = "User 1"
    role.organization_code = "org1"
    role.enable = true
    role.resource_codes = List(
      s"GET@/org/1/foo/",
      s"GET@/auth/manage/account/"
    )
    EZ_Role.save(role)

    var account = EZ_Account()
    account.login_id = "u1"
    account.name = "u1"
    account.email = "net@sunisle.org"
    account.password = "123"
    account.organization_code = "org1"
    account.enable = true
    account.role_codes = List(
      "org1@user1"
    )
    EZ_Account.save(account)

    // login
    val loginResp = AuthService.doLogin("u1", "123")
    assert(loginResp
      && loginResp.body.token != ""
      && loginResp.body.login_id == "u1"
      && loginResp.body.organization_code == "org1"
    )
    val token = loginResp.body.token
    val loginInfoResp = AuthService.doGetLoginInfo(token)
    assert(loginInfoResp
      && loginInfoResp.body.token != ""
      && loginInfoResp.body.login_id == "u1"
      && loginInfoResp.body.organization_code == "org1"
    )
    account = EZ_Account.getByLoginId(loginInfoResp.body.login_id).body
    assert(
      JsonHelper.toObject[Resp[EZ_Account]](
        HttpClientProcessor.get(s"http://127.0.0.1:8080/auth/manage/account/${account.id}/?__ez_token__=$token")
      ).code == StandardCode.UNAUTHORIZED)
    assert(
      JsonHelper.toObject[Resp[List[EZ_Account]]](
        HttpClientProcessor.get(s"http://127.0.0.1:8080/auth/manage/account/?__ez_token__=$token")
      )
    )
    assert(
      JsonHelper.toObject[Resp[Void]](
        HttpClientProcessor.get(s"http://127.0.0.1:8080/org/1/foo/?__ez_token__=$token")
      ).code == StandardCode.NOT_IMPLEMENTED)

  }

}


