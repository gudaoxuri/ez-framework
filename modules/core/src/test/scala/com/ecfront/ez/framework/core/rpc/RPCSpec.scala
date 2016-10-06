package com.ecfront.ez.framework.core.rpc

import java.util.concurrent.CountDownLatch

import com.ecfront.common.{Resp, StandardCode}
import com.ecfront.ez.framework.core.EZ
import com.ecfront.ez.framework.core.test.MockStartupSpec

class RPCSpec extends MockStartupSpec {

  test("RPC Test") {
    assert(EZ.eb.ack[Resp[Void]]("HTTP@GET@/test1/", null))
    assert(EZ.eb.ack[Resp[String]]("HTTP@POST@/test1/post/", "aaa").body == "aaa")
    assert(EZ.eb.ack[Resp[BodyTest]]("HTTP@PUT@/put/", BodyTest(null)).code == StandardCode.BAD_REQUEST)
    assert(EZ.eb.ack[Resp[BodyTest]]("HTTP@PUT@/put/", BodyTest("测试")).body.a == "测试")
    assert(EZ.eb.ack[Resp[String]]("HTTP@DELETE@/test1/:id/", null, Map("id" -> "11")).body == "11")
    assert(!EZ.eb.ack[Resp[Boolean]]("WS@WS@/ws/:id/:id2/", Map("a" -> false), Map("id" -> "11", "id2" -> "222")).body)

    EZ.eb.publish("/test2/", "sub")
    EZ.eb.request("/test2/resp/", BodyTest("resp"))
    assert(EZ.eb.ack[Resp[BodyTest]]("/reply/", BodyTest("ack")).body.a == "ack")

    new CountDownLatch(1).await()
  }

}


