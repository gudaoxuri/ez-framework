package com.ecfront.ez.framework.service.email

import com.ecfront.ez.framework.core.test.MockStartupSpec

class EmailSpec extends MockStartupSpec {

  test("Email Test") {
    val sendResp = EmailProcessor.send("hi-sb@ecfront.com", List("i@sunisle.org", "364341806@qq.com"), "test 1", "<h1>h1</h1><br/>1\r\n2\r\n")
    assert(sendResp)
  }
}


