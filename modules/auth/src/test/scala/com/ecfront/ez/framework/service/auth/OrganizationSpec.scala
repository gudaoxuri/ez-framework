package com.ecfront.ez.framework.service.auth

import com.ecfront.common.StandardCode
import com.ecfront.ez.framework.core.test.MockStartupSpec
import com.ecfront.ez.framework.service.auth.model.{EZ_Account, EZ_Organization, EZ_Resource, EZ_Role}
import com.ecfront.ez.framework.service.rpc.foundation.Method
import com.ecfront.ez.framework.service.rpc.http.RespHttpClientProcessor
import com.ecfront.ez.framework.service.storage.foundation.Page

class OrganizationSpec extends MockStartupSpec {

  test("Organization test") {

    EZ_Organization.deleteByCode("org1")
    EZ_Organization.deleteByCode("org2")
    EZ_Resource.deleteByCode("GET@/org/1/foo/")
    EZ_Role.deleteByCode("org1@user1")
    EZ_Account.deleteByLoginId("u1", "org1")
    EZ_Account.deleteByLoginId("u2", "org2")

    EZ_Resource.save(EZ_Resource(Method.GET, "/org/1/foo/", s"Fetch org1 Info"))

    var token=AuthService.doLogin("sysadmin", "admin", "",new EZAuthContext).body.token

    // 添加一个组织
    var org=RespHttpClientProcessor.post[EZ_Organization](
      s"http://127.0.0.1:8080/auth/manage/organization/?__ez_token__=$token",Map(
        "code" -> "org1",
        "name" -> "组织11"
      )).body
    // 修改组织名
    RespHttpClientProcessor.put[EZ_Organization](
      s"http://127.0.0.1:8080/auth/manage/organization/${org.id}/?__ez_token__=$token",Map(
        "name" -> "组织1"
      ))
    // page
    val orgs=RespHttpClientProcessor.get[Page[EZ_Organization]](s"http://127.0.0.1:8080/auth/manage/organization/page/1/10/?__ez_token__=$token").body
    assert(orgs.recordTotal==2&&orgs.objects.last.name=="组织1"&&orgs.objects.last.enable)

    var role = EZ_Role("user1", "User 1", List(
      s"GET@/org/1/foo/",
      s"GET@/auth/manage/account/"
    ), "org1")
    EZ_Role.save(role)
    var account = EZ_Account("u1", "net@sunisle.org", "u1", "123", List(
      "org1@user1"
    ), "org1")
    EZ_Account.save(account)

    // login
    var loginResp = AuthService.doLogin("u1", "123", "org1",new EZAuthContext)
    assert(loginResp
      && loginResp.body.token != ""
      && loginResp.body.login_id == "u1"
      && loginResp.body.organization_code == "org1"
    )
    token = loginResp.body.token
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

    token= AuthService.doLogin("sysadmin", "admin", "",new EZAuthContext).body.token
    // disable
    RespHttpClientProcessor.get[Void](
      s"http://127.0.0.1:8080/auth/manage/organization/${org.id}/disable/?__ez_token__=$token")
    // get
    org=RespHttpClientProcessor.get[EZ_Organization](
      s"http://127.0.0.1:8080/auth/manage/organization/${org.id}/?__ez_token__=$token").body
    assert(!org.enable)
    loginResp = AuthService.doLogin("u1", "123", "org1",new EZAuthContext)
    assert(loginResp.code==StandardCode.LOCKED)

    // enable
    RespHttpClientProcessor.get[Void](
      s"http://127.0.0.1:8080/auth/manage/organization/${org.id}/enable/?__ez_token__=$token")
    // get
    org=RespHttpClientProcessor.get[EZ_Organization](
      s"http://127.0.0.1:8080/auth/manage/organization/${org.id}/?__ez_token__=$token").body
    assert(org.enable)
    loginResp = AuthService.doLogin("u1", "123", "org1",new EZAuthContext)
    assert(loginResp)

    // 再添加一个组织
    org=RespHttpClientProcessor.post[EZ_Organization](
      s"http://127.0.0.1:8080/auth/manage/organization/?__ez_token__=$token",Map(
        "code" -> "org2",
        "name" -> "组织2"
      )).body
    account = EZ_Account("u2", "net@sunisle.org", "u2", "123", List(
      "org2@"+EZ_Role.ORG_ADMIN_ROLE_FLAG
    ), "org2")
    EZ_Account.save(account)

    // 使用org2的用户登录
    assert(AuthService.doLogin("u2", "123", "org1",new EZAuthContext).code==StandardCode.NOT_FOUND)
    token=AuthService.doLogin("u2", "123", "org2",new EZAuthContext).body.token
    var accounts=RespHttpClientProcessor.get[Page[EZ_Account]](
      s"http://127.0.0.1:8080/auth/manage/account/page/1/10/?__ez_token__=$token").body
    assert(accounts.recordTotal==1&&accounts.objects.head.login_id=="u2")

    var roles=RespHttpClientProcessor.get[Page[EZ_Role]](
      s"http://127.0.0.1:8080/auth/manage/role/page/1/10/?__ez_token__=$token").body
    assert(roles.recordTotal==2&&roles.objects.head.flag=="org_admin")

  }

}


