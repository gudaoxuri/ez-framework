package com.asto.ez.framework.function

import java.util.concurrent.CountDownLatch

import com.asto.ez.framework.BasicSpec
import com.asto.ez.framework.helper.RedisHelper
import com.ecfront.common.JsonHelper
import io.vertx.core.Vertx

import scala.async.Async.{async, await}
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

class RedisHelperSpec extends BasicSpec {

  test("RedisHelper Test") {
    val cdl = new CountDownLatch(1)

    RedisHelper.init(Vertx.vertx(), "192.168.4.99", 6379, 0)

    Await.result(RedisHelper.del("dop_test"), Duration.Inf)
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

    Await.result(RedisHelper.del("list_test"), Duration.Inf)
    Await.result(RedisHelper.lmset("list_test", List("v1", "v2")), Duration.Inf)
    Await.result(RedisHelper.lpush("list_test", "v3"), Duration.Inf)
    Await.result(RedisHelper.lset("list_test", "v3_new", 0), Duration.Inf)
    assert(Await.result(RedisHelper.llen("list_test"), Duration.Inf).body == 3)
    assert(Await.result(RedisHelper.lpop("list_test"), Duration.Inf).body == "v3_new")
    assert(Await.result(RedisHelper.llen("list_test"), Duration.Inf).body == 2)
    assert(Await.result(RedisHelper.lindex("list_test", 1), Duration.Inf).body == "v1")
    val listVals = Await.result(RedisHelper.lget("list_test"), Duration.Inf).body
    assert(listVals.size == 2 && listVals == List("v2", "v1"))

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
    await(RedisHelper.del("n_test"))
    val exists1Resp = await(RedisHelper.exists("n_test"))
    assert(!exists1Resp.body)
    await(RedisHelper.set("n_test", s"""{"name":"jzy"}""", 4))
    val exists2Resp = await(RedisHelper.exists("n_test"))
    assert(exists2Resp.body)
    val get2Resp = await(RedisHelper.get("n_test"))
    assert(JsonHelper.toJson(get2Resp.body).get("name").asText() == "jzy")
    Thread.sleep(4000)
    val exists3Resp = await(RedisHelper.exists("n_test"))
    assert(!exists3Resp.body)
    val get3Resp = await(RedisHelper.get("n_test"))
    assert(get3Resp.body == null)

    await(RedisHelper.del("hash_test"))
    await(RedisHelper.hmset("hash_test", Map("f1" -> "v1", "f2" -> "v2")))
    await(RedisHelper.hset("hash_test", "f3", "v3"))
    assert(await(RedisHelper.hget("hash_test", "f3")).body == "v3")
    assert(await(RedisHelper.hexists("hash_test", "f3")).body)
    val hashVals = await(RedisHelper.hgetall("hash_test")).body
    assert(hashVals.size == 3 && hashVals("f1") == "v1" && hashVals("f2") == "v2" && hashVals("f3") == "v3")
    await(RedisHelper.hdel("hash_test", "f3"))
    assert(!await(RedisHelper.hexists("hash_test", "f3")).body)
    await(RedisHelper.del("hash_test"))
    assert(!await(RedisHelper.exists("hash_test")).body)

    await(RedisHelper.del("list_test"))
    await(RedisHelper.lmset("list_test", List("v1", "v2")))
    await(RedisHelper.lpush("list_test", "v3"))
    await(RedisHelper.lset("list_test", "v3_new", 2))
    assert(await(RedisHelper.llen("list_test")).body == 3)
    assert(await(RedisHelper.lpop("list_test")).body == "v1")
    assert(await(RedisHelper.llen("list_test")).body == 2)
    assert(await(RedisHelper.lindex("list_test", 1)).body == "v3_new")
    val listVals = await(RedisHelper.lget("list_test")).body
    assert(listVals.size == 2 && listVals == List("v2", "v3_new"))


    await(RedisHelper.del("int_test"))
    await(RedisHelper.incr("int_test", 10))
    await(RedisHelper.incr("int_test", 10))
    assert(await(RedisHelper.get("int_test")).body.toLong == 20)
    await(RedisHelper.decr("int_test", 4))
    await(RedisHelper.decr("int_test", 2))
    assert(await(RedisHelper.get("int_test")).body.toLong == 14)

  }


}


