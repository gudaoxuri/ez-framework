package com.asto.ez.framework.auth

import com.asto.ez.framework.MockStartupSpec
import com.ecfront.common.{AsyncResp, Resp}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Promise}

class MenuSpec extends MockStartupSpec {

  test("Menu test") {

    EZ_Menu.deleteByCond("{}")

    Await.result(EZ_Menu.save(EZ_Menu("pub.a", "P_A", "", List())), Duration.Inf)
    Await.result(EZ_Menu.save(EZ_Menu("pub.a.a", "P_A_A", "pub.a", List())), Duration.Inf)
    Await.result(EZ_Menu.save(EZ_Menu("pub.a.b", "P_A_B", "pub.a", List())), Duration.Inf)
    Await.result(EZ_Menu.save(EZ_Menu("sys.a", "S_A", "", List("@system"))), Duration.Inf)
    Await.result(EZ_Menu.save(EZ_Menu("sys.b", "s_B", "", List("@system", "@user"))), Duration.Inf)

    val pubMenuP = Promise[Resp[List[EZ_Menu]]]()
    AuthService.doGetMenus(List(), AsyncResp(pubMenuP))
    val pubMenus = Await.result(pubMenuP.future, Duration.Inf).body
    assert(pubMenus.length == 3)

    val loginP = Promise[Resp[Token_Info_VO]]()
    AuthService.doLogin("sysadmin", "admin", AsyncResp(loginP))
    val roleCodes = Await.result(loginP.future, Duration.Inf).body.role_info.keys.toList
    val authMenuP = Promise[Resp[List[EZ_Menu]]]()
    AuthService.doGetMenus(roleCodes, AsyncResp(authMenuP))
    val authMenus = Await.result(authMenuP.future, Duration.Inf).body
    assert(authMenus.length == 5)

  }

}


