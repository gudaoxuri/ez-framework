package com.ecfront.ez.framework.module.auth

import com.ecfront.ez.framework.BasicSpec
import com.ecfront.ez.framework.module.auth.manage.{AccountService, RoleService}
import com.ecfront.ez.framework.service.IdModel

class AuthManageSpec extends BasicSpec {

  test("age 测试") {
    AuthBasic.init()

    val newRole1 = RoleService._getById("admin").body
    assert(newRole1.id == "admin")
    assert(newRole1.name == "管理员")
    assert(newRole1.resource_ids == Map(AuthBasic.GET_SOME -> "获取资源", AuthBasic.ADD_SOME -> "保存资源"))

    val newRole2 = RoleService._getById("user").body
    assert(newRole2.id == "user")
    assert(newRole2.name == "普通用户")
    assert(newRole2.resource_ids == Map())


    val newAccount1 = AccountService._getById("user1").body
    assert(newAccount1.id == "user1")
    assert(newAccount1.name == "用户1")
    assert(newAccount1.password == AccountService.packageEncryptPwd("user1", "123"))
    assert(newAccount1.email == "123@ecfront.com")
    assert(newAccount1.role_ids == Map("admin" -> "管理员"))

    val newAccount2 = AccountService._getById("user2").body
    assert(newAccount2.id == "user2")
    assert(newAccount2.name == "用户2")
    assert(newAccount2.password == AccountService.packageEncryptPwd("user2", "123"))
    assert(newAccount2.email == "123@ecfront.com")
    assert(newAccount2.role_ids == Map())

  }

  test("local cache 测试") {
    AuthBasic.init()

    assert(LocalCacheContainer.resources == collection.mutable.HashSet(AuthBasic.GET_SOME, AuthBasic.ADD_SOME))
    assert(LocalCacheContainer.roles == collection.mutable.Map("admin" -> Set(AuthBasic.GET_SOME, AuthBasic.ADD_SOME), "user" -> Set()))

    assert(LocalCacheContainer.existResource(AuthBasic.GET_SOME))
    assert(LocalCacheContainer.existResource(AuthBasic.ADD_SOME))
    assert(!LocalCacheContainer.existResource(s"POST${IdModel.SPLIT_FLAG}/index/:id/1"))

    assert(LocalCacheContainer.matchInRoles(AuthBasic.GET_SOME, Set("admin", "user")))
    assert(LocalCacheContainer.matchInRoles(AuthBasic.GET_SOME, Set("admin")))
    assert(!LocalCacheContainer.matchInRoles(AuthBasic.GET_SOME, Set("user")))
    assert(!LocalCacheContainer.matchInRoles(AuthBasic.ADD_SOME, Set("user")))

  }

}




