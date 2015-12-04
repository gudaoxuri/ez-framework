package com.asto.ez.framework.rpc

import java.util.concurrent.CountDownLatch

import com.asto.ez.framework.rpc.http.HttpClientProcessor
import com.asto.ez.framework.{BasicSpec, EZGlobal}
import com.fasterxml.jackson.databind.JsonNode

import scala.async.Async.{async, await}
import scala.concurrent.ExecutionContext.Implicits.global
class HttpClientProcessorSpec extends BasicSpec {

  override def before2(): Any = {
    HttpClientProcessor.httpClient = EZGlobal.vertx.createHttpClient()
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

    val result = await(HttpClientProcessor.get("http://dop.yuanbaopu.com/query/realtime/summary/?time=today&index=pv",classOf[JsonNode])).body
    println(result)
    result

  }

}
