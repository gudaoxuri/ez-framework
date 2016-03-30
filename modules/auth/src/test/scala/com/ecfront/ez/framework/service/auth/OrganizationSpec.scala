package com.ecfront.ez.framework.service.auth

import com.ecfront.common.StandardCode
import com.ecfront.ez.framework.core.test.MockStartupSpec
import com.ecfront.ez.framework.service.auth.model.{EZ_Account, EZ_Organization, EZ_Resource, EZ_Role}
import com.ecfront.ez.framework.service.rpc.foundation.Method
import com.ecfront.ez.framework.service.rpc.http.RespHttpClientProcessor

class OrganizationSpec extends MockStartupSpec {

  test("Organization test") {

    EZ_Organization.deleteByCode("org1")
    EZ_Resource.deleteByCode("GET@/org/1/foo/")
    EZ_Role.deleteByCode("org1@user1")
    EZ_Account.deleteByLoginId("u1", "org1")

    EZ_Organization.save(EZ_Organization("org1", "组织1"))
    EZ_Resource.save(EZ_Resource(Method.GET, "/org/1/foo/", s"Fetch org1 Info"))

    val role = EZ_Role("user1", "User 1", List(
      s"GET@/org/1/foo/",
      s"GET@/auth/manage/account/"
    ), "org1")
    EZ_Role.save(role)

    var account = EZ_Account("u1", "net@sunisle.org", "u1", "123", List(
      "org1@user1"
    ), "org1")
    EZ_Account.save(account)

    // login
    val loginResp = AuthService.doLogin("u1", "123", "org1")
    assert(loginResp
      && loginResp.body.token != ""
      && loginResp.body.login_id == "u1"
      && loginResp.body.organization_code == "org1"
    )
    val token = loginResp.body.token
    val loginInfoResp = CacheManager.getTokenInfo(token)
    assert(loginInfoResp
      && loginInfoResp.body.token != ""
      && loginInfoResp.body.login_id == "u1"
      && loginInfoResp.body.organization_code == "org1"
    )
    account = EZ_Account.getByLoginId(loginInfoResp.body.login_id, "org1").body
    assert(
      RespHttpClientProcessor.get[EZ_Account](s"http://127.0.0.1:8080/auth/manage/account/${account.id}/?__ez_token__=$token")
        .code == StandardCode.UNAUTHORIZED)
    assert(
      RespHttpClientProcessor.get[Void](s"http://127.0.0.1:8080/org/1/foo/?__ez_token__=$token")
        .code == StandardCode.NOT_IMPLEMENTED)

  }

}


