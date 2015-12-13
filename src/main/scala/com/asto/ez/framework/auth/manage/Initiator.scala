package com.asto.ez.framework.auth.manage

import com.asto.ez.framework.auth.{EZ_Account, EZ_Resource, EZ_Role}
import com.asto.ez.framework.rpc.Method

import scala.async.Async.{async, await}
import scala.concurrent.ExecutionContext.Implicits.global

object Initiator {

  val SYSTEM_ACCOUNT_CODE = "sysadmin"
  val SYSTEM_ROLE_CODE = "system"

  def init() = async {
    val res = EZ_Resource()
    res.method = Method.GET
    res.uri = "/auth/logininfo/"
    res.name = s"Fetch Login Info"
    res.enable = true

    await(ResourceService.save(Map(), res, null))
    res.method = Method.GET
    res.uri = "/auth/logout/"
    res.name = s"Logout"
    await(ResourceService.save(Map(), res, null))

    res.method = Method.GET
    res.uri = "/auth/manage/account/:id/"
    res.name = s"Fetch Account By Id"
    await(ResourceService.save(Map(), res, null))

    res.method = Method.POST
    res.uri = "/auth/manage/account/"
    res.name = s"Save a new Account"
    await(ResourceService.save(Map(), res, null))

    res.method = Method.PUT
    res.uri = "/auth/manage/account/:id/"
    res.name = s"Update a exist Account By Id"
    await(ResourceService.save(Map(), res, null))

    res.method = Method.DELETE
    res.uri = "/auth/manage/account/:id/"
    res.name = s"Delete a exist Account By Id"
    await(ResourceService.save(Map(), res, null))

    res.method = Method.GET
    res.uri = "/auth/manage/role/:id/"
    res.name = s"Fetch Role By Id"
    await(ResourceService.save(Map(), res, null))

    res.method = Method.POST
    res.uri = "/auth/manage/role/"
    res.name = s"Save a new Role"
    await(ResourceService.save(Map(), res, null))

    res.method = Method.PUT
    res.uri = "/auth/manage/role/:id/"
    res.name = s"Update a exist Role By Id"
    await(ResourceService.save(Map(), res, null))

    res.method = Method.DELETE
    res.uri = "/auth/manage/role/:id/"
    res.name = s"Delete a exist Role By Id"
    await(ResourceService.save(Map(), res, null))

    res.method = Method.GET
    res.uri = "/auth/manage/resource/:id/"
    res.name = s"Fetch Resource By Id"
    await(ResourceService.save(Map(), res, null))

    res.method = Method.POST
    res.uri = "/auth/manage/resource/"
    res.name = s"Save a new Resource"
    await(ResourceService.save(Map(), res, null))

    res.method = Method.PUT
    res.uri = "/auth/manage/resource/:id/"
    res.name = s"Update a exist Resource By Id"
    await(ResourceService.save(Map(), res, null))

    res.method = Method.DELETE
    res.uri = "/auth/manage/resource/:id/"
    res.name = s"Delete a exist Resource By Id"
    await(ResourceService.save(Map(), res, null))

    val role = EZ_Role()
    role.code = SYSTEM_ROLE_CODE
    role.name = "System Role"
    role.enable = true
    role.resource_codes = List(
      s"GET${EZ_Resource.SPLIT}/auth/logininfo/",
      s"GET${EZ_Resource.SPLIT}/auth/logout/",
      s"GET${EZ_Resource.SPLIT}/auth/manage/account/:id/",
      s"POST${EZ_Resource.SPLIT}/auth/manage/account/",
      s"PUT${EZ_Resource.SPLIT}/auth/manage/account/:id/",
      s"DELETE${EZ_Resource.SPLIT}/auth/manage/account/:id/",
      s"GET${EZ_Resource.SPLIT}/auth/manage/role/:id/",
      s"POST${EZ_Resource.SPLIT}/auth/manage/role/",
      s"PUT${EZ_Resource.SPLIT}/auth/manage/role/:id/",
      s"DELETE${EZ_Resource.SPLIT}/auth/manage/role/:id/",
      s"GET${EZ_Resource.SPLIT}/auth/manage/resource/:id/",
      s"POST${EZ_Resource.SPLIT}/auth/manage/resource/",
      s"PUT${EZ_Resource.SPLIT}/auth/manage/resource/:id/",
      s"DELETE${EZ_Resource.SPLIT}/auth/manage/resource/:id/"
    )
    await(RoleService.save(Map(), role, null))

    val account = EZ_Account()
    account.login_id = SYSTEM_ACCOUNT_CODE
    account.name = "System Administrator"
    account.password = "admin"
    account.enable = true
    account.role_codes = List(
      SYSTEM_ROLE_CODE
    )
    await(AccountService.save(Map(), account, null))
  }

}
