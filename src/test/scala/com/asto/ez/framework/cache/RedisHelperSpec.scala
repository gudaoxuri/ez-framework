package com.asto.ez.framework.cache

import java.util.concurrent.CountDownLatch

import com.asto.ez.framework.BasicSpec
import com.ecfront.common.JsonHelper
import io.vertx.core.Vertx

import scala.async.Async.{async, await}
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

class RedisHelperSpec extends BasicSpec {

  test("RedisHelper Test") {
    val cdl = new CountDownLatch(1)

    RedisProcessor.init(Vertx.vertx(), "192.168.4.99", 6379, 0)

    Await.result(RedisProcessor.del("dop_test"), Duration.Inf)
    RedisProcessor.exists("dop_test").onSuccess {
      case exists1Resp =>
        assert(!exists1Resp.body)
        RedisProcessor.get("dop_test").onSuccess {
          case get1Resp =>
            assert(get1Resp.body == null)
            RedisProcessor.set("dop_test", s"""{"name":"jzy"}""", 10).onSuccess {
              case setResp =>
                RedisProcessor.exists("dop_test").onSuccess {
                  case exists2Resp =>
                    assert(exists2Resp.body)
                    RedisProcessor.get("dop_test").onSuccess {
                      case get2Resp =>
                        assert(JsonHelper.toJson(get2Resp.body).get("name").asText() == "jzy")
                        Thread.sleep(10000)
                        RedisProcessor.exists("dop_test").onSuccess {
                          case exists3Resp =>
                            assert(!exists3Resp.body)
                            RedisProcessor.get("dop_test").onSuccess {
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

    Await.result(RedisProcessor.del("list_test"), Duration.Inf)
    Await.result(RedisProcessor.lmset("list_test", List("v1", "v2")), Duration.Inf)
    Await.result(RedisProcessor.lpush("list_test", "v3"), Duration.Inf)
    Await.result(RedisProcessor.lset("list_test", "v3_new", 0), Duration.Inf)
    assert(Await.result(RedisProcessor.llen("list_test"), Duration.Inf).body == 3)
    assert(Await.result(RedisProcessor.lpop("list_test"), Duration.Inf).body == "v3_new")
    assert(Await.result(RedisProcessor.llen("list_test"), Duration.Inf).body == 2)
    assert(Await.result(RedisProcessor.lindex("list_test", 1), Duration.Inf).body == "v1")
    val listVals = Await.result(RedisProcessor.lget("list_test"), Duration.Inf).body
    assert(listVals.size == 2 && listVals == List("v2", "v1"))

    cdl.await()
  }

  test("RedisHelper Async Test") {
    RedisProcessor.init(Vertx.vertx(), "192.168.4.99", 6379, 0)
    val cdl = new CountDownLatch(1)
    testRedisHelperAsync().onSuccess {
      case resp =>
        cdl.countDown()
    }
    cdl.await()
  }

  def testRedisHelperAsync() = async {
    await(RedisProcessor.del("n_test"))
    val exists1Resp = await(RedisProcessor.exists("n_test"))
    assert(!exists1Resp.body)
    await(RedisProcessor.set("n_test", s"""{"name":"jzy"}""", 4))
    val exists2Resp = await(RedisProcessor.exists("n_test"))
    assert(exists2Resp.body)
    val get2Resp = await(RedisProcessor.get("n_test"))
    assert(JsonHelper.toJson(get2Resp.body).get("name").asText() == "jzy")
    Thread.sleep(4000)
    val exists3Resp = await(RedisProcessor.exists("n_test"))
    assert(!exists3Resp.body)
    val get3Resp = await(RedisProcessor.get("n_test"))
    assert(get3Resp.body == null)

    await(RedisProcessor.del("hash_test"))
    await(RedisProcessor.hmset("hash_test", Map("f1" -> "v1", "f2" -> "v2")))
    await(RedisProcessor.hset("hash_test", "f3", "v3"))
    assert(await(RedisProcessor.hget("hash_test", "f3")).body == "v3")
    assert(await(RedisProcessor.hexists("hash_test", "f3")).body)
    val hashVals = await(RedisProcessor.hgetall("hash_test")).body
    assert(hashVals.size == 3 && hashVals("f1") == "v1" && hashVals("f2") == "v2" && hashVals("f3") == "v3")
    await(RedisProcessor.hdel("hash_test", "f3"))
    assert(!await(RedisProcessor.hexists("hash_test", "f3")).body)
    await(RedisProcessor.del("hash_test"))
    assert(!await(RedisProcessor.exists("hash_test")).body)

    await(RedisProcessor.del("list_test"))
    await(RedisProcessor.lmset("list_test", List("v1", "v2")))
    await(RedisProcessor.lpush("list_test", "v3"))
    await(RedisProcessor.lset("list_test", "v3_new", 2))
    assert(await(RedisProcessor.llen("list_test")).body == 3)
    assert(await(RedisProcessor.lpop("list_test")).body == "v1")
    assert(await(RedisProcessor.llen("list_test")).body == 2)
    assert(await(RedisProcessor.lindex("list_test", 1)).body == "v3_new")
    val listVals = await(RedisProcessor.lget("list_test")).body
    assert(listVals.size == 2 && listVals == List("v2", "v3_new"))


    await(RedisProcessor.del("int_test"))
    await(RedisProcessor.incr("int_test", 10))
    await(RedisProcessor.incr("int_test", 10))
    assert(await(RedisProcessor.get("int_test")).body.toLong == 20)
    await(RedisProcessor.decr("int_test", 4))
    await(RedisProcessor.decr("int_test", 2))
    assert(await(RedisProcessor.get("int_test")).body.toLong == 14)

  }


}


