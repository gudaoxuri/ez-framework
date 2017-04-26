package com.ecfront.ez.framework.test

import com.ecfront.ez.framework.core.EZManager

/**
  * Mock EZ服务启动测试类，服务测试多继承此类
  */
trait MockStartupSpec extends BasicSpec {

  def before():Unit={
    EZManager.start()
  }

  before {
    before()
  }

  test("Mock Start & shutdown flow") {

  }
}

