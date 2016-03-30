package com.ecfront.ez.framework.service.auth

import com.ecfront.common.{JsonHelper, Resp, StandardCode}
import com.ecfront.ez.framework.core.test.MockStartupSpec
import com.ecfront.ez.framework.service.auth.model.EZ_Account
import com.ecfront.ez.framework.service.rpc.http.HttpClientProcessor

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future, Promise}

class RPCAuthSpec extends MockStartupSpec {

  test("RPC auth test") {

    assert(
      JsonHelper.toObject[Resp[Void]](
        HttpClientProcessor.get("http://127.0.0.1:8080/auth/logout/?__ez_token__=1122222")).code == StandardCode.UNAUTHORIZED)
    var loginInfoResp = JsonHelper.toObject[Resp[Token_Info_VO]](
      HttpClientProcessor.post("http://127.0.0.1:8080/public/auth/login/", Map("id" -> "admin", "password" -> "admin")))
    assert(!loginInfoResp)
    loginInfoResp = JsonHelper.toObject[Resp[Token_Info_VO]](
      HttpClientProcessor.post("http://127.0.0.1:8080/public/auth/login/", Map("id" -> "sysadmin", "password" -> "admin")))
    assert(loginInfoResp && loginInfoResp.body.name == "Sys Admin")
    var token = loginInfoResp.body.token
    assert(JsonHelper.toObject[Resp[Void]](HttpClientProcessor.get(s"http://127.0.0.1:8080/auth/logout/?__ez_token__=$token")))
    loginInfoResp = JsonHelper.toObject[Resp[Token_Info_VO]](
      HttpClientProcessor.post("http://127.0.0.1:8080/public/auth/login/", Map("id" -> "sysadmin", "password" -> "admin")))
    assert(loginInfoResp.body.token != token)
    token = loginInfoResp.body.token
    var account = EZ_Account.getByLoginId(loginInfoResp.body.login_id, "").body
    account = JsonHelper.toObject[Resp[EZ_Account]](
      HttpClientProcessor.get(s"http://127.0.0.1:8080/auth/manage/account/${account.id}/?__ez_token__=$token")).body
    assert(account.login_id == "sysadmin")
    loginInfoResp = JsonHelper.toObject[Resp[Token_Info_VO]](
      HttpClientProcessor.post("http://127.0.0.1:8080/public/auth/login/", Map("id" -> "sysadmin", "password" -> "admin")))
    assert(JsonHelper.toObject[Resp[EZ_Account]](
      HttpClientProcessor.get(s"http://127.0.0.1:8080/auth/manage/account/${account.id}/?__ez_token__=$token")).code == StandardCode.UNAUTHORIZED)
    assert(JsonHelper.toObject[Resp[EZ_Account]](
      HttpClientProcessor.get(s"http://127.0.0.1:8080/auth/manage/account/${account.id}/?__ez_token__=${loginInfoResp.body.token}")))

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


