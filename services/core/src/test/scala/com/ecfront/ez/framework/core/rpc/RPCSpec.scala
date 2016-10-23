package com.ecfront.ez.framework.core.rpc

import com.ecfront.common.{Resp, StandardCode}
import com.ecfront.ez.framework.core.{EZ, MockStartupSpec}

class RPCSpec extends MockStartupSpec {

  test("RPC Test") {
    assert(EZ.eb.ack[Resp[Void]]("GET@/test1/", null)._1)
    assert(EZ.eb.ack[Resp[String]]("POST@/test1/post/", "aaa")._1.body == "aaa")
    assert(EZ.eb.ack[Resp[BodyTest]]("PUT@/put/", BodyTest(null))._1.code == StandardCode.BAD_REQUEST)
    assert(EZ.eb.ack[Resp[BodyTest]]("PUT@/put/", BodyTest("测试"))._1.body.a == "测试")
    assert(EZ.eb.ack[Resp[String]]("DELETE@/test1/:id/", null, Map("id" -> "11"))._1.body == "11")
    assert(!EZ.eb.ack[Resp[Boolean]]("WS@/ws/:id/:id2/", Map("a" -> false), Map("id" -> "11", "id2" -> "222"))._1.body)

    EZ.eb.publish("/test2/", "sub")
    EZ.eb.request("/test2/resp/", BodyTest("resp"))
    assert(EZ.eb.ack[Resp[BodyTest]]("/reply/", BodyTest("ack"))._1.body.a == "ack")
  }

}


