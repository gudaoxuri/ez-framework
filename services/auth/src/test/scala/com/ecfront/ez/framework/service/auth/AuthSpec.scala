package com.ecfront.ez.framework.service.auth

import java.util.concurrent.CountDownLatch

import com.ecfront.common.StandardCode
import com.ecfront.ez.framework.core.EZ
import com.ecfront.ez.framework.core.rpc.{Method, OptInfo, RespHttpClientProcessor}
import com.ecfront.ez.framework.service.auth.model._
import com.ecfront.ez.framework.service.jdbc.{BaseModel, Page}
import com.ecfront.ez.framework.test.{GatewayStartupSpec, MockStartupSpec}
import io.vertx.core.http.HttpClientResponse
import io.vertx.core.{Handler, Vertx}

class AuthSpec extends GatewayStartupSpec {

  var token: String = ""

  def u(path: String): String = s"http://127.0.0.1:8080/ez/auth/$path?__ez_token__=$token"

  test("auth test") {
    basicTest()
    rpcTest()
    organizationTest()
    menuTest()
    performanceTest()
  }

  def basicTest(): Unit = {
    val resources = EZ_Resource.find("").body
    assert(
      resources.size == 9
        && resources.head.method == "*"
        && resources.head.uri == "/ez/auth/manage/*"
        && resources.head.code == "*" + BaseModel.SPLIT + "/ez/auth/manage/*"
    )
    val roles = EZ_Role.find("").body
    assert(
      roles.size == 2
        && roles.head.code == BaseModel.SPLIT + EZ_Role.SYSTEM_ROLE_FLAG
        && roles.head.flag == EZ_Role.SYSTEM_ROLE_FLAG
        && roles.head.name == "System"
        && roles.head.resource_codes.size == 1
        && roles.head.resource_codes.head == "*" + BaseModel.SPLIT + "/ez/auth/manage/*"
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
    assert(!AuthService.doLogin(EZ_Account.SYSTEM_ACCOUNT_LOGIN_ID, "errorpwd", "", ""))
    val loginResp = AuthService.doLogin(EZ_Account.SYSTEM_ACCOUNT_LOGIN_ID, "admin", "", "")
    assert(loginResp
      && loginResp.body.token != ""
      && loginResp.body.loginId == EZ_Account.SYSTEM_ACCOUNT_LOGIN_ID
      && loginResp.body.name == "Sys Admin"
      && loginResp.body.organizationCode == ""
      && loginResp.body.roleCodes == Set(
      BaseModel.SPLIT + EZ_Role.SYSTEM_ROLE_FLAG
    )
    )
    val token = loginResp.body.token
    // get login info
    val loginInfo = CacheManager.Token.getTokenInfo(token)
    assert(loginInfo.token != ""
      && loginInfo.loginId == EZ_Account.SYSTEM_ACCOUNT_LOGIN_ID
      && loginInfo.organizationCode == ""
      && loginInfo.roleCodes == Set(
      BaseModel.SPLIT + EZ_Role.SYSTEM_ROLE_FLAG
    )
    )
    // logout
    assert(AuthService.doLogout(token))
    // get login info
    assert(CacheManager.Token.getTokenInfo(token) == null)

  }

  def rpcTest(): Unit = {
    // login & logout
    token = "1111"
    assert(
      RespHttpClientProcessor.get[Void](u("logout/")).code == StandardCode.UNAUTHORIZED)
    var loginInfoResp =
      RespHttpClientProcessor.post[OptInfo]("http://127.0.0.1:8080/public/ez/auth/login/", Map("id" -> "admin1", "password" -> "admin"))
    assert(!loginInfoResp)
    loginInfoResp =
      RespHttpClientProcessor.post[OptInfo]("http://127.0.0.1:8080/public/ez/auth/login/", Map("id" -> "sysadmin", "password" -> "admin"))
    assert(loginInfoResp && loginInfoResp.body.name == "Sys Admin")
    token = loginInfoResp.body.token
    assert(RespHttpClientProcessor.get[Void](u(s"logout/")))
    loginInfoResp =
      RespHttpClientProcessor.post[OptInfo]("http://127.0.0.1:8080/public/ez/auth/login/", Map("id" -> "sysadmin", "password" -> "admin"))
    assert(loginInfoResp.body.token != token)
    token = loginInfoResp.body.token
    // getByLoginId
    var account = EZ_Account.getByLoginId(loginInfoResp.body.loginId, "").body
    account =
      RespHttpClientProcessor.get[EZ_Account](u(s"manage/account/${account.id}/")).body
    assert(account.login_id == "sysadmin")

    // 改名
    RespHttpClientProcessor.put[EZ_Account](u(s"manage/account/${account.id}/"), Map("name" -> "system"))
    // 改登录名及密码
    RespHttpClientProcessor.put[EZ_Account](u(s"manage/account/uuid/${account.code}/"), Map("password" -> "456", "login_id" -> "sysadmin1"))
    // 改密码，已修改过账号，需要重新登录
    assert(RespHttpClientProcessor.put[EZ_Account](u(s"manage/account/uuid/${account.code}/"),
      Map("password" -> "456", "ext_info" -> "aaaaa")).code == StandardCode.UNAUTHORIZED)
    // 重新登录
    token = RespHttpClientProcessor.post[OptInfo]("http://127.0.0.1:8080/public/ez/auth/login/", Map("id" -> "sysadmin1", "password" -> "456")).body.token
    RespHttpClientProcessor.put[EZ_Account](u(s"manage/account/uuid/${account.code}/"), Map("password" -> "123", "ext_info" -> "aaaaa")).body
    // 改密成功
    loginInfoResp =
      RespHttpClientProcessor.post[OptInfo]("http://127.0.0.1:8080/public/ez/auth/login/", Map("id" -> "sysadmin1", "password" -> "123"))
    assert(loginInfoResp)
    assert(loginInfoResp.body.name == "system")

    token = loginInfoResp.body.token
    // 改回去
    RespHttpClientProcessor.put[EZ_Account](u(s"manage/account/uuid/${account.code}/"),
      Map("login_id" -> "sysadmin", "name" -> "Sys Admin", "password" -> "admin")).body
    loginInfoResp =
      RespHttpClientProcessor.post[OptInfo]("http://127.0.0.1:8080/public/ez/auth/login/", Map("id" -> "sysadmin", "password" -> "admin"))
    assert(loginInfoResp.body.name == "Sys Admin" && loginInfoResp.body.extInfo == "aaaaa")
    token = loginInfoResp.body.token
    RespHttpClientProcessor.get[Void](u(s"manage/account/${account.id}/disable/"))
    assert(!EZ_Account.getByLoginId("sysadmin", "").body.enable)
    // 禁用token被清空，无法再次启用
    RespHttpClientProcessor.get[Void](u(s"manage/account/${account.id}/enable/"))
    assert(!EZ_Account.getByLoginId("sysadmin", "").body.enable)
    EZ_Account.enableByLoginId("sysadmin", "")
  }

  def organizationTest(): Unit = {
    val c = new CountDownLatch(1)
    EZ.eb.subscribe(ServiceAdapter.EB_ORG_INIT_FLAG, classOf[EZ_Organization]) {
      (org, _) =>
        println(org.code)
        c.countDown()
    }
    EZ_Organization.deleteByCode("org1")
    EZ_Organization.deleteByCode("org2")
    EZ_Resource.deleteByCode("GET@/org/1/foo/")
    EZ_Role.deleteByCode("org1@org_admin")
    EZ_Role.deleteByCode("org2@org_admin")
    EZ_Role.deleteByCode("org1@user")
    EZ_Role.deleteByCode("org2@user")
    EZ_Role.deleteByCode("org1@user1")
    EZ_Account.deleteByLoginId("u1", "org1")
    EZ_Account.deleteByLoginId("u2", "org2")
    EZ_Account.deleteByLoginId("admin", "org1")
    EZ_Account.deleteByLoginId("admin", "org2")

    EZ_Resource.save(EZ_Resource(Method.GET.toString, "/org/1/foo/", s"Fetch org1 Info"))

    token = AuthService.doLogin("sysadmin", "admin", "", "").body.token

    // 添加一个组织
    var org = RespHttpClientProcessor.post[EZ_Organization](u("manage/organization/"), Map(
      "code" -> "org1",
      "name" -> "组织11"
    )).body
    // 修改组织名
    RespHttpClientProcessor.put[EZ_Organization](u(s"manage/organization/${org.id}/"), Map(
      "name" -> "组织1"
    ))
    // page
    val orgs = RespHttpClientProcessor.get[Page[EZ_Organization]](u(s"manage/organization/page/1/10/")).body
    assert(orgs.recordTotal == 2 && orgs.objects.last.name == "组织1" && orgs.objects.last.enable)

    var role = EZ_Role("user1", "User 1", Set(
      s"GET@/org/1/foo/",
      s"*@/ez/auth/manage/account/*"
    ), "org1")
    EZ_Role.save(role)
    var account1 = EZ_Account("u1", "net@sunisle.org", "u1", "123", Set("org1@user1"), "org1")
    EZ_Account.save(account1)

    // login
    var loginResp = AuthService.doLogin("u1", "123", "org1", "")
    assert(loginResp
      && loginResp.body.token != ""
      && loginResp.body.loginId == "u1"
      && loginResp.body.organizationCode == "org1"
    )
    token = loginResp.body.token
    val loginInfo = CacheManager.Token.getTokenInfo(token)
    assert(loginInfo.token != ""
      && loginInfo.loginId == "u1"
      && loginInfo.organizationCode == "org1"
    )
    account1 = EZ_Account.getByLoginId(loginInfo.loginId, "org1").body
    assert(RespHttpClientProcessor.get[EZ_Role](u(s"manage/role/")).code == StandardCode.UNAUTHORIZED)
    assert(RespHttpClientProcessor.get[EZ_Account](u(s"manage/account/${account1.id}/")))
    assert(RespHttpClientProcessor.get[EZ_Account](u(s"manage/account/uuid/${account1.code}/")))
    assert(
      RespHttpClientProcessor.get[Void](s"http://127.0.0.1:8080/org/1/foo/?__ez_token__=$token")
        .code == StandardCode.NOT_IMPLEMENTED)

    token = AuthService.doLogin("sysadmin", "admin", "", "").body.token
    // disable
    RespHttpClientProcessor.get[Void](u(s"manage/organization/${org.id}/disable/"))
    // get
    org = RespHttpClientProcessor.get[EZ_Organization](u(s"manage/organization/${org.id}/")).body
    assert(!org.enable)
    loginResp = AuthService.doLogin("u1", "123", "org1", "")
    assert(loginResp.code == StandardCode.LOCKED)
    // enable
    RespHttpClientProcessor.get[Void](u(s"manage/organization/${org.id}/enable/"))
    // get
    org = RespHttpClientProcessor.get[EZ_Organization](u(s"manage/organization/${org.id}/")).body
    assert(org.enable)
    loginResp = AuthService.doLogin("u1", "123", "org1", "")
    assert(loginResp)

    // 再添加一个组织
    org = RespHttpClientProcessor.post[EZ_Organization](
      u(s"manage/organization/"), Map(
        "code" -> "org2",
        "name" -> "组织2"
      )).body
    var account2 = EZ_Account("u2", "net@sunisle.org", "u2", "123", Set("org2@user"), "org2")
    account2 = EZ_Account.save(account2).body

    // org2没有u1用户
    assert(AuthService.doLogin("u2", "123", "org1", "").code == StandardCode.NOT_FOUND)
    // 使用org2的管理员登录
    token = AuthService.doLogin("admin", "admin", "org2", "").body.token
    // 查看org2组织的账号列表
    var accounts = RespHttpClientProcessor.get[Page[EZ_Account]](u(s"manage/account/page/1/10/")).body
    assert(accounts.recordTotal == 2 && accounts.objects.head.login_id == "admin")
    // 查看org2组织的角色列表
    var roles = RespHttpClientProcessor.get[Page[EZ_Role]](u(s"manage/role/page/1/10/")).body
    assert(roles.recordTotal == 1 && roles.objects.head.flag == "org_admin")
    // 尝试用org2管理员编辑org1用户
    assert(RespHttpClientProcessor.put[EZ_Account](u(s"manage/account/${account1.id}/"), Map(
      "name" -> "u1_error"
    )).code == StandardCode.NOT_FOUND)
    // 尝试用org2管理员编辑自己组织下的用户
    assert(RespHttpClientProcessor.put[EZ_Account](u(s"manage/account/${account2.id}/"), Map(
      "name" -> "u2_new"
    )).body.name == "u2_new")
    c.await()
  }

  def menuTest(): Unit = {
    EZ_Menu.deleteByCond("")
    EZ_Menu.save(EZ_Menu("pub.a", "P_A", "", List()))
    EZ_Menu.save(EZ_Menu("pub.a.a", "P_A_A", "@pub.a", List()))
    EZ_Menu.save(EZ_Menu("pub.a.b", "P_A_B", "@pub.a", List()))
    EZ_Menu.save(EZ_Menu("sys.a", "S_A", "", List("@system")))
    EZ_Menu.save(EZ_Menu("sys.b", "s_B", "", List("@system", "@user")))
    val pubMenus = AuthService.doGetMenus(Set(), "").body
    assert(pubMenus.length == 3)
    val roleCodes = AuthService.doLogin("sysadmin", "admin", "", "").body.roleCodes
    val authMenus = AuthService.doGetMenus(roleCodes, "").body
    assert(authMenus.length == 5)
  }

  def performanceTest(): Unit = {
    val c = new CountDownLatch(200)
    for (i <- 0 until 100) {
      Vertx.vertx.createHttpClient().get(8080, "127.0.0.1", "/public/test/wait/?id=" + i, new Handler[HttpClientResponse] {
        override def handle(event: HttpClientResponse): Unit = {
          logger.info("wait success")
          c.countDown()
        }
      }).end()
    }
    for (i <- 0 until 100) {
      Vertx.vertx.createHttpClient().get(8080, "127.0.0.1", "/public/test/immediately/", new Handler[HttpClientResponse] {
        override def handle(event: HttpClientResponse): Unit = {
          logger.info("immediately success")
          c.countDown()
        }
      }).end()
    }

    c.await()

  }
}


