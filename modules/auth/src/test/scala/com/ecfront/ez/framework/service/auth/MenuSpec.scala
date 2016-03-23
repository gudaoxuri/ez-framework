package com.ecfront.ez.framework.service.auth

import com.ecfront.ez.framework.core.test.MockStartupSpec
import com.ecfront.ez.framework.service.auth.model.EZ_Menu

class MenuSpec extends MockStartupSpec {

  test("Menu test") {

    EZ_Menu.deleteByCond("")

    EZ_Menu.save(EZ_Menu("pub.a", "P_A", "", List()))
    EZ_Menu.save(EZ_Menu("pub.a.a", "P_A_A", "pub.a", List()))
    EZ_Menu.save(EZ_Menu("pub.a.b", "P_A_B", "pub.a", List()))
    EZ_Menu.save(EZ_Menu("sys.a", "S_A", "", List("@system")))
    EZ_Menu.save(EZ_Menu("sys.b", "s_B", "", List("@system", "@user")))

    val pubMenus = AuthService.doGetMenus(List()).body
    assert(pubMenus.length == 3)

    val roleCodes = AuthService.doLogin("sysadmin", "admin").body.role_info.keys.toList
    val authMenus = AuthService.doGetMenus(roleCodes).body
    assert(authMenus.length == 5)

  }

}


