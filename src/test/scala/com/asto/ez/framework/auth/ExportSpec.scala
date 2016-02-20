package com.asto.ez.framework.auth

import com.asto.ez.framework.MockStartupSpec
import com.asto.ez.framework.rpc.http.HttpClientProcessor
import com.ecfront.common.{AsyncResp, Resp}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Promise}
import scala.reflect.io.File

class ExportSpec extends MockStartupSpec {

  test("Export Test") {

    Thread.sleep(4000)

    val loginP = Promise[Resp[Token_Info_VO]]()
    AuthService.doLogin("sysadmin", "admin", AsyncResp(loginP))
    val token = Await.result(loginP.future, Duration.Inf).body.token
    val file = Await.result(HttpClientProcessor.get(
      s"http://0.0.0.0:8080/auth/manage/resource/export/?__ez_token__=$token",
      classOf[File]), Duration.Inf).body

  }

}


