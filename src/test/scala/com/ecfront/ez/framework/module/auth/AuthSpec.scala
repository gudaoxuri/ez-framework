package com.ecfront.ez.framework.module.auth

import com.ecfront.ez.framework.BasicSpec
import com.ecfront.ez.framework.module.auth.manage.{AccountService, ResourceService, RoleService}
import com.ecfront.ez.framework.service.IdModel

class AuthSpec extends BasicSpec {

  test("Manage 测试") {
    val res = Resource()
    res.id = s"GET${IdModel.SPLIT_FLAG}/index/:id/"
    res.name = "获取资源1"
    ResourceService._save(res)
    res.id = s"POST${IdModel.SPLIT_FLAG}/index/:id/"
    res.name = "保存资源2"
    ResourceService._save(res)

    val role1 = Role()
    role1.id = "admin"
    role1.name = "系统管理员"
    RoleService._save(role1)
    role1.resource_ids = Map(s"GET${IdModel.SPLIT_FLAG}/index/:id/" -> null, s"POST${IdModel.SPLIT_FLAG}/index/:id/" -> null)
    RoleService._update("admin", role1)
    val newRole1 = RoleService._getById("admin").body
    assert(newRole1.id == "admin")
    assert(newRole1.name == "系统管理员")
    assert(newRole1.resource_ids == Map(s"GET${IdModel.SPLIT_FLAG}/index/:id/" -> "获取资源1", s"POST${IdModel.SPLIT_FLAG}/index/:id/" -> "保存资源2"))

    val role2 = Role()
    role2.id = "user"
    role2.name = "普通用户"
    role2.resource_ids = Map(s"GET${IdModel.SPLIT_FLAG}/index/:id/" -> null, s"POST${IdModel.SPLIT_FLAG}/index/:id/" -> null)
    RoleService._save(role2)
    role2.resource_ids = Map()
    RoleService._update("user", role2)
    val newRole2 = RoleService._getById("user").body
    assert(newRole2.id == "user")
    assert(newRole2.name == "普通用户")
    assert(newRole2.resource_ids == Map())

    val account1 = Account()
    account1.id = "user1"
    account1.name = "用户1"
    account1.password = "123"
    account1.email = "123@ecfront.com"
    account1.role_ids = Map("admin" -> null)
    AccountService._save(account1)
    val newAccount1 = AccountService._getById("user1").body
    assert(newAccount1.id == "user1")
    assert(newAccount1.name == "用户1")
    assert(newAccount1.password == AccountService.packageEncryptPwd("user1", "123"))
    assert(newAccount1.email == "123@ecfront.com")
    assert(newAccount1.role_ids == Map("admin" -> "系统管理员"))

    val account2 = Account()
    account2.id = "user2"
    account2.name = "用户2"
    account2.password = "123"
    account2.email = "123@ecfront.com"
    account2.role_ids = Map("admin" -> null, "user" -> null)
    AccountService._save(account2)
    var newAccount2 = AccountService._getById("user2").body
    assert(newAccount2.role_ids == Map("admin" -> "系统管理员", "user" -> "普通用户"))
    newAccount2.role_ids = Map()
    newAccount2.password = "123"
    AccountService._update("user2", newAccount2)
    newAccount2 = AccountService._getById("user2").body
    assert(newAccount2.id == "user2")
    assert(newAccount2.name == "用户2")
    assert(newAccount2.password == AccountService.packageEncryptPwd("user2", "123"))
    assert(newAccount2.email == "123@ecfront.com")
    assert(newAccount2.role_ids == Map())

  }
}




