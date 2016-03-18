package com.ecfront.ez.framework.service.rpc.http

import com.ecfront.common.{JsonHelper, Resp, StandardCode}
import com.ecfront.ez.framework.core.test.MockStartupSpec
import com.ecfront.ez.framework.service.rpc.http.test.EZ_Resource
import com.ecfront.ez.framework.service.storage.foundation.Page

class HttpServerSpec extends MockStartupSpec {

  test("Http test") {

    EZ_Resource.deleteByCond("")

    assert(JsonHelper.toObject[Resp[EZ_Resource]](HttpClientProcessor.get("http://127.0.0.1:8080/resource/1/")).body == null)
    assert(JsonHelper.toObject[Resp[List[EZ_Resource]]](HttpClientProcessor.get("http://127.0.0.1:8080/resource/")).body.isEmpty)

    val res = EZ_Resource()
    assert(JsonHelper.toObject[Resp[EZ_Resource]](HttpClientProcessor.post("http://127.0.0.1:8080/resource/", res)).code == StandardCode.BAD_REQUEST)

    assert(JsonHelper.toObject[Resp[EZ_Resource]](HttpClientProcessor.post("http://127.0.0.1:8080/resource/", EZ_Resource("1", "GET", "/ss"))).body.id != null)
    assert(JsonHelper.toObject[Resp[EZ_Resource]](
      HttpClientProcessor.post("http://127.0.0.1:8080/resource/", EZ_Resource("1", "GET", "/ss"))).code == StandardCode.BAD_REQUEST)


    assert(JsonHelper.toObject[Resp[List[EZ_Resource]]](HttpClientProcessor.get(
      "http://127.0.0.1:8080/resource/")).body.head.uri == "/ss")

    HttpClientProcessor.post("http://127.0.0.1:8080/resource/", EZ_Resource("2", "GET", "/2s"))
    HttpClientProcessor.post("http://127.0.0.1:8080/resource/", EZ_Resource("3", "GET", "/3s"))
    HttpClientProcessor.post("http://127.0.0.1:8080/resource/", EZ_Resource("4", "GET", "/4s"))
    HttpClientProcessor.post("http://127.0.0.1:8080/resource/", EZ_Resource("5", "GET", "/5s"))

    val pageResult = JsonHelper.toObject[Resp[Page[EZ_Resource]]](HttpClientProcessor.get(
      "http://127.0.0.1:8080/resource/page/2/2/")).body
    assert(pageResult.pageNumber == 2)
    assert(pageResult.pageSize == 2)
    assert(pageResult.pageTotal == 3)
    assert(pageResult.recordTotal == 5)
    assert(pageResult.objects.size == 2)

  }

  test("https test") {

    EZ_Resource.deleteByCond("")
    HttpClientProcessor.post("https://127.0.0.1:8080/resource/", EZ_Resource("2", "GET", "/2s"))
    HttpClientProcessor.post("https://127.0.0.1:8080/resource/", EZ_Resource("3", "GET", "/3s"))
    HttpClientProcessor.post("https://127.0.0.1:8080/resource/", EZ_Resource("4", "GET", "/4s"))
    HttpClientProcessor.post("https://127.0.0.1:8080/resource/", EZ_Resource("5", "GET", "/5s"))

    val pageResult = JsonHelper.toObject[Resp[Page[EZ_Resource]]](HttpClientProcessor.get(
      "https://127.0.0.1:8080/resource/page/2/2/")).body
    assert(pageResult.pageNumber == 2)
    assert(pageResult.pageSize == 2)
    assert(pageResult.pageTotal == 2)
    assert(pageResult.recordTotal == 4)
    assert(pageResult.objects.size == 2)
  }

}


