package com.asto.ez.framework.auth

import com.asto.ez.framework.MockStartupSpec
import com.asto.ez.framework.rpc.http.HttpClientProcessor
import com.ecfront.common.StandardCode

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class RPCAuthSpec extends MockStartupSpec {

  test("RPC auth test") {

    Thread.sleep(8000)
    assert(Await.result(HttpClientProcessor.get("http://127.0.0.1:8080/auth/logout/?__ez_token__=1122222", classOf[Void]), Duration.Inf).code == StandardCode.UNAUTHORIZED)
    var loginInfoResp = Await.result(HttpClientProcessor.post("http://127.0.0.1:8080/public/auth/login/", Map("login_id" -> "admin", "password" -> "admin"), classOf[Token_Info_VO]), Duration.Inf)
    assert(!loginInfoResp)
    loginInfoResp = Await.result(HttpClientProcessor.post("http://127.0.0.1:8080/public/auth/login/", Map("login_id" -> "sysadmin", "password" -> "admin"), classOf[Token_Info_VO]), Duration.Inf)
    assert(loginInfoResp && loginInfoResp.body.login_name == "System Administrator")
    var token = loginInfoResp.body.token
    assert(Await.result(HttpClientProcessor.get(s"http://127.0.0.1:8080/auth/logout/?__ez_token__=$token", classOf[Void]), Duration.Inf))
    loginInfoResp = Await.result(HttpClientProcessor.post("http://127.0.0.1:8080/public/auth/login/", Map("login_id" -> "sysadmin", "password" -> "admin"), classOf[Token_Info_VO]), Duration.Inf)
    assert(loginInfoResp.body.token != token)
    token = loginInfoResp.body.token
    var account: EZ_Account = Await.result(EZ_Account.getByLoginId(loginInfoResp.body.login_id), Duration.Inf).body
    account = Await.result(HttpClientProcessor.get(s"http://127.0.0.1:8080/auth/manage/account/${account.id}/?__ez_token__=$token", classOf[EZ_Account]), Duration.Inf).body
    assert(account.login_id == "sysadmin")
    loginInfoResp = Await.result(HttpClientProcessor.post("http://127.0.0.1:8080/public/auth/login/", Map("login_id" -> "sysadmin", "password" -> "admin"), classOf[Token_Info_VO]), Duration.Inf)
    assert(Await.result(HttpClientProcessor.get(s"http://127.0.0.1:8080/auth/manage/account/${account.id}/?__ez_token__=$token", classOf[EZ_Account]), Duration.Inf).code == StandardCode.UNAUTHORIZED)
    assert(Await.result(HttpClientProcessor.get(s"http://127.0.0.1:8080/auth/manage/account/${account.id}/?__ez_token__=${loginInfoResp.body.token}", classOf[EZ_Account]), Duration.Inf))

  }

}


