package com.asto.ez.framework.function

import java.util.concurrent.CountDownLatch

import com.asto.ez.framework.helper.HttpClientHelper
import com.asto.ez.framework.{BasicSpec, EZGlobal}
import com.fasterxml.jackson.databind.JsonNode
import scala.async.Async.{async, await}
import scala.concurrent.ExecutionContext.Implicits.global
class HttpClientSpec extends BasicSpec {

  override def before2(): Any = {
    HttpClientHelper.httpClient = EZGlobal.vertx.createHttpClient()
  }

  test("HttpClient Test")  {
    val cdl= new CountDownLatch(1)
    testHttpClient().onSuccess{
      case resp =>
        cdl.countDown()
    }
    cdl.await()
  }

  def testHttpClient() = async {

    val result = await(HttpClientHelper.get("http://doptest.yuanbaopu.com/query/realtime/summary/?time=today&index=pv",classOf[JsonNode])).body
    println(result)
    result

  }

}
