package com.ecfront.ez.framework.service.rpc.websocket

import java.util.concurrent.CountDownLatch

import com.ecfront.ez.framework.core.test.MockStartupSpec
import com.ecfront.ez.framework.service.rpc.foundation.Method
import com.ecfront.ez.framework.service.rpc.websocket.test.EZ_Resource

class WebSocketServerSpec extends MockStartupSpec {

  test("WebSocket test") {

    EZ_Resource.deleteByCond("")

    WebSocketProcessor.ws(Method.POST, "/resource/", EZ_Resource("1", "GET", "/1s"))
    Thread.sleep(10000)
    WebSocketProcessor.ws(Method.POST, "/resource/", EZ_Resource("2", "GET", "/2s"))
    new CountDownLatch(1).await()

  }

}


