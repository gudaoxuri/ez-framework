package com.ecfront.ez.framework.service.auth

import com.ecfront.common.{JsonHelper, Resp, StandardCode}
import com.ecfront.ez.framework.core.test.MockStartupSpec
import com.ecfront.ez.framework.service.auth.model.EZ_Account
import com.ecfront.ez.framework.service.rpc.http.RespHttpClientProcessor

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future, Promise}

class RPCAuthSpec extends MockStartupSpec {

  test("RPC auth test") {

    assert(
      RespHttpClientProcessor.get[Void]("http://127.0.0.1:8080/auth/logout/?__ez_token__=1122222").code == StandardCode.UNAUTHORIZED)
    var loginInfoResp =
      RespHttpClientProcessor.post[Token_Info_VO]("http://127.0.0.1:8080/public/auth/login/", Map("id" -> "admin1", "password" -> "admin"))
    assert(!loginInfoResp)
    loginInfoResp =
      RespHttpClientProcessor.post[Token_Info_VO]("http://127.0.0.1:8080/public/auth/login/", Map("id" -> "sysadmin", "password" -> "admin"))
    assert(loginInfoResp && loginInfoResp.body.name == "Sys Admin")
    var token = loginInfoResp.body.token
    assert(RespHttpClientProcessor.get[Void](s"http://127.0.0.1:8080/auth/logout/?__ez_token__=$token"))
    loginInfoResp =
      RespHttpClientProcessor.post[Token_Info_VO]("http://127.0.0.1:8080/public/auth/login/", Map("id" -> "sysadmin", "password" -> "admin"))
    assert(loginInfoResp.body.token != token)
    token = loginInfoResp.body.token
    var account = EZ_Account.getByLoginId(loginInfoResp.body.login_id, "").body
    account =
      RespHttpClientProcessor.get[EZ_Account](s"http://127.0.0.1:8080/auth/manage/account/${account.id}/?__ez_token__=$token").body
    assert(account.login_id == "sysadmin")
    loginInfoResp =
      RespHttpClientProcessor.post[Token_Info_VO]("http://127.0.0.1:8080/public/auth/login/", Map("id" -> "sysadmin", "password" -> "admin"))
    assert(
      RespHttpClientProcessor.get[EZ_Account](s"http://127.0.0.1:8080/auth/manage/account/${account.id}/?__ez_token__=$token")
        .code == StandardCode.UNAUTHORIZED)
    assert(
      RespHttpClientProcessor.get[EZ_Account](s"http://127.0.0.1:8080/auth/manage/account/${account.id}/?__ez_token__=${loginInfoResp.body.token}"))
    token = loginInfoResp.body.token

    RespHttpClientProcessor.put[EZ_Account](
      s"http://127.0.0.1:8080/auth/manage/account/${account.id}/?__ez_token__=$token",
      Map("name" -> "system")).body
    RespHttpClientProcessor.put[EZ_Account](
      s"http://127.0.0.1:8080/auth/manage/account/${account.id}/?__ez_token__=$token",
      Map("password" -> "123", "ext_info" -> Map("ext1" -> "aaaa"))).body
    loginInfoResp =
      RespHttpClientProcessor.post[Token_Info_VO]("http://127.0.0.1:8080/public/auth/login/", Map("id" -> "sysadmin", "password" -> "123"))
    assert(loginInfoResp.body.name == "system")
    token = loginInfoResp.body.token
    RespHttpClientProcessor.put[EZ_Account](
      s"http://127.0.0.1:8080/auth/manage/account/${account.id}/?__ez_token__=$token",
      Map("name" -> "Sys Admin", "password" -> "admin")).body
    loginInfoResp =
      RespHttpClientProcessor.post[Token_Info_VO]("http://127.0.0.1:8080/public/auth/login/", Map("id" -> "sysadmin", "password" -> "admin"))
    assert(loginInfoResp.body.name == "Sys Admin" && loginInfoResp.body.ext_info("ext1") == "aaaa")
    token = loginInfoResp.body.token
    RespHttpClientProcessor.get[Void](
      s"http://127.0.0.1:8080/auth/manage/account/${account.id}/disable/?__ez_token__=$token")
    assert(!EZ_Account.getByLoginId("sysadmin","").body.enable)
    RespHttpClientProcessor.get[Void](
      s"http://127.0.0.1:8080/auth/manage/account/${account.id}/enable/?__ez_token__=$token")
    assert(EZ_Account.getByLoginId("sysadmin","").body.enable)
  }

  test("Json Parse Test") {
    val respF = p[Account_VO](
      s"""
         |{"id":"1","login_id":"sysadmin","name":"Sys Admin","image":"","email":"i@sunisle.org","current_password":null,"new_password":null,"organization_code":null,"ext_id":"1","ext_info":{"department":"","phone":"","in_service":true,"id":"1"}}
       """.stripMargin)
    val resp = Await.result(respF, Duration.Inf)
    println(resp)
  }

  def p[E: Manifest](str: String): Future[Resp[E]] = {
    val p = Promise[Resp[E]]()
    p.success(Resp.success(JsonHelper.toObject[E](str)))
    p.future
  }

}


