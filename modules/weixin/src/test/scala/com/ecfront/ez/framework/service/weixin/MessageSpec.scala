package com.ecfront.ez.framework.service.weixin

import java.util.concurrent.CountDownLatch

import com.ecfront.ez.framework.core.test.MockStartupSpec

class MessageSpec extends MockStartupSpec {

  test("message test") {
    new CountDownLatch(1).await()
  }

}


