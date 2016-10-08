package com.ecfront.ez.framework.gateway

import java.util.concurrent.CountDownLatch

import com.ecfront.common.StandardCode
import com.ecfront.ez.framework.core.rpc.{HttpClientProcessor, RespHttpClientProcessor}
import com.ecfront.ez.framework.core.test.MockStartupSpec
import com.ecfront.ez.framework.service.jdbc.Page
import org.joox.JOOX._

class HttpServerSpec extends MockStartupSpec {

  test("Http test") {

    assert(RespHttpClientProcessor.get[EZ_Resource]("http://127.0.0.1:8080/resource/1/").body == null)
    assert(RespHttpClientProcessor.get[List[EZ_Resource]]("http://127.0.0.1:8080/resource").body.isEmpty)

    val res = EZ_Resource()
    assert(RespHttpClientProcessor.post[EZ_Resource]("http://127.0.0.1:8080/resource/", res).code == StandardCode.BAD_REQUEST)

    assert(RespHttpClientProcessor.post[EZ_Resource]("http://127.0.0.1:8080/resource", EZ_Resource("1", "GET", "/ss")).body.id != null)
    assert(RespHttpClientProcessor.post[EZ_Resource]("http://127.0.0.1:8080/resource/", EZ_Resource("1", "GET", "/ss")).code == StandardCode.BAD_REQUEST)

    assert(RespHttpClientProcessor.get[List[EZ_Resource]]("http://127.0.0.1:8080/resource/").body.head.uri == "/ss")

    HttpClientProcessor.post("http://127.0.0.1:8080/resource/", EZ_Resource("2", "GET", "/2s"))
    HttpClientProcessor.post("http://127.0.0.1:8080/resource/", EZ_Resource("3", "GET", "/3s"))
    HttpClientProcessor.post("http://127.0.0.1:8080/resource/", EZ_Resource("4", "GET", "/4s"))
    HttpClientProcessor.post("http://127.0.0.1:8080/resource/", EZ_Resource("5", "GET", "/5s"))

    val pageResult = RespHttpClientProcessor.get[Page[EZ_Resource]]("http://127.0.0.1:8080/resource/page/2/2/").body
    assert(pageResult.pageNumber == 2)
    assert(pageResult.pageSize == 2)
    assert(pageResult.pageTotal == 3)
    assert(pageResult.recordTotal == 5)
    assert(pageResult.objects.size == 2)

    var xmlStr = HttpClientProcessor.get(
      "http://127.0.0.1:8080/resource/xml/str/", "text/xml; charset=utf-8")
    assert($(xmlStr).find("city").size() > 0)
    xmlStr = HttpClientProcessor.get(
      "http://127.0.0.1:8080/resource/xml/", "text/xml; charset=utf-8")
    assert($(xmlStr).find("city").size() > 0)
    xmlStr = HttpClientProcessor.post(
      "http://127.0.0.1:8080/resource/xml/", xmlStr, "text/xml; charset=utf-8")
    assert($(xmlStr).find("city").size() > 0)
    xmlStr = HttpClientProcessor.post(
      "http://127.0.0.1:8080/resource/xml/", $(xmlStr).document(), "text/xml; charset=utf-8")
    assert($(xmlStr).find("city").size() > 0)
    xmlStr = HttpClientProcessor.post(
      "http://127.0.0.1:8080/resource/xml/str/", $(xmlStr).document(), "text/xml; charset=utf-8")
    assert($(xmlStr).find("city").size() > 0)
    xmlStr = HttpClientProcessor.post(
      "http://127.0.0.1:8080/resource/xml/str/", $(xmlStr).document(), "text/xml; charset=utf-8")
    assert($(xmlStr).find("city").size() > 0)
    xmlStr = HttpClientProcessor.post(
      "http://127.0.0.1:8080/resource/xml/str/error/", $(xmlStr).document(), "text/xml; charset=utf-8")
    assert($(xmlStr).find("error").size() > 0)
    xmlStr = HttpClientProcessor.get(
      "http://flash.weather.com.cn:80/wmaps/xml/china.xml", "text/xml; charset=utf-8")
    assert($(xmlStr).find("city").size() > 0)
  }

  test("https test") {

    HttpClientProcessor.post("https://127.0.0.1:8080/resource/", EZ_Resource("2", "GET", "/2s"))
    HttpClientProcessor.post("https://127.0.0.1:8080/resource/", EZ_Resource("3", "GET", "/3s"))
    HttpClientProcessor.post("https://127.0.0.1:8080/resource/", EZ_Resource("4", "GET", "/4s"))
    HttpClientProcessor.post("https://127.0.0.1:8080/resource/", EZ_Resource("5", "GET", "/5s"))

    val pageResult = RespHttpClientProcessor.get[Page[EZ_Resource]]("https://127.0.0.1:8080/resource/page/2/2/").body
    assert(pageResult.pageNumber == 2)
    assert(pageResult.pageSize == 2)
    assert(pageResult.pageTotal == 2)
    assert(pageResult.recordTotal == 4)
    assert(pageResult.objects.size == 2)
  }

  test("Http1 test") {
    new CountDownLatch(1).await()
  }

}


