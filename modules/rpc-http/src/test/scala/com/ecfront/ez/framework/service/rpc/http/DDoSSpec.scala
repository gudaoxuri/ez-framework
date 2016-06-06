package com.ecfront.ez.framework.service.rpc.http

import com.ecfront.common.{JsonHelper, Resp, StandardCode}
import com.ecfront.ez.framework.core.test.MockStartupSpec
import com.ecfront.ez.framework.service.rpc.http.interceptor.AntiDDoSInterceptor
import com.ecfront.ez.framework.service.rpc.http.test.EZ_Resource

class DDoSSpec extends MockStartupSpec {

  test("DDoS test") {

    AntiDDoSInterceptor.init(5L, 1000L)

    assert(JsonHelper.toObject[Resp[EZ_Resource]](HttpClientProcessor.get("http://127.0.0.1:8080/resource/1/")).body == null)
    assert(JsonHelper.toObject[Resp[EZ_Resource]](HttpClientProcessor.get("http://127.0.0.1:8080/resource/1/")).body == null)
    assert(JsonHelper.toObject[Resp[EZ_Resource]](HttpClientProcessor.get("http://127.0.0.1:8080/resource/1/")).body == null)
    assert(JsonHelper.toObject[Resp[EZ_Resource]](HttpClientProcessor.get("http://127.0.0.1:8080/resource/1/")).body == null)
    assert(JsonHelper.toObject[Resp[EZ_Resource]](HttpClientProcessor.get("http://127.0.0.1:8080/resource/1/")).code == StandardCode.LOCKED)
    Thread.sleep(30000)
    assert(JsonHelper.toObject[Resp[EZ_Resource]](HttpClientProcessor.get("http://127.0.0.1:8080/resource/1/")).code == StandardCode.LOCKED)
    Thread.sleep(30000)
    assert(JsonHelper.toObject[Resp[EZ_Resource]](HttpClientProcessor.get("http://127.0.0.1:8080/resource/1/")).body == null)

    AntiDDoSInterceptor.init(500000L, 5L)
    assert(JsonHelper.toObject[Resp[EZ_Resource]](HttpClientProcessor.get("http://127.0.0.1:8080/bad/")).code==StandardCode.NOT_IMPLEMENTED)
    assert(JsonHelper.toObject[Resp[EZ_Resource]](HttpClientProcessor.get("http://127.0.0.1:8080/bad/")).code==StandardCode.NOT_IMPLEMENTED)
    assert(JsonHelper.toObject[Resp[EZ_Resource]](HttpClientProcessor.get("http://127.0.0.1:8080/bad/")).code==StandardCode.NOT_IMPLEMENTED)
    assert(JsonHelper.toObject[Resp[EZ_Resource]](HttpClientProcessor.get("http://127.0.0.1:8080/bad/")).code==StandardCode.NOT_IMPLEMENTED)
    assert(JsonHelper.toObject[Resp[EZ_Resource]](HttpClientProcessor.get("http://127.0.0.1:8080/bad/")).code==StandardCode.NOT_IMPLEMENTED)
    assert(JsonHelper.toObject[Resp[EZ_Resource]](HttpClientProcessor.get("http://127.0.0.1:8080/bad/")).code==StandardCode.LOCKED)
    Thread.sleep(30000)
    assert(JsonHelper.toObject[Resp[EZ_Resource]](HttpClientProcessor.get("http://127.0.0.1:8080/bad/")).code==StandardCode.LOCKED)
    Thread.sleep(30000)
    assert(JsonHelper.toObject[Resp[EZ_Resource]](HttpClientProcessor.get("http://127.0.0.1:8080/bad/")).code==StandardCode.NOT_IMPLEMENTED)

  }

}


