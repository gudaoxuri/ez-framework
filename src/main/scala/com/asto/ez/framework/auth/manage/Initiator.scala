package com.asto.ez.framework.auth.manage

import com.asto.ez.framework.auth.{EZ_Account, EZ_Resource, EZ_Role}
import com.asto.ez.framework.rpc.Method
import com.typesafe.scalalogging.slf4j.LazyLogging

import scala.async.Async.{async, await}
import scala.concurrent.ExecutionContext.Implicits.global

object Initiator extends LazyLogging {

  def init() = async {

    val exist = EZ_Resource.model.existById("")
    val res = EZ_Resource()
    res.method = Method.GET
    res.uri = "/auth/logininfo/"
    res.name = s"Fetch Login Info"
    res.enable = true

    await(res.save())
    res.method = Method.GET
    res.uri = "/auth/logout/"
    res.name = s"Logout"
    await(res.save())

    res.method = Method.GET
    res.uri = "/auth/manage/account/:id/"
    res.name = s"Fetch Account By Id"
    await(res.save())

    res.method = Method.POST
    res.uri = "/auth/manage/account/"
    res.name = s"Save a new Account"
    await(res.save())

    res.method = Method.PUT
    res.uri = "/auth/manage/account/:id/"
    res.name = s"Update a exist Account By Id"
    await(res.save())

    res.method = Method.DELETE
    res.uri = "/auth/manage/account/:id/"
    res.name = s"Delete a exist Account By Id"
    await(res.save())

    res.method = Method.GET
    res.uri = "/auth/manage/role/:id/"
    res.name = s"Fetch Role By Id"
    await(res.save())

    res.method = Method.POST
    res.uri = "/auth/manage/role/"
    res.name = s"Save a new Role"
    await(res.save())

    res.method = Method.PUT
    res.uri = "/auth/manage/role/:id/"
    res.name = s"Update a exist Role By Id"
    await(res.save())

    res.method = Method.DELETE
    res.uri = "/auth/manage/role/:id/"
    res.name = s"Delete a exist Role By Id"
    await(res.save())

    res.method = Method.GET
    res.uri = "/auth/manage/resource/:id/"
    res.name = s"Fetch Resource By Id"
    await(res.save())

    res.method = Method.POST
    res.uri = "/auth/manage/resource/"
    res.name = s"Save a new Resource"
    await(res.save())

    res.method = Method.PUT
    res.uri = "/auth/manage/resource/:id/"
    res.name = s"Update a exist Resource By Id"
    await(res.save())

    res.method = Method.DELETE
    res.uri = "/auth/manage/resource/:id/"
    res.name = s"Delete a exist Resource By Id"
    await(res.save())

    val role = EZ_Role()
    role.code = EZ_Role.SYSTEM_ROLE_CODE
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
    await(role.save())

    val account = EZ_Account()
    account.login_id = EZ_Account.SYSTEM_ACCOUNT_CODE
    account.name = "System Administrator"
    account.password = "admin"
    account.enable = true
    account.role_codes = List(
      EZ_Role.SYSTEM_ROLE_CODE
    )
    await(account.save())

  }

}
