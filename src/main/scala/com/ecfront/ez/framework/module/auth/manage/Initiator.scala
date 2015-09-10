package com.ecfront.ez.framework.module.auth.manage

import com.ecfront.ez.framework.module.auth.{EZ_Account, EZ_Resource, EZ_Role}
import com.ecfront.ez.framework.storage.IdModel


object Initiator {

  val SYSTEM_ACCOUNT_ID = "sysadmin"
  val SYSTEM_ROLE_ID = "system"

  def init(): Unit = {
    val res = EZ_Resource()
    res.id = s"GET${IdModel.SPLIT_FLAG}/auth/logininfo/"
    res.name = s"Fetch Login Info"
    res.enable=true
    ResourceService._save(res)
    res.id = s"GET${IdModel.SPLIT_FLAG}/auth/logout/"
    res.name = s"Logout"
    res.enable=true
    ResourceService._save(res)

    res.id = s"GET${IdModel.SPLIT_FLAG}/auth/manage/account/:id/"
    res.name = s"Fetch Account By Id"
    res.enable=true
    ResourceService._save(res)
    res.id = s"POST${IdModel.SPLIT_FLAG}/auth/manage/account/"
    res.name = s"Save a new Account"
    res.enable=true
    ResourceService._save(res)
    res.id = s"PUT${IdModel.SPLIT_FLAG}/auth/manage/account/:id/"
    res.name = s"Update a exist Account By Id"
    res.enable=true
    ResourceService._save(res)
    res.id = s"DELETE${IdModel.SPLIT_FLAG}/auth/manage/account/:id/"
    res.name = s"Delete a exist Account By Id"
    res.enable=true
    ResourceService._save(res)

    res.id = s"GET${IdModel.SPLIT_FLAG}/auth/manage/role/:id/"
    res.name = s"Fetch Role By Id"
    res.enable=true
    ResourceService._save(res)
    res.id = s"POST${IdModel.SPLIT_FLAG}/auth/manage/role/"
    res.name = s"Save a new Role"
    res.enable=true
    ResourceService._save(res)
    res.id = s"PUT${IdModel.SPLIT_FLAG}/auth/manage/role/:id/"
    res.name = s"Update a exist Role By Id"
    res.enable=true
    ResourceService._save(res)
    res.id = s"DELETE${IdModel.SPLIT_FLAG}/auth/manage/role/:id/"
    res.name = s"Delete a exist Role By Id"
    res.enable=true
    ResourceService._save(res)

    res.id = s"GET${IdModel.SPLIT_FLAG}/auth/manage/resource/:id/"
    res.name = s"Fetch Resource By Id"
    res.enable=true
    ResourceService._save(res)
    res.id = s"POST${IdModel.SPLIT_FLAG}/auth/manage/resource/"
    res.name = s"Save a new Resource"
    res.enable=true
    ResourceService._save(res)
    res.id = s"PUT${IdModel.SPLIT_FLAG}/auth/manage/resource/:id/"
    res.name = s"Update a exist Resource By Id"
    res.enable=true
    ResourceService._save(res)
    res.id = s"DELETE${IdModel.SPLIT_FLAG}/auth/manage/resource/:id/"
    res.name = s"Delete a exist Resource By Id"
    res.enable=true
    ResourceService._save(res)

    val role = EZ_Role()
    role.id = SYSTEM_ROLE_ID
    role.name = "System Role"
    role.enable=true
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

    val account = EZ_Account()
    account.id = SYSTEM_ACCOUNT_ID
    account.name = "System Administrator"
    account.password = "admin"
    account.enable=true
    account.role_ids = Map(
      SYSTEM_ROLE_ID -> null
    )
    AccountService._save(account)
  }

}
