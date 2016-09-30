package com.ecfront.ez.framework.core.cache

import java.util.concurrent.CountDownLatch

import com.ecfront.common.JsonHelper
import com.ecfront.ez.framework.core.EZContext
import com.ecfront.ez.framework.core.test.MockStartupSpec

class CacheSpec extends MockStartupSpec {

  test("Redis Test") {
    EZContext.cache.flushdb()

    EZContext.cache.del("n_test")
    assert(!EZContext.cache.exists("n_test"))
    EZContext.cache.set("n_test", s"""{"name":"jzy"}""", 1)
    assert(EZContext.cache.exists("n_test"))
    assert(JsonHelper.toJson(EZContext.cache.get("n_test")).get("name").asText() == "jzy")
    Thread.sleep(1000)
    assert(!EZContext.cache.exists("n_test"))
    assert(EZContext.cache.get("n_test") == null)

    EZContext.cache.del("hash_test")
    EZContext.cache.hmset("hash_test", Map("f1" -> "v1", "f2" -> "v2"))
    EZContext.cache.hset("hash_test", "f3", "v3")
    assert(EZContext.cache.hget("hash_test", "f3") == "v3")
    assert(EZContext.cache.hget("hash_test", "notexist") == null)
    assert(EZContext.cache.hexists("hash_test", "f3"))
    val hashVals = EZContext.cache.hgetAll("hash_test")
    assert(hashVals.size == 3 && hashVals("f1") == "v1" && hashVals("f2") == "v2" && hashVals("f3") == "v3")
    EZContext.cache.hdel("hash_test", "f3")
    assert(!EZContext.cache.hexists("hash_test", "f3"))
    EZContext.cache.del("hash_test")
    assert(!EZContext.cache.exists("hash_test"))

    EZContext.cache.del("list_test")
    EZContext.cache.lmset("list_test", List("v1", "v2"))
    EZContext.cache.lpush("list_test", "v0")
    EZContext.cache.lset("list_test", "v2_new", 2)
    assert(EZContext.cache.llen("list_test") == 3)
    assert(EZContext.cache.lpop("list_test") == "v0")
    assert(EZContext.cache.llen("list_test") == 2)
    assert(EZContext.cache.lindex("list_test", 1) == "v2_new")
    val listVals = EZContext.cache.lget("list_test")
    assert(listVals.size == 2 && listVals == List("v2", "v2_new"))

    EZContext.cache.del("int_test")
    assert(EZContext.cache.incr("int_test", 0) == 0)
    EZContext.cache.incr("int_test", 10)
    assert(EZContext.cache.get("int_test") == "10")
    EZContext.cache.incr("int_test", 0)
    assert(EZContext.cache.get("int_test") == "10")
    EZContext.cache.incr("int_test", 10)
    assert(EZContext.cache.get("int_test") == "20")
    EZContext.cache.decr("int_test", 4)
    EZContext.cache.decr("int_test", 2)
    assert(EZContext.cache.get("int_test") == "14")
    EZContext.cache.expire("int_test", 1)
    assert(EZContext.cache.get("int_test") == "14")
    Thread.sleep(1100)
    assert(EZContext.cache.get("int_test") == null)
  }

  test("Compression test") {
    EZContext.cache.flushdb()
    val max = 1000
    val cdl = new CountDownLatch(max)
    val setThreads = for (i <- 0 until max) yield
      new Thread(new Runnable {
        override def run(): Unit = {
          EZContext.cache.set(s"test$i", i + "")
        }
      })
    setThreads.foreach(_.start())
    val getThreads = for (i <- 0 until max) yield
      new Thread(new Runnable {
        override def run(): Unit = {
          while (!EZContext.cache.exists(s"test$i")) {
          }
          EZContext.cache.get(s"test$i")
          EZContext.cache.del(s"test$i")
          cdl.countDown()
        }
      })
    getThreads.foreach(_.start())
    cdl.await()
  }

}


