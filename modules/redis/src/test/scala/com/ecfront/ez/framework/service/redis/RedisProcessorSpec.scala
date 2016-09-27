package com.ecfront.ez.framework.service.redis

import java.util.concurrent.CountDownLatch

import com.ecfront.common.JsonHelper
import com.ecfront.ez.framework.core.test.MockStartupSpec

class RedisProcessorSpec extends MockStartupSpec {

  test("Redis Test") {
    RedisProcessor.flushdb()

    RedisProcessor.del("n_test")
    assert(!RedisProcessor.exists("n_test"))
    RedisProcessor.set("n_test", s"""{"name":"jzy"}""", 1)
    assert(RedisProcessor.exists("n_test"))
    assert(JsonHelper.toJson(RedisProcessor.get("n_test")).get("name").asText() == "jzy")
    Thread.sleep(1000)
    assert(!RedisProcessor.exists("n_test"))
    assert(RedisProcessor.get("n_test") == null)

    RedisProcessor.del("hash_test")
    RedisProcessor.hmset("hash_test", Map("f1" -> "v1", "f2" -> "v2"))
    RedisProcessor.hset("hash_test", "f3", "v3")
    assert(RedisProcessor.hget("hash_test", "f3") == "v3")
    assert(RedisProcessor.hget("hash_test", "notexist") == null)
    assert(RedisProcessor.hexists("hash_test", "f3"))
    val hashVals = RedisProcessor.hgetAll("hash_test")
    assert(hashVals.size == 3 && hashVals("f1") == "v1" && hashVals("f2") == "v2" && hashVals("f3") == "v3")
    RedisProcessor.hdel("hash_test", "f3")
    assert(!RedisProcessor.hexists("hash_test", "f3"))
    RedisProcessor.del("hash_test")
    assert(!RedisProcessor.exists("hash_test"))

    RedisProcessor.del("list_test")
    RedisProcessor.lmset("list_test", List("v1", "v2"))
    RedisProcessor.lpush("list_test", "v0")
    RedisProcessor.lset("list_test", "v2_new", 2)
    assert(RedisProcessor.llen("list_test") == 3)
    assert(RedisProcessor.lpop("list_test") == "v0")
    assert(RedisProcessor.llen("list_test") == 2)
    assert(RedisProcessor.lindex("list_test", 1) == "v2_new")
    val listVals = RedisProcessor.lget("list_test")
    assert(listVals.size == 2 && listVals == List("v2", "v2_new"))

    RedisProcessor.del("int_test")
    assert(RedisProcessor.incr("int_test", 0) == 0)
    RedisProcessor.incr("int_test", 10)
    assert(RedisProcessor.get("int_test") == "10")
    RedisProcessor.incr("int_test", 0)
    assert(RedisProcessor.get("int_test") == "10")
    RedisProcessor.incr("int_test", 10)
    assert(RedisProcessor.get("int_test") == "20")
    RedisProcessor.decr("int_test", 4)
    RedisProcessor.decr("int_test", 2)
    assert(RedisProcessor.get("int_test") == "14")
    RedisProcessor.expire("int_test", 1)
    assert(RedisProcessor.get("int_test") == "14")
    Thread.sleep(1100)
    assert(RedisProcessor.get("int_test") == null)
  }

  test("Compression test") {
    RedisProcessor.flushdb()
    val max = 1000
    val cdl = new CountDownLatch(max)
    val setThreads = for (i <- 0 until max) yield
      new Thread(new Runnable {
        override def run(): Unit = {
          RedisProcessor.set(s"test$i", i + "")
        }
      })
    setThreads.foreach(_.start())
    val getThreads = for (i <- 0 until max) yield
      new Thread(new Runnable {
        override def run(): Unit = {
          while (!RedisProcessor.exists(s"test$i")) {
          }
          RedisProcessor.get(s"test$i")
          RedisProcessor.del(s"test$i")
          cdl.countDown()
        }
      })
    getThreads.foreach(_.start())
    cdl.await()
  }

}


