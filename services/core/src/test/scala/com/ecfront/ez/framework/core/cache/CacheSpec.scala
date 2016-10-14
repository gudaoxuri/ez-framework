package com.ecfront.ez.framework.core.cache

import java.util.concurrent.CountDownLatch

import com.ecfront.common.JsonHelper
import com.ecfront.ez.framework.core.{EZ, MockStartupSpec}

class CacheSpec extends MockStartupSpec {

  test("Redis Test") {
    EZ.cache.flushdb()

    EZ.cache.del("n_test")
    assert(!EZ.cache.exists("n_test"))
    EZ.cache.set("n_test", s"""{"name":"jzy"}""", 1)
    assert(EZ.cache.exists("n_test"))
    assert(JsonHelper.toJson(EZ.cache.get("n_test")).get("name").asText() == "jzy")
    Thread.sleep(1000)
    assert(!EZ.cache.exists("n_test"))
    assert(EZ.cache.get("n_test") == null)

    EZ.cache.del("hash_test")
    EZ.cache.hmset("hash_test", Map("f1" -> "v1", "f2" -> "v2"))
    EZ.cache.hset("hash_test", "f3", "v3")
    assert(EZ.cache.hget("hash_test", "f3") == "v3")
    assert(EZ.cache.hget("hash_test", "notexist") == null)
    assert(EZ.cache.hexists("hash_test", "f3"))
    val hashVals = EZ.cache.hgetAll("hash_test")
    assert(hashVals.size == 3 && hashVals("f1") == "v1" && hashVals("f2") == "v2" && hashVals("f3") == "v3")
    EZ.cache.hdel("hash_test", "f3")
    assert(!EZ.cache.hexists("hash_test", "f3"))
    EZ.cache.del("hash_test")
    assert(!EZ.cache.exists("hash_test"))

    EZ.cache.del("list_test")
    EZ.cache.lmset("list_test", List("v1", "v2"))
    EZ.cache.lpush("list_test", "v0")
    EZ.cache.lset("list_test", "v2_new", 2)
    assert(EZ.cache.llen("list_test") == 3)
    assert(EZ.cache.lpop("list_test") == "v0")
    assert(EZ.cache.llen("list_test") == 2)
    assert(EZ.cache.lindex("list_test", 1) == "v2_new")
    val listVals = EZ.cache.lget("list_test")
    assert(listVals.size == 2 && listVals == List("v2", "v2_new"))

    EZ.cache.del("int_test")
    assert(EZ.cache.incr("int_test", 0) == 0)
    EZ.cache.incr("int_test", 10)
    assert(EZ.cache.get("int_test") == "10")
    EZ.cache.incr("int_test", 0)
    assert(EZ.cache.get("int_test") == "10")
    EZ.cache.incr("int_test", 10)
    assert(EZ.cache.get("int_test") == "20")
    EZ.cache.decr("int_test", 4)
    EZ.cache.decr("int_test", 2)
    assert(EZ.cache.get("int_test") == "14")
    EZ.cache.expire("int_test", 1)
    assert(EZ.cache.get("int_test") == "14")
    Thread.sleep(1100)
    assert(EZ.cache.get("int_test") == null)
  }

  test("Compression test") {
    EZ.cache.flushdb()
    val max = 1000
    val cdl = new CountDownLatch(max)
    val setThreads = for (i <- 0 until max) yield
      new Thread(new Runnable {
        override def run(): Unit = {
          EZ.cache.set(s"test$i", i + "")
        }
      })
    setThreads.foreach(_.start())
    val getThreads = for (i <- 0 until max) yield
      new Thread(new Runnable {
        override def run(): Unit = {
          while (!EZ.cache.exists(s"test$i")) {
          }
          EZ.cache.get(s"test$i")
          EZ.cache.del(s"test$i")
          cdl.countDown()
        }
      })
    getThreads.foreach(_.start())
    cdl.await()
  }

}


