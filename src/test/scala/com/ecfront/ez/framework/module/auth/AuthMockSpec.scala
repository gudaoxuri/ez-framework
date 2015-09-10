package com.ecfront.ez.framework.module.auth

import com.ecfront.common.{JsonHelper, Resp, StandardCode}
import com.ecfront.ez.framework.helper.HttpHelper
import com.ecfront.ez.framework.module.auth.manage.AccountService
import org.scalatest.FunSuite

class AuthMockSpec extends FunSuite {

  test("auth 测试") {
    AuthBasic.init()

    val result = JsonHelper.toGenericObject[Resp[Token_Info_VO]](HttpHelper.post(s"http://127.0.0.1:8080/public/auth/login/",Map("loginId" -> "sysadmin","password" -> "admin")))
    assert(result)
    val token = result.body.token
    assert(token != "")
    println("Token:" + token)
    val loginInfo = JsonHelper.toGenericObject[Resp[Token_Info_VO]](HttpHelper.get(s"http://127.0.0.1:8080/auth/logininfo/?_token_=$token")).body
    assert(loginInfo.login_id == "sysadmin")
    val account = EZ_Account()
    account.id = "testUser"
    account.name = "测试用户"
    account.password = "456"
    account.role_ids = Map("user" -> null)
    JsonHelper.toGenericObject[Resp[String]](HttpHelper.post(s"http://127.0.0.1:8080/auth/manage/account/?_token_=$token",account))
    val getAccount = JsonHelper.toGenericObject[Resp[EZ_Account]](HttpHelper.get(s"http://127.0.0.1:8080/auth/manage/account/testUser/?_token_=$token")).body
    assert(getAccount.id == "testUser")
    assert(getAccount.name == "测试用户")
    assert(getAccount.password == AccountService.packageEncryptPwd("testUser", "456"))
    assert(getAccount.role_ids("user").name == "普通用户")
    JsonHelper.toGenericObject[Resp[Void]](HttpHelper.get(s"http://127.0.0.1:8080/auth/logout/?_token_=$token"))
    val getAccountWrap = JsonHelper.toGenericObject[Resp[EZ_Account]](HttpHelper.get(s"http://127.0.0.1:8080/auth/manage/account/testUser/?_token_=$token"))
    assert(getAccountWrap.code == StandardCode.UNAUTHORIZED)


  }


}




