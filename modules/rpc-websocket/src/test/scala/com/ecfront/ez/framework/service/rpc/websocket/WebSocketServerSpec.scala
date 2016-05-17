package com.ecfront.ez.framework.service.rpc.websocket

import java.util.concurrent.CountDownLatch

import com.ecfront.ez.framework.core.test.MockStartupSpec

class WebSocketServerSpec extends MockStartupSpec {

  test("WebSocket test") {
    //Thread.sleep(10000)
    //WebSocketMessagePushManager.ws(Method.REQUEST, "/resource/", EZ_Resource("2", "GET", "/2s"))
    new CountDownLatch(1).await()

  }

}


