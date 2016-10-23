package com.ecfront.ez.framework.service.tpsi

import com.ecfront.common.Resp
import com.ecfront.ez.framework.core.EZ
import com.ecfront.ez.framework.test.MockStartupSpec

class TPSISpec extends MockStartupSpec {

  test("tpsi Test") {
    val obj = TPSITestObj("测试", 2.2)
    EZ.eb.request("/tpsi/resp/", obj, Map("id" -> "1"))
    EZ.eb.publish("/tpsi/sub/", obj, Map("id" -> "1"))
    val reply = EZ.eb.ack[Resp[TPSITestObj]]("/tpsi/reply/", obj, Map("id" -> "1"))
    assert(reply._1.body.t == "测试" && reply._1.body.d == 2.2)
    TPSITestService.counter.await()
  }

}

