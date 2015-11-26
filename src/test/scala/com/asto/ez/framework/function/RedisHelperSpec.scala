package com.asto.ez.framework.function

import java.util.concurrent.CountDownLatch

import com.asto.ez.framework.BasicSpec
import com.asto.ez.framework.helper.RedisHelper
import com.ecfront.common.JsonHelper
import io.vertx.core.Vertx

import scala.async.Async.{async, await}
import scala.concurrent.ExecutionContext.Implicits.global

class RedisHelperSpec extends BasicSpec {

  test("RedisHelper Test") {
    val cdl = new CountDownLatch(1)

    RedisHelper.init(Vertx.vertx(), "192.168.4.99", 6379, 0)

    RedisHelper.exists("dop_test").onSuccess {
      case exists1Resp =>
        assert(!exists1Resp.body)
        RedisHelper.get("dop_test").onSuccess {
          case get1Resp =>
            assert(get1Resp.body == null)
            RedisHelper.set("dop_test", s"""{"name":"jzy"}""", 10).onSuccess {
              case setResp =>
                RedisHelper.exists("dop_test").onSuccess {
                  case exists2Resp =>
                    assert(exists2Resp.body)
                    RedisHelper.get("dop_test").onSuccess {
                      case get2Resp =>
                        assert(JsonHelper.toJson(get2Resp.body).get("name").asText() == "jzy")
                        Thread.sleep(10000)
                        RedisHelper.exists("dop_test").onSuccess {
                          case exists3Resp =>
                            assert(!exists3Resp.body)
                            RedisHelper.get("dop_test").onSuccess {
                              case get3Resp =>
                                assert(get3Resp.body == null)
                                cdl.countDown()
                            }
                        }
                    }
                }
            }
        }
    }

    cdl.await()
  }

  test("RedisHelper Async Test") {
    RedisHelper.init(Vertx.vertx(), "192.168.4.99", 6379, 0)
    val cdl = new CountDownLatch(1)
    testRedisHelperAsync().onSuccess {
      case resp =>
        cdl.countDown()
    }
    cdl.await()
  }

  def testRedisHelperAsync() = async {
    val exists1Resp = await(RedisHelper.exists("dop_test"))
    assert(!exists1Resp.body)
    await(RedisHelper.set("dop_test", s"""{"name":"jzy"}""", 10))
    val exists2Resp = await(RedisHelper.exists("dop_test"))
    assert(exists2Resp.body)
    val get2Resp = await(RedisHelper.get("dop_test"))
    assert(JsonHelper.toJson(get2Resp.body).get("name").asText() == "jzy")
    Thread.sleep(10000)
    val exists3Resp = await(RedisHelper.exists("dop_test"))
    assert(!exists3Resp.body)
    val get3Resp = await(RedisHelper.get("dop_test"))
    assert(get3Resp.body == null)
  }


}


