package com.ecfront.ez.framework.module.auth.manage

import com.ecfront.ez.framework.module.auth.{Account, Resource, Role}
import com.ecfront.ez.framework.service.IdModel


object Initiator {

  val SYSTEM_ACCOUNT_ID = "sysadmin"
  val SYSTEM_ROLE_ID = "system"

  def init(): Unit = {
    val res = Resource()
    res.id = s"GET${IdModel.SPLIT_FLAG}/auth/logininfo/"
    res.name = s"Fetch Login Info"
    ResourceService._save(res)
    res.id = s"GET${IdModel.SPLIT_FLAG}/auth/logout/"
    res.name = s"Logout"
    ResourceService._save(res)

    res.id = s"GET${IdModel.SPLIT_FLAG}/auth/manage/account/:id/"
    res.name = s"Fetch Account By Id"
    ResourceService._save(res)
    res.id = s"POST${IdModel.SPLIT_FLAG}/auth/manage/account/"
    res.name = s"Save a new Account"
    ResourceService._save(res)
    res.id = s"PUT${IdModel.SPLIT_FLAG}/auth/manage/account/:id/"
    res.name = s"Update a exist Account By Id"
    ResourceService._save(res)
    res.id = s"DELETE${IdModel.SPLIT_FLAG}/auth/manage/account/:id/"
    res.name = s"Delete a exist Account By Id"
    ResourceService._save(res)

    res.id = s"GET${IdModel.SPLIT_FLAG}/auth/manage/role/:id/"
    res.name = s"Fetch Role By Id"
    ResourceService._save(res)
    res.id = s"POST${IdModel.SPLIT_FLAG}/auth/manage/role/"
    res.name = s"Save a new Role"
    ResourceService._save(res)
    res.id = s"PUT${IdModel.SPLIT_FLAG}/auth/manage/role/:id/"
    res.name = s"Update a exist Role By Id"
    ResourceService._save(res)
    res.id = s"DELETE${IdModel.SPLIT_FLAG}/auth/manage/role/:id/"
    res.name = s"Delete a exist Role By Id"
    ResourceService._save(res)

    res.id = s"GET${IdModel.SPLIT_FLAG}/auth/manage/resource/:id/"
    res.name = s"Fetch Resource By Id"
    ResourceService._save(res)
    res.id = s"POST${IdModel.SPLIT_FLAG}/auth/manage/resource/"
    res.name = s"Save a new Resource"
    ResourceService._save(res)
    res.id = s"PUT${IdModel.SPLIT_FLAG}/auth/manage/resource/:id/"
    res.name = s"Update a exist Resource By Id"
    ResourceService._save(res)
    res.id = s"DELETE${IdModel.SPLIT_FLAG}/auth/manage/resource/:id/"
    res.name = s"Delete a exist Resource By Id"
    ResourceService._save(res)

    val role = Role()
    role.id = SYSTEM_ROLE_ID
    role.name = "System Role"
    role.resource_ids = Map(
      s"GET${IdModel.SPLIT_FLAG}/auth/logininfo/" -> null,
      s"GET${IdModel.SPLIT_FLAG}/auth/logout/" -> null,
      s"GET${IdModel.SPLIT_FLAG}/auth/manage/account/:id/" -> null,
      s"POST${IdModel.SPLIT_FLAG}/auth/manage/account/" -> null,
      s"PUT${IdModel.SPLIT_FLAG}/auth/manage/account/:id/" -> null,
      s"DELETE${IdModel.SPLIT_FLAG}/auth/manage/account/:id/" -> null,
      s"GET${IdModel.SPLIT_FLAG}/auth/manage/role/:id/" -> null,
      s"POST${IdModel.SPLIT_FLAG}/auth/manage/role/" -> null,
      s"PUT${IdModel.SPLIT_FLAG}/auth/manage/role/:id/" -> null,
      s"DELETE${IdModel.SPLIT_FLAG}/auth/manage/role/:id/" -> null,
      s"GET${IdModel.SPLIT_FLAG}/auth/manage/resource/:id/" -> null,
      s"POST${IdModel.SPLIT_FLAG}/auth/manage/resource/" -> null,
      s"PUT${IdModel.SPLIT_FLAG}/auth/manage/resource/:id/" -> null,
      s"DELETE${IdModel.SPLIT_FLAG}/auth/manage/resource/:id/" -> null
    )
    RoleService._save(role)

    val account = Account()
    account.id = SYSTEM_ACCOUNT_ID
    account.name = "System Administrator"
    account.password = "admin"
    account.role_ids = Map(
      SYSTEM_ROLE_ID -> null
    )
    AccountService._save(account)
  }

}
