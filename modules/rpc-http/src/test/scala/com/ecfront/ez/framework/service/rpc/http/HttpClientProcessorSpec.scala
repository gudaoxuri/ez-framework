package com.ecfront.ez.framework.service.rpc.http

import com.ecfront.ez.framework.core.test.MockStartupSpec

class HttpClientProcessorSpec extends MockStartupSpec {

  test("HttpClient Test") {
    val result = HttpClientProcessor.get("http://dop.yuanbaopu.com/query/realtime/summary/?time=today&index=pv")
    println(result)
  }

}
