package com.ecfront.ez.framework.service.email

import com.ecfront.ez.framework.core.EZContext
import com.ecfront.ez.framework.core.helper.EFileType
import com.ecfront.ez.framework.core.test.MockStartupSpec

class EmailSpec extends MockStartupSpec {


  test("Email Test") {
    val txt = EZContext.vertx.fileSystem().readFileBlocking(
      this.getClass.getResource("/").getPath + "ez.json")

    val sendResp = EmailProcessor.send(
      "hi-sb@ecfront.com",
      List("i@sunisle.org", "364341806@qq.com"),
      null, null,
      "test 1",
      "<h1>h1</h1><br/>1\r\n2\r\n",
      List(("attach.txt", EFileType.TXT.toString, txt))
    )
    assert(sendResp)
  }
}


