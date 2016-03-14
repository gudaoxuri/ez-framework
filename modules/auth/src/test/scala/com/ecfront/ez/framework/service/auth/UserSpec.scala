package com.ecfront.ez.framework.service.auth

import com.ecfront.common.{JsonHelper, Resp, StandardCode}
import com.ecfront.ez.framework.core.test.MockStartupSpec
import com.ecfront.ez.framework.service.rpc.http.HttpClientProcessor

class UserSpec extends MockStartupSpec {

  test("User Register test") {

    val account = Account_VO()
    account.login_id = "u1"
    account.new_password = "123"
    assert(
      JsonHelper.toObject[Resp[String]](
        HttpClientProcessor.post(
          s"http://127.0.0.1:8080/public/register/",
          account)).code == StandardCode.BAD_REQUEST)
    account.email = "net@"
    assert(
      JsonHelper.toObject[Resp[String]](
        HttpClientProcessor.post(
          s"http://127.0.0.1:8080/public/register/",
          account)).message == "【email】format error")

    account.name = "u1"
    account.email = "i@sunisle.org"
    assert(
      JsonHelper.toObject[Resp[String]](
        HttpClientProcessor.post(
          s"http://127.0.0.1:8080/public/register/",
          account)).message.contains("unique"))

    account.email = "net@sunisle.org"
    assert(
      JsonHelper.toObject[Resp[String]](
        HttpClientProcessor.post(
          s"http://127.0.0.1:8080/public/register/",
          account)))

  }

  test("User Active test") {

    assert(
      JsonHelper.toObject[Resp[String]](
        HttpClientProcessor.get(
          s"http://0.0.0.0:8080/public/active/account/i@sunisle.org/74df7c7f-7d7d-47a0-bfc9-2b6df1c586cd12325753090669/ ")
      ).code == StandardCode.NOT_FOUND)
    assert(
      JsonHelper.toObject[Resp[String]](
        HttpClientProcessor.get(
          s"http://0.0.0.0:8080/public/active/account/net@sunisle.org/74df7c7f-7d7d-47a0-bfc9-2b6df1c586cd12325753090669/ ")
      ))

  }


  test("User Get Or Update test") {

    val token = AuthService.doLogin("u1", "123").body.token
    val accountVO = JsonHelper.toObject[Resp[Account_VO]](
      HttpClientProcessor.get(
        s"http://0.0.0.0:8080/auth/manage/account/bylogin/?__ez_token__=$token")).body
    assert(accountVO.login_id == "u1")
    accountVO.name = "u2"
    accountVO.old_password = "111"
    assert(JsonHelper.toObject[Resp[String]](
      HttpClientProcessor.put(
        s"http://0.0.0.0:8080/auth/manage/account/bylogin/?__ez_token__=$token",
        accountVO)).message == "Old Password Error")
    accountVO.old_password = "123"
    accountVO.new_password = "111"
    assert(JsonHelper.toObject[Resp[String]](
      HttpClientProcessor.put(
        s"http://0.0.0.0:8080/auth/manage/account/bylogin/?__ez_token__=$token",
        accountVO)))

  }

  test("User Found Password test") {

    JsonHelper.toObject[Resp[Void]](
      HttpClientProcessor.put(
        s"http://0.0.0.0:8080/public/findpassword/net@sunisle.org/", Map("newPassword" -> "abc"))).body

  }

  test("User Active New Password test") {

    assert(JsonHelper.toObject[Resp[String]](
      HttpClientProcessor.get(
        s"http://0.0.0.0:8080/public/active/password/i@sunisle.org/83a10fc0-9fbe-4da4-beb0-6e90b9adfc0814147819669595/")
    ).code == StandardCode.NOT_FOUND)
    assert(JsonHelper.toObject[Resp[String]](
      HttpClientProcessor.get(
        s"http://0.0.0.0:8080/public/active/password/net@sunisle.org/83a10fc0-9fbe-4da4-beb0-6e90b9adfc0814147819669595/")
    ))
    assert(AuthService.doLogin("u1", "abc"))

  }

}


