package com.ecfront.ez.framework.service.auth

import com.ecfront.ez.framework.service.auth.model._
import com.ecfront.ez.framework.service.rpc.foundation.Method
import com.ecfront.ez.framework.service.storage.foundation.BaseModel
import com.typesafe.scalalogging.slf4j.LazyLogging

/**
  * RBAC 实体初始化器
  *
  * 添加默认的多个资源，2个角色、1号系统管理员账号、1个组织
  *
  */
object Initiator extends LazyLogging {

  def init(): Unit = {

    val exist = if (ServiceAdapter.mongoStorage) {
      EZ_Resource.existByCond(s"""{"code":"${Method.GET + BaseModel.SPLIT + "/auth/manage/organization/"}"}""")
    } else {
      EZ_Resource.existByCond(s"""code = ?""", List(Method.GET + BaseModel.SPLIT + "/auth/manage/organization/"))
    }
    if (!exist.body) {
      val org = EZ_Organization(ServiceAdapter.defaultOrganizationCode, "default")
      EZ_Organization.save(org)

      EZ_Resource.save(EZ_Resource(Method.GET, "/auth/manage/organization/", s"Find Organizations"))
      EZ_Resource.save(EZ_Resource(Method.GET, "/auth/manage/organization/page/:pageNumber/:pageSize/", s"Paging Organizations"))
      EZ_Resource.save(EZ_Resource(Method.GET, "/auth/manage/organization/:id/", s"Fetch Organization By Id"))
      EZ_Resource.save(EZ_Resource(Method.POST, "/auth/manage/organization/", s"Save a new Organization"))
      EZ_Resource.save(EZ_Resource(Method.PUT, "/auth/manage/organization/:id/", s"Update a exist Organization By Id"))
      EZ_Resource.save(EZ_Resource(Method.DELETE, "/auth/manage/organization/:id/", s"Delete a exist Organization By Id"))
      EZ_Resource.save(EZ_Resource(Method.GET, "/auth/manage/organization/:id/enable/", s"Enabled a exist Organization By Id"))
      EZ_Resource.save(EZ_Resource(Method.GET, "/auth/manage/organization/:id/disable/", s"Disabled a exist Organization By Id"))
      EZ_Resource.save(EZ_Resource(Method.POST, "/auth/manage/organization/res/", s"Upload Organization file"))
      EZ_Resource.save(EZ_Resource(Method.GET, "/auth/manage/organization/res/:date/:fileName", s"Download Organization file"))
      EZ_Resource.save(EZ_Resource(Method.GET, "/auth/manage/organization/export/", s"Export Organization file"))

      EZ_Resource.save(EZ_Resource(Method.GET, "/auth/manage/account/", s"Find Accounts"))
      EZ_Resource.save(EZ_Resource(Method.GET, "/auth/manage/account/page/:pageNumber/:pageSize/", s"Paging Accounts"))
      EZ_Resource.save(EZ_Resource(Method.GET, "/auth/manage/account/:id/", s"Fetch Account By Id"))
      EZ_Resource.save(EZ_Resource(Method.POST, "/auth/manage/account/", s"Save a new Account"))
      EZ_Resource.save(EZ_Resource(Method.PUT, "/auth/manage/account/:id/", s"Update a exist Account By Id"))
      EZ_Resource.save(EZ_Resource(Method.DELETE, "/auth/manage/account/:id/", s"Delete a exist Account By Id"))
      EZ_Resource.save(EZ_Resource(Method.GET, "/auth/manage/account/:id/enable/", s"Enabled a exist Account By Id"))
      EZ_Resource.save(EZ_Resource(Method.GET, "/auth/manage/account/:id/disable/", s"Disabled a exist Account By Id"))
      EZ_Resource.save(EZ_Resource(Method.POST, "/auth/manage/account/res/", s"Upload Account file"))
      EZ_Resource.save(EZ_Resource(Method.GET, "/auth/manage/account/res/:date/:fileName", s"Download Account file"))
      EZ_Resource.save(EZ_Resource(Method.GET, "/auth/manage/account/export/", s"Export Account file"))

      EZ_Resource.save(EZ_Resource(Method.GET, "/auth/manage/role/", s"Find Roles"))
      EZ_Resource.save(EZ_Resource(Method.GET, "/auth/manage/role/page/:pageNumber/:pageSize/", s"Paging Roles"))
      EZ_Resource.save(EZ_Resource(Method.GET, "/auth/manage/role/:id/", s"Fetch Role By Id"))
      EZ_Resource.save(EZ_Resource(Method.POST, "/auth/manage/role/", s"Save a new Role"))
      EZ_Resource.save(EZ_Resource(Method.PUT, "/auth/manage/role/:id/", s"Update a exist Role By Id"))
      EZ_Resource.save(EZ_Resource(Method.DELETE, "/auth/manage/role/:id/", s"Delete a exist Role By Id"))
      EZ_Resource.save(EZ_Resource(Method.GET, "/auth/manage/role/:id/enable/", s"Enabled a exist Role By Id"))
      EZ_Resource.save(EZ_Resource(Method.GET, "/auth/manage/role/:id/disable/", s"Disabled a exist Role By Id"))
      EZ_Resource.save(EZ_Resource(Method.POST, "/auth/manage/role/res/", s"Upload Role file"))
      EZ_Resource.save(EZ_Resource(Method.GET, "/auth/manage/role/res/:date/:fileName", s"Download Role file"))
      EZ_Resource.save(EZ_Resource(Method.GET, "/auth/manage/role/export/", s"Export Role file"))

      EZ_Resource.save(EZ_Resource(Method.GET, "/auth/manage/resource/", s"Find Resources"))
      EZ_Resource.save(EZ_Resource(Method.GET, "/auth/manage/resource/page/:pageNumber/:pageSize/", s"Paging Resources"))
      EZ_Resource.save(EZ_Resource(Method.GET, "/auth/manage/resource/:id/", s"Fetch Resource By Id"))
      EZ_Resource.save(EZ_Resource(Method.POST, "/auth/manage/resource/", s"Save a new Resource"))
      EZ_Resource.save(EZ_Resource(Method.PUT, "/auth/manage/resource/:id/", s"Update a exist Resource By Id"))
      EZ_Resource.save(EZ_Resource(Method.DELETE, "/auth/manage/resource/:id/", s"Delete a exist Resource By Id"))
      EZ_Resource.save(EZ_Resource(Method.GET, "/auth/manage/resource/:id/enable/", s"Enabled a exist Resource By Id"))
      EZ_Resource.save(EZ_Resource(Method.GET, "/auth/manage/resource/:id/disable/", s"Disabled a exist Resource By Id"))
      EZ_Resource.save(EZ_Resource(Method.POST, "/auth/manage/resource/res/", s"Upload Resource file"))
      EZ_Resource.save(EZ_Resource(Method.GET, "/auth/manage/resource/res/:date/:fileName", s"Download Resource file"))
      EZ_Resource.save(EZ_Resource(Method.GET, "/auth/manage/resource/export/", s"Export Resource file"))

      EZ_Resource.save(EZ_Resource(Method.GET, "/auth/manage/menu/", s"Find Menus"))
      EZ_Resource.save(EZ_Resource(Method.GET, "/auth/manage/menu/page/:pageNumber/:pageSize/", s"Paging Menus"))
      EZ_Resource.save(EZ_Resource(Method.GET, "/auth/manage/menu/:id/", s"Fetch Menu By Id"))
      EZ_Resource.save(EZ_Resource(Method.POST, "/auth/manage/menu/", s"Save a new Menu"))
      EZ_Resource.save(EZ_Resource(Method.PUT, "/auth/manage/menu/:id/", s"Update a exist Menu By Id"))
      EZ_Resource.save(EZ_Resource(Method.DELETE, "/auth/manage/menu/:id/", s"Delete a exist Menu By Id"))
      EZ_Resource.save(EZ_Resource(Method.GET, "/auth/manage/menu/:id/enable/", s"Enabled a exist Menu By Id"))
      EZ_Resource.save(EZ_Resource(Method.GET, "/auth/manage/menu/:id/disable/", s"Disabled a exist Menu By Id"))
      EZ_Resource.save(EZ_Resource(Method.POST, "/auth/manage/menu/res/", s"Upload Menu file"))
      EZ_Resource.save(EZ_Resource(Method.GET, "/auth/manage/menu/res/:date/:fileName", s"Download Menu file"))
      EZ_Resource.save(EZ_Resource(Method.GET, "/auth/manage/menu/export/", s"Export Menu file"))

      EZ_Role.save(EZ_Role(EZ_Role.SYSTEM_ROLE_FLAG, "System", List(
        s"${Method.GET}${BaseModel.SPLIT}/auth/manage/organization/",
        s"${Method.GET}${BaseModel.SPLIT}/auth/manage/organization/page/:pageNumber/:pageSize/",
        s"${Method.GET}${BaseModel.SPLIT}/auth/manage/organization/:id/",
        s"${Method.POST}${BaseModel.SPLIT}/auth/manage/organization/",
        s"${Method.PUT}${BaseModel.SPLIT}/auth/manage/organization/:id/",
        s"${Method.DELETE}${BaseModel.SPLIT}/auth/manage/organization/:id/",
        s"${Method.GET}${BaseModel.SPLIT}/auth/manage/organization/:id/enable/",
        s"${Method.GET}${BaseModel.SPLIT}/auth/manage/organization/:id/disable/",
        s"${Method.POST}${BaseModel.SPLIT}/auth/manage/organization/res/",
        s"${Method.GET}${BaseModel.SPLIT}/auth/manage/organization/res/:date/:fileName",
        s"${Method.GET}${BaseModel.SPLIT}/auth/manage/organization/export/",
        s"${Method.GET}${BaseModel.SPLIT}/auth/manage/account/",
        s"${Method.GET}${BaseModel.SPLIT}/auth/manage/account/page/:pageNumber/:pageSize/",
        s"${Method.GET}${BaseModel.SPLIT}/auth/manage/account/:id/",
        s"${Method.POST}${BaseModel.SPLIT}/auth/manage/account/",
        s"${Method.PUT}${BaseModel.SPLIT}/auth/manage/account/:id/",
        s"${Method.DELETE}${BaseModel.SPLIT}/auth/manage/account/:id/",
        s"${Method.GET}${BaseModel.SPLIT}/auth/manage/account/:id/enable/",
        s"${Method.GET}${BaseModel.SPLIT}/auth/manage/account/:id/disable/",
        s"${Method.POST}${BaseModel.SPLIT}/auth/manage/account/res/",
        s"${Method.GET}${BaseModel.SPLIT}/auth/manage/account/res/:date/:fileName",
        s"${Method.GET}${BaseModel.SPLIT}/auth/manage/account/export/",
        s"${Method.GET}${BaseModel.SPLIT}/auth/manage/role/",
        s"${Method.GET}${BaseModel.SPLIT}/auth/manage/role/page/:pageNumber/:pageSize/",
        s"${Method.GET}${BaseModel.SPLIT}/auth/manage/role/:id/",
        s"${Method.POST}${BaseModel.SPLIT}/auth/manage/role/",
        s"${Method.PUT}${BaseModel.SPLIT}/auth/manage/role/:id/",
        s"${Method.DELETE}${BaseModel.SPLIT}/auth/manage/role/:id/",
        s"${Method.GET}${BaseModel.SPLIT}/auth/manage/role/:id/enable/",
        s"${Method.GET}${BaseModel.SPLIT}/auth/manage/role/:id/disable/",
        s"${Method.POST}${BaseModel.SPLIT}/auth/manage/role/res/",
        s"${Method.GET}${BaseModel.SPLIT}/auth/manage/role/res/:date/:fileName",
        s"${Method.GET}${BaseModel.SPLIT}/auth/manage/role/export/",
        s"${Method.GET}${BaseModel.SPLIT}/auth/manage/resource/",
        s"${Method.GET}${BaseModel.SPLIT}/auth/manage/resource/page/:pageNumber/:pageSize/",
        s"${Method.GET}${BaseModel.SPLIT}/auth/manage/resource/:id/",
        s"${Method.POST}${BaseModel.SPLIT}/auth/manage/resource/",
        s"${Method.PUT}${BaseModel.SPLIT}/auth/manage/resource/:id/",
        s"${Method.DELETE}${BaseModel.SPLIT}/auth/manage/resource/:id/",
        s"${Method.GET}${BaseModel.SPLIT}/auth/manage/resource/:id/enable/",
        s"${Method.GET}${BaseModel.SPLIT}/auth/manage/resource/:id/disable/",
        s"${Method.POST}${BaseModel.SPLIT}/auth/manage/resource/res/",
        s"${Method.GET}${BaseModel.SPLIT}/auth/manage/resource/res/:date/:fileName",
        s"${Method.GET}${BaseModel.SPLIT}/auth/manage/resource/export/",
        s"${Method.GET}${BaseModel.SPLIT}/auth/manage/menu/",
        s"${Method.GET}${BaseModel.SPLIT}/auth/manage/menu/page/:pageNumber/:pageSize/",
        s"${Method.GET}${BaseModel.SPLIT}/auth/manage/menu/:id/",
        s"${Method.POST}${BaseModel.SPLIT}/auth/manage/menu/",
        s"${Method.PUT}${BaseModel.SPLIT}/auth/manage/menu/:id/",
        s"${Method.DELETE}${BaseModel.SPLIT}/auth/manage/menu/:id/",
        s"${Method.GET}${BaseModel.SPLIT}/auth/manage/menu/:id/enable/",
        s"${Method.GET}${BaseModel.SPLIT}/auth/manage/menu/:id/disable/",
        s"${Method.POST}${BaseModel.SPLIT}/auth/manage/menu/res/",
        s"${Method.GET}${BaseModel.SPLIT}/auth/manage/menu/res/:date/:fileName",
        s"${Method.GET}${BaseModel.SPLIT}/auth/manage/menu/export/"
      )))
      EZ_Role.save(EZ_Role(EZ_Role.USER_ROLE_FLAG, "User", List(
        s"${Method.POST}${BaseModel.SPLIT}/auth/manage/account/res/",
        s"${Method.GET}${BaseModel.SPLIT}/auth/manage/account/res/:date/:fileName"
      )))

      val account = EZ_Account(EZ_Account.SYSTEM_ACCOUNT_CODE, "admin" + EZ_Account.VIRTUAL_EMAIL, "Sys Admin", "admin", List(
        BaseModel.SPLIT + EZ_Role.SYSTEM_ROLE_FLAG
      ))
      EZ_Account.save(account)

      EZ_Menu.save(EZ_Menu("ez.dashboard", "Dashboard", "",
        List(BaseModel.SPLIT + EZ_Role.USER_ROLE_FLAG, BaseModel.SPLIT + EZ_Role.SYSTEM_ROLE_FLAG),
        "icon-home", "sidebar.nav.DASHBARD", 10000))
      EZ_Menu.save(EZ_Menu("#sysManage", "System Manage", "",
        List(BaseModel.SPLIT + EZ_Role.SYSTEM_ROLE_FLAG),
        "icon-settings", "sidebar.nav.sysManage._", -1))
      EZ_Menu.save(EZ_Menu("ez.sysmanage-organization-list", "Organization", BaseModel.SPLIT + "#sysManage",
        List(BaseModel.SPLIT + EZ_Role.SYSTEM_ROLE_FLAG),
        "icon-globe", "sidebar.nav.sysManage.Organization"))
      EZ_Menu.save(EZ_Menu("ez.sysmanage-resource-list",
        "Resource", BaseModel.SPLIT + "#sysManage",
        List(BaseModel.SPLIT + EZ_Role.SYSTEM_ROLE_FLAG),
        "icon-basket-loaded", "sidebar.nav.sysManage.RESOURCE"))
      EZ_Menu.save(EZ_Menu("ez.sysmanage-role-list", "Role", BaseModel.SPLIT + "#sysManage",
        List(BaseModel.SPLIT + EZ_Role.SYSTEM_ROLE_FLAG),
        "icon-shuffle", "sidebar.nav.sysManage.ROLE"))
      EZ_Menu.save(EZ_Menu("ez.sysmanage-account-list", "Account", BaseModel.SPLIT + "#sysManage",
        List(BaseModel.SPLIT + EZ_Role.SYSTEM_ROLE_FLAG),
        "icon-people", "sidebar.nav.sysManage.ACCOUNT"))
      EZ_Menu.save(EZ_Menu("ez.sysmanage-menu-list", "Menu", BaseModel.SPLIT + "#sysManage",
        List(BaseModel.SPLIT + EZ_Role.SYSTEM_ROLE_FLAG),
        "icon-grid", "sidebar.nav.sysManage.MENU"))

      logger.info("Initialized auth basic data.")
    }
  }

}
