package com.asto.ez.framework.auth.manage

import com.asto.ez.framework.auth._
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
      await(EZ_Resource.save(EZ_Resource(Method.GET, "/auth/manage/organization/res/", s"Upload Organization file")))

      await(EZ_Resource.save(EZ_Resource(Method.GET, "/auth/manage/account/", s"Find Accounts")))
      await(EZ_Resource.save(EZ_Resource(Method.GET, "/auth/manage/account/page/:pageNumber/:pageSize/", s"Paging Accounts")))
      await(EZ_Resource.save(EZ_Resource(Method.GET, "/auth/manage/account/:id/", s"Fetch Account By Id")))
      await(EZ_Resource.save(EZ_Resource(Method.POST, "/auth/manage/account/", s"Save a new Account")))
      await(EZ_Resource.save(EZ_Resource(Method.PUT, "/auth/manage/account/:id/", s"Update a exist Account By Id")))
      await(EZ_Resource.save(EZ_Resource(Method.DELETE, "/auth/manage/account/:id/", s"Delete a exist Account By Id")))
      await(EZ_Resource.save(EZ_Resource(Method.GET, "/auth/manage/account/:id/enable/", s"Enabled a exist Account By Id")))
      await(EZ_Resource.save(EZ_Resource(Method.GET, "/auth/manage/account/:id/disable/", s"Disabled a exist Account By Id")))
      await(EZ_Resource.save(EZ_Resource(Method.GET, "/auth/manage/account/res/", s"Upload Account file")))

      await(EZ_Resource.save(EZ_Resource(Method.GET, "/auth/manage/role/", s"Find Roles")))
      await(EZ_Resource.save(EZ_Resource(Method.GET, "/auth/manage/role/page/:pageNumber/:pageSize/", s"Paging Roles")))
      await(EZ_Resource.save(EZ_Resource(Method.GET, "/auth/manage/role/:id/", s"Fetch Role By Id")))
      await(EZ_Resource.save(EZ_Resource(Method.POST, "/auth/manage/role/", s"Save a new Role")))
      await(EZ_Resource.save(EZ_Resource(Method.PUT, "/auth/manage/role/:id/", s"Update a exist Role By Id")))
      await(EZ_Resource.save(EZ_Resource(Method.DELETE, "/auth/manage/role/:id/", s"Delete a exist Role By Id")))
      await(EZ_Resource.save(EZ_Resource(Method.GET, "/auth/manage/role/:id/enable/", s"Enabled a exist Role By Id")))
      await(EZ_Resource.save(EZ_Resource(Method.GET, "/auth/manage/role/:id/disable/", s"Disabled a exist Role By Id")))
      await(EZ_Resource.save(EZ_Resource(Method.GET, "/auth/manage/role/res/", s"Upload Role file")))

      await(EZ_Resource.save(EZ_Resource(Method.GET, "/auth/manage/resource/", s"Find Resources")))
      await(EZ_Resource.save(EZ_Resource(Method.GET, "/auth/manage/resource/page/:pageNumber/:pageSize/", s"Paging Resources")))
      await(EZ_Resource.save(EZ_Resource(Method.GET, "/auth/manage/resource/:id/", s"Fetch Resource By Id")))
      await(EZ_Resource.save(EZ_Resource(Method.POST, "/auth/manage/resource/", s"Save a new Resource")))
      await(EZ_Resource.save(EZ_Resource(Method.PUT, "/auth/manage/resource/:id/", s"Update a exist Resource By Id")))
      await(EZ_Resource.save(EZ_Resource(Method.DELETE, "/auth/manage/resource/:id/", s"Delete a exist Resource By Id")))
      await(EZ_Resource.save(EZ_Resource(Method.GET, "/auth/manage/resource/:id/enable/", s"Enabled a exist Resource By Id")))
      await(EZ_Resource.save(EZ_Resource(Method.GET, "/auth/manage/resource/:id/disable/", s"Disabled a exist Resource By Id")))
      await(EZ_Resource.save(EZ_Resource(Method.GET, "/auth/manage/resource/res/", s"Upload Resource file")))

      await(EZ_Resource.save(EZ_Resource(Method.GET, "/auth/manage/menu/", s"Find Menus")))
      await(EZ_Resource.save(EZ_Resource(Method.GET, "/auth/manage/menu/page/:pageNumber/:pageSize/", s"Paging Menus")))
      await(EZ_Resource.save(EZ_Resource(Method.GET, "/auth/manage/menu/:id/", s"Fetch Menu By Id")))
      await(EZ_Resource.save(EZ_Resource(Method.POST, "/auth/manage/menu/", s"Save a new Menu")))
      await(EZ_Resource.save(EZ_Resource(Method.PUT, "/auth/manage/menu/:id/", s"Update a exist Menu By Id")))
      await(EZ_Resource.save(EZ_Resource(Method.DELETE, "/auth/manage/menu/:id/", s"Delete a exist Menu By Id")))
      await(EZ_Resource.save(EZ_Resource(Method.GET, "/auth/manage/menu/:id/enable/", s"Enabled a exist Menu By Id")))
      await(EZ_Resource.save(EZ_Resource(Method.GET, "/auth/manage/menu/:id/disable/", s"Disabled a exist Menu By Id")))
      await(EZ_Resource.save(EZ_Resource(Method.GET, "/auth/manage/menu/res/", s"Upload Menu file")))

      await(EZ_Resource.save(EZ_Resource(Method.GET, "/auth/manage/account/bylogin/", s"Fetch Account By Login")))
      await(EZ_Resource.save(EZ_Resource(Method.PUT, "/auth/manage/account/bylogin/", s"Update Account By Login")))

      val role = EZ_Role(EZ_Role.SYSTEM_ROLE_CODE, "System", List(
        s"${Method.GET}${BaseModel.SPLIT}/auth/manage/organization/",
        s"${Method.GET}${BaseModel.SPLIT}/auth/manage/organization/page/:pageNumber/:pageSize/",
        s"${Method.GET}${BaseModel.SPLIT}/auth/manage/organization/:id/",
        s"${Method.POST}${BaseModel.SPLIT}/auth/manage/organization/",
        s"${Method.PUT}${BaseModel.SPLIT}/auth/manage/organization/:id/",
        s"${Method.DELETE}${BaseModel.SPLIT}/auth/manage/organization/:id/",
        s"${Method.GET}${BaseModel.SPLIT}/auth/manage/organization/:id/enable/",
        s"${Method.GET}${BaseModel.SPLIT}/auth/manage/organization/:id/disable/",
        s"${Method.GET}${BaseModel.SPLIT}/auth/manage/organization/res/",
        s"${Method.GET}${BaseModel.SPLIT}/auth/manage/account/",
        s"${Method.GET}${BaseModel.SPLIT}/auth/manage/account/page/:pageNumber/:pageSize/",
        s"${Method.GET}${BaseModel.SPLIT}/auth/manage/account/:id/",
        s"${Method.POST}${BaseModel.SPLIT}/auth/manage/account/",
        s"${Method.PUT}${BaseModel.SPLIT}/auth/manage/account/:id/",
        s"${Method.DELETE}${BaseModel.SPLIT}/auth/manage/account/:id/",
        s"${Method.GET}${BaseModel.SPLIT}/auth/manage/account/:id/enable/",
        s"${Method.GET}${BaseModel.SPLIT}/auth/manage/account/:id/disable/",
        s"${Method.GET}${BaseModel.SPLIT}/auth/manage/account/res/",
        s"${Method.GET}${BaseModel.SPLIT}/auth/manage/role/",
        s"${Method.GET}${BaseModel.SPLIT}/auth/manage/role/page/:pageNumber/:pageSize/",
        s"${Method.GET}${BaseModel.SPLIT}/auth/manage/role/:id/",
        s"${Method.POST}${BaseModel.SPLIT}/auth/manage/role/",
        s"${Method.PUT}${BaseModel.SPLIT}/auth/manage/role/:id/",
        s"${Method.DELETE}${BaseModel.SPLIT}/auth/manage/role/:id/",
        s"${Method.GET}${BaseModel.SPLIT}/auth/manage/role/:id/enable/",
        s"${Method.GET}${BaseModel.SPLIT}/auth/manage/role/:id/disable/",
        s"${Method.GET}${BaseModel.SPLIT}/auth/manage/role/res/",
        s"${Method.GET}${BaseModel.SPLIT}/auth/manage/resource/",
        s"${Method.GET}${BaseModel.SPLIT}/auth/manage/resource/page/:pageNumber/:pageSize/",
        s"${Method.GET}${BaseModel.SPLIT}/auth/manage/resource/:id/",
        s"${Method.POST}${BaseModel.SPLIT}/auth/manage/resource/",
        s"${Method.PUT}${BaseModel.SPLIT}/auth/manage/resource/:id/",
        s"${Method.DELETE}${BaseModel.SPLIT}/auth/manage/resource/:id/",
        s"${Method.GET}${BaseModel.SPLIT}/auth/manage/resource/:id/enable/",
        s"${Method.GET}${BaseModel.SPLIT}/auth/manage/resource/:id/disable/",
        s"${Method.GET}${BaseModel.SPLIT}/auth/manage/resource/res/",
        s"${Method.GET}${BaseModel.SPLIT}/auth/manage/menu/",
        s"${Method.GET}${BaseModel.SPLIT}/auth/manage/menu/page/:pageNumber/:pageSize/",
        s"${Method.GET}${BaseModel.SPLIT}/auth/manage/menu/:id/",
        s"${Method.POST}${BaseModel.SPLIT}/auth/manage/menu/",
        s"${Method.PUT}${BaseModel.SPLIT}/auth/manage/menu/:id/",
        s"${Method.DELETE}${BaseModel.SPLIT}/auth/manage/menu/:id/",
        s"${Method.GET}${BaseModel.SPLIT}/auth/manage/menu/:id/enable/",
        s"${Method.GET}${BaseModel.SPLIT}/auth/manage/menu/:id/disable/",
        s"${Method.GET}${BaseModel.SPLIT}/auth/manage/menu/res/"
      ))
      await(EZ_Role.save(role))
      await(EZ_Role.save( EZ_Role(EZ_Role.USER_ROLE_CODE, "User",List(
        s"${Method.GET}${BaseModel.SPLIT}/auth/manage/account/bylogin/",
        s"${Method.PUT}${BaseModel.SPLIT}/auth/manage/account/bylogin/"
      ))))

      val account = EZ_Account(EZ_Account.SYSTEM_ACCOUNT_CODE, "i@sunisle.org","System Administrator", "admin", List(
        BaseModel.SPLIT + EZ_Role.SYSTEM_ROLE_CODE
      ))
      await(EZ_Account.save(account))

      await(EZ_Menu.save(EZ_Menu("ez.dashboard", "Dashboard", "", List(BaseModel.SPLIT + EZ_Role.USER_ROLE_CODE,BaseModel.SPLIT + EZ_Role.SYSTEM_ROLE_CODE), "icon-home", "sidebar.nav.DASHBARD")))
      await(EZ_Menu.save(EZ_Menu("#sysManage", "System Manage", "", List(BaseModel.SPLIT + EZ_Role.SYSTEM_ROLE_CODE), "icon-settings", "sidebar.nav.sysManage._")))
      await(EZ_Menu.save(EZ_Menu("ez.sysmanage-organization-list", "Organization", "#sysManage", List(BaseModel.SPLIT + EZ_Role.SYSTEM_ROLE_CODE), "icon-globe", "sidebar.nav.sysManage.Organization")))
      await(EZ_Menu.save(EZ_Menu("ez.sysmanage-resource-list", "Resource", "#sysManage", List(BaseModel.SPLIT + EZ_Role.SYSTEM_ROLE_CODE), "icon-basket-loaded", "sidebar.nav.sysManage.RESOURCE")))
      await(EZ_Menu.save(EZ_Menu("ez.sysmanage-role-list", "Role", "#sysManage", List(BaseModel.SPLIT + EZ_Role.SYSTEM_ROLE_CODE), "icon-shuffle", "sidebar.nav.sysManage.ROLE")))
      await(EZ_Menu.save(EZ_Menu("ez.sysmanage-account-list", "Account", "#sysManage", List(BaseModel.SPLIT + EZ_Role.SYSTEM_ROLE_CODE), "icon-people", "sidebar.nav.sysManage.ACCOUNT")))
      await(EZ_Menu.save(EZ_Menu("ez.sysmanage-menu-list", "Menu", "#sysManage", List(BaseModel.SPLIT + EZ_Role.SYSTEM_ROLE_CODE), "icon-grid", "sidebar.nav.sysManage.MENU")))

      logger.info("Initialized auth basic data.")
    }
  }

}
