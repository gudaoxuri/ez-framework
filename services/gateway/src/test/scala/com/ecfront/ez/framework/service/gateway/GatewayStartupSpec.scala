package com.ecfront.ez.framework.service.gateway

import java.util.concurrent.CountDownLatch

import com.ecfront.ez.framework.test.MockStartupSpec

class GatewayStartupSpec extends MockStartupSpec {

  test("gateway startup test") {
    new CountDownLatch(1).await()
  }

}


