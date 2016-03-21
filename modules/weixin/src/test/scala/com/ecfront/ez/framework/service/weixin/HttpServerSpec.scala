package com.ecfront.ez.framework.service.oauth2

import java.util.concurrent.CountDownLatch

import com.ecfront.ez.framework.core.test.MockStartupSpec

class HttpServerSpec extends MockStartupSpec {

  test("OAuth2 test") {

    new CountDownLatch(1).await()

  }

}


