package com.ecfront.ez.framework.service.auth

import com.ecfront.common.{JsonHelper, Resp, StandardCode}
import com.ecfront.ez.framework.core.test.MockStartupSpec
import com.ecfront.ez.framework.service.auth.model.EZ_Account
import com.ecfront.ez.framework.service.rpc.http.HttpClientProcessor

class UserSpec extends MockStartupSpec {

  // Step 1 Register
  test("User Register test") {

    EZ_Account.deleteByEmail("net@sunisle.org")

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

  // Step 2 Active Account
  test("User Active test") {

    // Replace Real url in your email
    val emailReceivedUrl = "http://127.0.0.1:8080/public/active/account/net@sunisle.org/7a23106f-6288-45d5-9897-a9ddfafff08d243804632434419/"
    assert(
      JsonHelper.toObject[Resp[String]](
        HttpClientProcessor.get(
          // Error url
          s"http://0.0.0.0:8080/public/active/account/i@sunisle.org/74df7c7f-7d7d-47a0-bfc9-2b6df1c586cd12325753090669/ ")
      ).code == StandardCode.NOT_FOUND)

    val result = HttpClientProcessor.get(
      // Real url
      emailReceivedUrl)
    println(result)

  }

  // Step 3 Modify Account
  test("User Get Or Update test") {

    val token = AuthService.doLogin("u1", "123").body.token
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
    val emailReceivedUrl = "http://127.0.0.1:8080/public/active/password/net@sunisle.org/1d7b6c29-b465-44a0-bc3f-aa3d0d82487a244006759536524/"

    assert(JsonHelper.toObject[Resp[String]](
      HttpClientProcessor.get(
        s"http://0.0.0.0:8080/public/active/password/i@sunisle.org/83a10fc0-9fbe-4da4-beb0-6e90b9adfc0814147819669595/")
    ).code == StandardCode.NOT_FOUND)
    val result = HttpClientProcessor.get(emailReceivedUrl)
    println(result)
    assert(AuthService.doLogin("u1", "abc"))

  }

}


