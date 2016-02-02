package com.asto.ez.framework.auth.manage

import com.asto.ez.framework.auth.{EZ_Account, EZ_Organization, EZ_Resource, EZ_Role}
import com.asto.ez.framework.rpc.Method
import com.asto.ez.framework.storage.BaseModel
import com.typesafe.scalalogging.slf4j.LazyLogging

import scala.async.Async.{async, await}
import scala.concurrent.ExecutionContext.Implicits.global

object Initiator extends LazyLogging {

  def init() = async {

    val exist = await(EZ_Resource.existByCond(s"""{"code":"${Method.GET + BaseModel.SPLIT + "/auth/manage/organization/"}"}""")).body
    if (!exist) {

      val org = EZ_Organization("", "default")
      await(EZ_Organization.save(org))

      await(EZ_Resource.save(EZ_Resource(Method.GET, "/auth/manage/organization/", s"Find Organizations")))
      await(EZ_Resource.save(EZ_Resource(Method.GET, "/auth/manage/organization/page/:pageNumber/:pageSize/", s"Paging Organizations")))
      await(EZ_Resource.save(EZ_Resource(Method.GET, "/auth/manage/organization/:id/", s"Fetch Organization By Id")))
      await(EZ_Resource.save(EZ_Resource(Method.POST, "/auth/manage/organization/", s"Save a new Organization")))
      await(EZ_Resource.save(EZ_Resource(Method.PUT, "/auth/manage/organization/:id/", s"Update a exist Organization By Id")))
      await(EZ_Resource.save(EZ_Resource(Method.DELETE, "/auth/manage/organization/:id/", s"Delete a exist Organization By Id")))
      await(EZ_Resource.save(EZ_Resource(Method.GET, "/auth/manage/organization/:id/enable/", s"Enabled a exist Organization By Id")))
      await(EZ_Resource.save(EZ_Resource(Method.GET, "/auth/manage/organization/:id/disable/", s"Disabled a exist Organization By Id")))

      await(EZ_Resource.save(EZ_Resource(Method.GET, "/auth/manage/account/", s"Find Accounts")))
      await(EZ_Resource.save(EZ_Resource(Method.GET, "/auth/manage/account/page/:pageNumber/:pageSize/", s"Paging Accounts")))
      await(EZ_Resource.save(EZ_Resource(Method.GET, "/auth/manage/account/:id/", s"Fetch Account By Id")))
      await(EZ_Resource.save(EZ_Resource(Method.POST, "/auth/manage/account/", s"Save a new Account")))
      await(EZ_Resource.save(EZ_Resource(Method.PUT, "/auth/manage/account/:id/", s"Update a exist Account By Id")))
      await(EZ_Resource.save(EZ_Resource(Method.DELETE, "/auth/manage/account/:id/", s"Delete a exist Account By Id")))
      await(EZ_Resource.save(EZ_Resource(Method.GET, "/auth/manage/account/:id/enable/", s"Enabled a exist Account By Id")))
      await(EZ_Resource.save(EZ_Resource(Method.GET, "/auth/manage/account/:id/disable/", s"Disabled a exist Account By Id")))

      await(EZ_Resource.save(EZ_Resource(Method.GET, "/auth/manage/role/", s"Find Roles")))
      await(EZ_Resource.save(EZ_Resource(Method.GET, "/auth/manage/role/page/:pageNumber/:pageSize/", s"Paging Roles")))
      await(EZ_Resource.save(EZ_Resource(Method.GET, "/auth/manage/role/:id/", s"Fetch Role By Id")))
      await(EZ_Resource.save(EZ_Resource(Method.POST, "/auth/manage/role/", s"Save a new Role")))
      await(EZ_Resource.save(EZ_Resource(Method.PUT, "/auth/manage/role/:id/", s"Update a exist Role By Id")))
      await(EZ_Resource.save(EZ_Resource(Method.DELETE, "/auth/manage/role/:id/", s"Delete a exist Role By Id")))
      await(EZ_Resource.save(EZ_Resource(Method.GET, "/auth/manage/role/:id/enable/", s"Enabled a exist Role By Id")))
      await(EZ_Resource.save(EZ_Resource(Method.GET, "/auth/manage/role/:id/disable/", s"Disabled a exist Role By Id")))

      await(EZ_Resource.save(EZ_Resource(Method.GET, "/auth/manage/resource/", s"Find Resources")))
      await(EZ_Resource.save(EZ_Resource(Method.GET, "/auth/manage/resource/page/:pageNumber/:pageSize/", s"Paging Resources")))
      await(EZ_Resource.save(EZ_Resource(Method.GET, "/auth/manage/resource/:id/", s"Fetch Resource By Id")))
      await(EZ_Resource.save(EZ_Resource(Method.POST, "/auth/manage/resource/", s"Save a new Resource")))
      await(EZ_Resource.save(EZ_Resource(Method.PUT, "/auth/manage/resource/:id/", s"Update a exist Resource By Id")))
      await(EZ_Resource.save(EZ_Resource(Method.DELETE, "/auth/manage/resource/:id/", s"Delete a exist Resource By Id")))
      await(EZ_Resource.save(EZ_Resource(Method.GET, "/auth/manage/resource/:id/enable/", s"Enabled a exist Resource By Id")))
      await(EZ_Resource.save(EZ_Resource(Method.GET, "/auth/manage/resource/:id/disable/", s"Disabled a exist Resource By Id")))

      val role = EZ_Role(EZ_Role.SYSTEM_ROLE_CODE, "System Role", List(
        s"${Method.GET}${BaseModel.SPLIT}/auth/manage/organization/",
        s"${Method.GET}${BaseModel.SPLIT}/auth/manage/organization/page/:pageNumber/:pageSize/",
        s"${Method.GET}${BaseModel.SPLIT}/auth/manage/organization/:id/",
        s"${Method.POST}${BaseModel.SPLIT}/auth/manage/organization/",
        s"${Method.PUT}${BaseModel.SPLIT}/auth/manage/organization/:id/",
        s"${Method.DELETE}${BaseModel.SPLIT}/auth/manage/organization/:id/",
        s"${Method.GET}${BaseModel.SPLIT}/auth/manage/organization/:id/enable/",
        s"${Method.GET}${BaseModel.SPLIT}/auth/manage/organization/:id/disable/",
        s"${Method.GET}${BaseModel.SPLIT}/auth/manage/account/",
        s"${Method.GET}${BaseModel.SPLIT}/auth/manage/account/page/:pageNumber/:pageSize/",
        s"${Method.GET}${BaseModel.SPLIT}/auth/manage/account/:id/",
        s"${Method.POST}${BaseModel.SPLIT}/auth/manage/account/",
        s"${Method.PUT}${BaseModel.SPLIT}/auth/manage/account/:id/",
        s"${Method.DELETE}${BaseModel.SPLIT}/auth/manage/account/:id/",
        s"${Method.GET}${BaseModel.SPLIT}/auth/manage/account/:id/enable/",
        s"${Method.GET}${BaseModel.SPLIT}/auth/manage/account/:id/disable/",
        s"${Method.GET}${BaseModel.SPLIT}/auth/manage/role/",
        s"${Method.GET}${BaseModel.SPLIT}/auth/manage/role/page/:pageNumber/:pageSize/",
        s"${Method.GET}${BaseModel.SPLIT}/auth/manage/role/:id/",
        s"${Method.POST}${BaseModel.SPLIT}/auth/manage/role/",
        s"${Method.PUT}${BaseModel.SPLIT}/auth/manage/role/:id/",
        s"${Method.DELETE}${BaseModel.SPLIT}/auth/manage/role/:id/",
        s"${Method.GET}${BaseModel.SPLIT}/auth/manage/role/:id/enable/",
        s"${Method.GET}${BaseModel.SPLIT}/auth/manage/role/:id/disable/",
        s"${Method.GET}${BaseModel.SPLIT}/auth/manage/resource/",
        s"${Method.GET}${BaseModel.SPLIT}/auth/manage/resource/page/:pageNumber/:pageSize/",
        s"${Method.GET}${BaseModel.SPLIT}/auth/manage/resource/:id/",
        s"${Method.POST}${BaseModel.SPLIT}/auth/manage/resource/",
        s"${Method.PUT}${BaseModel.SPLIT}/auth/manage/resource/:id/",
        s"${Method.DELETE}${BaseModel.SPLIT}/auth/manage/resource/:id/",
        s"${Method.GET}${BaseModel.SPLIT}/auth/manage/resource/:id/enable/",
        s"${Method.GET}${BaseModel.SPLIT}/auth/manage/resource/:id/disable/"
      ))
      await(EZ_Role.save(role))

      val account = EZ_Account(EZ_Account.SYSTEM_ACCOUNT_CODE, "System Administrator", "admin", List(
        BaseModel.SPLIT + EZ_Role.SYSTEM_ROLE_CODE
      ))
      await(EZ_Account.save(account))
      logger.info("Initialized auth basic data.")
    }
  }

}
