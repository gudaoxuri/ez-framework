package com.ecfront.ez.framework.module.auth

import com.ecfront.common.{JsonHelper, Resp, StandardCode}
import com.ecfront.ez.framework.helper.HttpHelper
import com.ecfront.ez.framework.module.auth.manage.AccountService
import org.scalatest.FunSuite

class AuthMockSpec extends FunSuite {

  test("auth 测试") {

    new Thread(new Runnable {
      override def run(): Unit = {
        MockStartup
      }
    }).start()

    Thread.sleep(10000)

    AuthBasic.init()

    val result = JsonHelper.toGenericObject[Resp[Token_Info_VO]](HttpHelper.post(s"http://127.0.0.1:8080/public/auth/login/",Map("loginId" -> "sysadmin","password" -> "admin")).body)
    assert(result)
    val token = result.body.token
    assert(token != "")
    println("Token:" + token)
    val loginInfo = JsonHelper.toGenericObject[Resp[Token_Info_VO]](HttpHelper.get(s"http://127.0.0.1:8080/auth/logininfo/?ez_token=$token").body).body
    assert(loginInfo.login_id == "sysadmin")
    val account = Account()
    account.id = "testUser"
    account.name = "测试用户"
    account.password = "456"
    account.role_ids = Map("user" -> null)
    JsonHelper.toGenericObject[Resp[String]](HttpHelper.post(s"http://127.0.0.1:8080/auth/manage/account/?ez_token=$token",account).body)
    val getAccount = JsonHelper.toGenericObject[Resp[Account]](HttpHelper.get(s"http://127.0.0.1:8080/auth/manage/account/testUser/?ez_token=$token").body).body
    assert(getAccount.id == "testUser")
    assert(getAccount.name == "测试用户")
    assert(getAccount.password == AccountService.packageEncryptPwd("testUser", "456"))
    assert(getAccount.role_ids == Map("user" -> "普通用户"))
    JsonHelper.toGenericObject[Resp[Void]](HttpHelper.get(s"http://127.0.0.1:8080/auth/logout/?ez_token=$token").body)
    val getAccountWrap = JsonHelper.toGenericObject[Resp[Account]](HttpHelper.get(s"http://127.0.0.1:8080/auth/manage/account/testUser/?ez_token=$token").body)
    assert(getAccountWrap.code == StandardCode.UNAUTHORIZED)


  }


}




