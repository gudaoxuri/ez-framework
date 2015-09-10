package com.ecfront.ez.framework.module.auth

import com.ecfront.ez.framework.module.auth.manage.{AccountService, ResourceService, RoleService}
import com.ecfront.ez.framework.storage.IdModel

object AuthBasic {

  val ADD_SOME = s"POST${IdModel.SPLIT_FLAG}/some/"
  val GET_SOME = s"GET${IdModel.SPLIT_FLAG}/some/:id/"

  def init(): Unit = {
    MockStartup

    val res = EZ_Resource()
    res.id = GET_SOME
    res.name = "获取资源"
    res.enable=true
    ResourceService._save(res)
    res.id = ADD_SOME
    res.name = "保存资源"
    res.enable=true
    ResourceService._save(res)

    val role1 = EZ_Role()
    role1.id = "admin"
    role1.name = "管理员"
    role1.enable=true
    RoleService._save(role1)
    role1.resource_ids = Map(GET_SOME -> null, ADD_SOME -> null)
    RoleService._update("admin", role1)

    val role2 = EZ_Role()
    role2.id = "user"
    role2.name = "普通用户"
    role2.enable=true
    role2.resource_ids = Map(GET_SOME -> null, ADD_SOME -> null)
    RoleService._save(role2)
    role2.resource_ids = Map()
    RoleService._update("user", role2)

    val account1 = EZ_Account()
    account1.id = "user1"
    account1.name = "用户1"
    account1.password = "123"
    account1.email = "123@ecfront.com"
    account1.enable=true
    account1.role_ids = Map("admin" -> null)
    AccountService._save(account1)

    val account2 = EZ_Account()
    account2.id = "user2"
    account2.name = "用户2"
    account2.password = "123"
    account2.email = "123@ecfront.com"
    account2.enable=true
    account2.role_ids = Map("admin" -> null, "user" -> null)
    AccountService._save(account2)
    val newAccount2 = AccountService._getById("user2").body
    assert(newAccount2.role_ids("admin").name == "管理员")
    assert(newAccount2.role_ids("user").name == "普通用户")
    newAccount2.role_ids = Map()
    newAccount2.password = "123"
    AccountService._update("user2", newAccount2)
  }

}




