package com.ecfront.ez.framework.module.auth

import com.ecfront.ez.framework.module.auth.manage.{AccountService, ResourceService, RoleService}
import com.ecfront.ez.framework.service.IdModel

object AuthBasic {

  val ADD_SOME = s"POST${IdModel.SPLIT_FLAG}/some/"
  val GET_SOME = s"GET${IdModel.SPLIT_FLAG}/some/:id/"

  def init(): Unit = {
    val res = Resource()
    res.id = GET_SOME
    res.name = "获取资源"
    ResourceService._save(res)
    res.id = ADD_SOME
    res.name = "保存资源"
    ResourceService._save(res)

    val role1 = Role()
    role1.id = "admin"
    role1.name = "管理员"
    RoleService._save(role1)
    role1.resource_ids = Map(GET_SOME -> null, ADD_SOME -> null)
    RoleService._update("admin", role1)

    val role2 = Role()
    role2.id = "user"
    role2.name = "普通用户"
    role2.resource_ids = Map(GET_SOME -> null, ADD_SOME -> null)
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
    assert(newAccount2.role_ids == Map("admin" -> "管理员", "user" -> "普通用户"))
    newAccount2.role_ids = Map()
    newAccount2.password = "123"
    AccountService._update("user2", newAccount2)
  }

}




