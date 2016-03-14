package com.ecfront.ez.framework.service.auth

import com.ecfront.ez.framework.core.test.MockStartupSpec
import com.ecfront.ez.framework.service.rpc.http.HttpClientProcessor

class ExportSpec extends MockStartupSpec {

  test("Export Test") {
    val token = AuthService.doLogin("sysadmin", "admin").body.token
    val result=HttpClientProcessor.get(
      s"http://0.0.0.0:8080/auth/manage/resource/export/?__ez_token__=$token")
    println(result)
  }

}


