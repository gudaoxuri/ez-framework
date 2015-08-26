package com.ecfront.ez.framework.module.auth

import com.ecfront.common.{JsonHelper, Resp, StandardCode}
import com.ecfront.ez.framework.BasicSpec
import com.ecfront.ez.framework.helper.HttpHelper
import com.ecfront.ez.framework.module.auth.manage.{AccountService, ResourceService, RoleService}
import com.ecfront.ez.framework.service.IdModel

class AuthSpec extends BasicSpec {

  val ADD_ACCOUNT = s"POST${IdModel.SPLIT_FLAG}/auth/manage/account/"
  val GET_ACCOUNT = s"GET${IdModel.SPLIT_FLAG}/auth/manage/account/:id/"

  def init(): Unit = {
    val res = Resource()
    res.id = GET_ACCOUNT
    res.name = "获取资源1"
    ResourceService._save(res)
    res.id = s"ADD_ACCOUNT"
    res.name = "保存资源2"
    ResourceService._save(res)

    val role1 = Role()
    role1.id = "admin"
    role1.name = "系统管理员"
    RoleService._save(role1)
    role1.resource_ids = Map(GET_ACCOUNT -> null, ADD_ACCOUNT -> null)
    RoleService._update("admin", role1)


    val role2 = Role()
    role2.id = "user"
    role2.name = "普通用户"
    role2.resource_ids = Map(GET_ACCOUNT -> null, ADD_ACCOUNT -> null)
    RoleService._save(role2)
    role2.resource_ids = Map()
    RoleService._update("user", role2)

    val account1 = Account()
    account1.id = "user1"
    account1.name = "用户1"
    account1.password = "123"
    account1.email = "123@ecfront.com"
    account1.role_ids = Map("admin" -> null)
    AccountService._save(account1)

    val account2 = Account()
    account2.id = "user2"
    account2.name = "用户2"
    account2.password = "123"
    account2.email = "123@ecfront.com"
    account2.role_ids = Map("admin" -> null, "user" -> null)
    AccountService._save(account2)
    val newAccount2 = AccountService._getById("user2").body
    assert(newAccount2.role_ids == Map("admin" -> "系统管理员", "user" -> "普通用户"))
    newAccount2.role_ids = Map()
    newAccount2.password = "123"
    AccountService._update("user2", newAccount2)
  }

  test("Manage 测试") {
    init()

    val newRole1 = RoleService._getById("admin").body
    assert(newRole1.id == "admin")
    assert(newRole1.name == "系统管理员")
    assert(newRole1.resource_ids == Map(GET_ACCOUNT -> "获取资源1", ADD_ACCOUNT -> "保存资源2"))

    val newRole2 = RoleService._getById("user").body
    assert(newRole2.id == "user")
    assert(newRole2.name == "普通用户")
    assert(newRole2.resource_ids == Map())


    val newAccount1 = AccountService._getById("user1").body
    assert(newAccount1.id == "user1")
    assert(newAccount1.name == "用户1")
    assert(newAccount1.password == AccountService.packageEncryptPwd("user1", "123"))
    assert(newAccount1.email == "123@ecfront.com")
    assert(newAccount1.role_ids == Map("admin" -> "系统管理员"))

    val newAccount2 = AccountService._getById("user2").body
    assert(newAccount2.id == "user2")
    assert(newAccount2.name == "用户2")
    assert(newAccount2.password == AccountService.packageEncryptPwd("user2", "123"))
    assert(newAccount2.email == "123@ecfront.com")
    assert(newAccount2.role_ids == Map())

  }

  test("local cache 测试") {
    init()

    assert(LocalCacheContainer.resources == collection.mutable.HashSet(GET_ACCOUNT, ADD_ACCOUNT))
    assert(LocalCacheContainer.roles == collection.mutable.Map("admin" -> Set(GET_ACCOUNT, ADD_ACCOUNT), "user" -> Set()))

    assert(LocalCacheContainer.existResource(GET_ACCOUNT))
    assert(LocalCacheContainer.existResource(ADD_ACCOUNT))
    assert(!LocalCacheContainer.existResource(s"POST${IdModel.SPLIT_FLAG}/index/:id/1"))

    assert(LocalCacheContainer.matchInRoles(GET_ACCOUNT, Set("admin", "user")))
    assert(LocalCacheContainer.matchInRoles(GET_ACCOUNT, Set("admin")))
    assert(!LocalCacheContainer.matchInRoles(GET_ACCOUNT, Set("user")))
    assert(!LocalCacheContainer.matchInRoles(ADD_ACCOUNT, Set("user")))

  }

  test("auth 测试") {

    MockStartup

    val result = JsonHelper.toGenericObject[Resp[TokenInfo]](HttpHelper.post(s"http://127.0.0.1:8080/public/auth/login/",Map("loginId" -> "user1","password" -> "123")).body)
    if (result) {
      val token = result.body.id
      assert(token != "")
      println("Token:" + token)
      val loginInfo = JsonHelper.toGenericObject[Resp[TokenInfo]](HttpHelper.get(s"http://127.0.0.1:8080/auth/manage/logininfo/?ez_token=$token").body).body
      assert(loginInfo.id == "user1")
      val account = Account()
      account.id = "testUser"
      account.name = "测试用户"
      account.password = "456"
      account.role_ids = Map("user" -> null)
      JsonHelper.toGenericObject[Resp[TokenInfo]](HttpHelper.post(s"http://127.0.0.1:8080/auth/manage/account/?ez_token=$token",account).body)
      val getAccount = JsonHelper.toGenericObject[Resp[Account]](HttpHelper.get(s"http://127.0.0.1:8080/auth/manage/account/testUser/?ez_token=$token").body).body
      assert(getAccount.id == "testUser")
      assert(getAccount.name == "测试用户")
      assert(getAccount.password == AccountService.packageEncryptPwd("testUser", "456"))
      assert(getAccount.role_ids == Map("user" -> "普通用户"))
      JsonHelper.toGenericObject[Resp[Void]](HttpHelper.get(s"http://127.0.0.1:8080/auth/manage/logout/?ez_token=$token").body)
      val getAccountWrap = JsonHelper.toGenericObject[Resp[Account]](HttpHelper.get(s"http://127.0.0.1:8080/auth/manage/account/testUser/?ez_token=$token").body)
      assert(getAccountWrap.code == StandardCode.UNAUTHORIZED)

    }

  }


}




