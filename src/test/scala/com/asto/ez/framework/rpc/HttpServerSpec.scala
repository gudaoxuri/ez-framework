package com.asto.ez.framework.rpc

import java.util.concurrent.CountDownLatch

import com.asto.ez.framework.MockStartupSpec

class HttpServerSpec extends MockStartupSpec {

  test("Upload test") {
    new CountDownLatch(1).await()
  }

}


