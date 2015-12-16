package com.asto.ez.framework.auth.manage

import com.asto.ez.framework.auth.{EZ_Account, EZ_Organization, EZ_Resource, EZ_Role}
import com.asto.ez.framework.rpc.Method
import com.asto.ez.framework.storage.BaseModel
import com.typesafe.scalalogging.slf4j.LazyLogging

import scala.async.Async.{async, await}
import scala.concurrent.ExecutionContext.Implicits.global

object Initiator extends LazyLogging {

  def init() = async {

    val exist = await(EZ_Resource.model.existByCond(s"""{"code":"${BaseModel.SPLIT + Method.GET + BaseModel.SPLIT + "/auth/logininfo/"}"}""")).body
    if (!exist) {

      val org = EZ_Organization()
      org.code = ""
      org.name = "default"
      org.enable = true
      await(org.save())

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
      role.flag = EZ_Role.SYSTEM_ROLE_CODE
      role.name = "System Role"
      role.organization_code = ""
      role.enable = true
      role.resource_codes = List(
        s"GET${BaseModel.SPLIT}/auth/logininfo/",
        s"GET${BaseModel.SPLIT}/auth/logout/",
        s"GET${BaseModel.SPLIT}/auth/manage/account/:id/",
        s"POST${BaseModel.SPLIT}/auth/manage/account/",
        s"PUT${BaseModel.SPLIT}/auth/manage/account/:id/",
        s"DELETE${BaseModel.SPLIT}/auth/manage/account/:id/",
        s"GET${BaseModel.SPLIT}/auth/manage/role/:id/",
        s"POST${BaseModel.SPLIT}/auth/manage/role/",
        s"PUT${BaseModel.SPLIT}/auth/manage/role/:id/",
        s"DELETE${BaseModel.SPLIT}/auth/manage/role/:id/",
        s"GET${BaseModel.SPLIT}/auth/manage/resource/:id/",
        s"POST${BaseModel.SPLIT}/auth/manage/resource/",
        s"PUT${BaseModel.SPLIT}/auth/manage/resource/:id/",
        s"DELETE${BaseModel.SPLIT}/auth/manage/resource/:id/"
      )
      await(role.save())

      val account = EZ_Account()
      account.login_id = EZ_Account.SYSTEM_ACCOUNT_CODE
      account.name = "System Administrator"
      account.password = "admin"
      account.organization_code = ""
      account.enable = true
      account.role_codes = List(
        BaseModel.SPLIT + EZ_Role.SYSTEM_ROLE_CODE
      )
      await(account.save())
      logger.info("Initialized auth basic data.")
    }
  }

}
