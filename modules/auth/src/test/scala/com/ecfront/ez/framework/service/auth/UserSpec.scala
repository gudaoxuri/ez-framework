package com.ecfront.ez.framework.service.auth

import com.ecfront.common.{JsonHelper, Resp, StandardCode}
import com.ecfront.ez.framework.core.test.MockStartupSpec
import com.ecfront.ez.framework.service.auth.model.EZ_Account
import com.ecfront.ez.framework.service.rpc.http.HttpClientProcessor

class UserSpec extends MockStartupSpec {

  // Step 1 Register
  test("User Register test") {

    EZ_Account.deleteByEmail("net@sunisle.org", "")

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
          account)).message.contains("exist"))

    account.email = "net@sunisle.org"
    assert(
      JsonHelper.toObject[Resp[String]](
        HttpClientProcessor.post(
          s"http://127.0.0.1:8080/public/register/",
          account)))

  }

  // Step 2 Active Account
  test("User Active test") {

    // Replace Real url in your email
    val emailReceivedUrl = "http://127.0.0.1:8080/public/active/account/8a8c14ac-3a9c-41b5-a73e-c393ae744b7192587209118855/"
    assert(
      JsonHelper.toObject[Resp[String]](
        HttpClientProcessor.get(
          // Error url
          s"http://0.0.0.0:8080/public/active/account/74df7c7f-7d7d-47a0-bfc9-2b6df1c586cd12325753090669/ ")
      ).code == StandardCode.NOT_FOUND)

    val result = HttpClientProcessor.get(
      // Real url
      emailReceivedUrl)
    println(result)

  }

  // Step 3 Modify Account
  test("User Get or Update test") {

    val token = AuthService.doLogin("u1", "123", "").body.token
    val accountVO = JsonHelper.toObject[Resp[Account_VO]](
      HttpClientProcessor.get(
        s"http://0.0.0.0:8080/auth/manage/account/bylogin/?__ez_token__=$token")).body
    assert(accountVO.login_id == "u1")
    accountVO.name = "u2"
    accountVO.current_password = "111"
    assert(JsonHelper.toObject[Resp[String]](
      HttpClientProcessor.put(
        s"http://0.0.0.0:8080/auth/manage/account/bylogin/?__ez_token__=$token",
        accountVO)).message == "Old Password Error")
    accountVO.current_password = "123"
    accountVO.new_password = "111"
    assert(JsonHelper.toObject[Resp[String]](
      HttpClientProcessor.put(
        s"http://0.0.0.0:8080/auth/manage/account/bylogin/?__ez_token__=$token",
        accountVO)))

  }

  // Step 4 Find Password
  test("User Find Password test") {

    JsonHelper.toObject[Resp[Void]](
      HttpClientProcessor.put(
        s"http://0.0.0.0:8080/public/findpassword/net@sunisle.org/", Map("newPassword" -> "abc"))).body
  }

  // Step 4 Active New Password
  test("User Active New Password test") {

    // Replace Real url in your email
    val emailReceivedUrl = "http://127.0.0.1:8080/public/active/password/ebb859ec-c852-4b6d-81a7-b5ea67bad7b192644402530626/"

    assert(JsonHelper.toObject[Resp[String]](
      HttpClientProcessor.get(
        s"http://0.0.0.0:8080/public/active/password/83a10fc0-9fbe-4da4-beb0-6e90b9adfc0814147819669595/")
    ).code == StandardCode.NOT_FOUND)
    val result = HttpClientProcessor.get(emailReceivedUrl)
    println(result)
    assert(AuthService.doLogin("u1", "abc", ""))

  }

}


