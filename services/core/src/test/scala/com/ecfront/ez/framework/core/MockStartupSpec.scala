package com.ecfront.ez.framework.core

/**
  * Mock EZ服务启动测试类，服务测试多继承此类
  */
class MockStartupSpec extends BasicSpec {

  before {
    EZManager.start()
  }

  test("Mock Start & shutdown flow") {

  }
}

