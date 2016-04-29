package com.ecfront.ez.framework.service.auth

import java.util.concurrent.CountDownLatch

import com.ecfront.ez.framework.core.test.MockStartupSpec

class PerformanceSpec extends MockStartupSpec {

  test("性能测试") {

    new CountDownLatch(1).await()

  }


}
