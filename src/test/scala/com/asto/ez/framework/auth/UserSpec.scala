package com.asto.ez.framework.auth

import com.asto.ez.framework.MockStartupSpec
import com.asto.ez.framework.rpc.http.HttpClientProcessor
import com.ecfront.common.{AsyncResp, Resp, StandardCode}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Promise}

class UserSpec extends MockStartupSpec {

  test("User Register test") {

    Thread.sleep(5000)
    val account = Account_VO()
    account.login_id = "u1"
    account.new_password = "123"
    assert(Await.result(HttpClientProcessor.post(
      s"http://127.0.0.1:8080/public/register/",
      account, classOf[String]), Duration.Inf).code == StandardCode.BAD_REQUEST)

    account.email = "net@"
    assert(Await.result(HttpClientProcessor.post(
      s"http://127.0.0.1:8080/public/register/",
      account, classOf[String]), Duration.Inf).message == "【email】format error")

    account.name = "u1"
    account.email = "i@sunisle.org"
    assert(Await.result(HttpClientProcessor.post(
      s"http://127.0.0.1:8080/public/register/",
      account, classOf[String]), Duration.Inf).message.contains("unique"))

    account.email = "net@sunisle.org"
    assert(Await.result(HttpClientProcessor.post(
      s"http://127.0.0.1:8080/public/register/",
      account, classOf[String]), Duration.Inf))

  }

  test("User Active test") {

    Thread.sleep(5000)
    assert(Await.result(HttpClientProcessor.get(
      s"http://0.0.0.0:8080/public/active/account/i@sunisle.org/74df7c7f-7d7d-47a0-bfc9-2b6df1c586cd12325753090669/ ",
      classOf[String]), Duration.Inf).code == StandardCode.NOT_FOUND)
    assert(Await.result(HttpClientProcessor.get(
      s"http://0.0.0.0:8080/public/active/account/net@sunisle.org/74df7c7f-7d7d-47a0-bfc9-2b6df1c586cd12325753090669/ ",
      classOf[String]), Duration.Inf))

  }


  test("User Get Or Update test") {

    Thread.sleep(5000)
    val loginP = Promise[Resp[Token_Info_VO]]()
    AuthService.doLogin("u1", "123", AsyncResp(loginP))
    val token = Await.result(loginP.future, Duration.Inf).body.token
    val accountVO = Await.result(HttpClientProcessor.get(
      s"http://0.0.0.0:8080/auth/manage/account/bylogin/?__ez_token__=$token",
      classOf[Account_VO]), Duration.Inf).body
    assert(accountVO.login_id == "u1")
    accountVO.name = "u2"
    accountVO.old_password = "111"
    assert(Await.result(HttpClientProcessor.put(
      s"http://0.0.0.0:8080/auth/manage/account/bylogin/?__ez_token__=$token",
      accountVO, classOf[String]), Duration.Inf).message == "Old Password Error")
    accountVO.old_password = "123"
    accountVO.new_password = "111"
    assert(Await.result(HttpClientProcessor.put(
      s"http://0.0.0.0:8080/auth/manage/account/bylogin/?__ez_token__=$token",
      accountVO, classOf[String]), Duration.Inf))

  }

  test("User Found Password test") {

    Thread.sleep(5000)
    Await.result(HttpClientProcessor.put(
      s"http://0.0.0.0:8080/public/findpassword/net@sunisle.org/", Map("newPassword" -> "abc"),
      classOf[Void]), Duration.Inf).body

  }

  test("User Active New Password test") {

    Thread.sleep(5000)
    assert(Await.result(HttpClientProcessor.get(
      s"http://0.0.0.0:8080/public/active/password/i@sunisle.org/83a10fc0-9fbe-4da4-beb0-6e90b9adfc0814147819669595/",
      classOf[String]), Duration.Inf).code == StandardCode.NOT_FOUND)
    assert(Await.result(HttpClientProcessor.get(
      s"http://0.0.0.0:8080/public/active/password/net@sunisle.org/83a10fc0-9fbe-4da4-beb0-6e90b9adfc0814147819669595/",
      classOf[String]), Duration.Inf))
    val loginP = Promise[Resp[Token_Info_VO]]()
    AuthService.doLogin("u1", "abc", AsyncResp(loginP))
    assert(Await.result(loginP.future, Duration.Inf))

  }

}


