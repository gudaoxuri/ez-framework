package com.ecfront.ez.framework.service.redis

import com.ecfront.common.JsonHelper
import com.ecfront.ez.framework.core.test.MockStartupSpec

class RedisProcessorSpec extends MockStartupSpec {

  test("Redis Test") {
    RedisProcessor.del("n_test")
    val exists1Resp = RedisProcessor.exists("n_test")
    assert(!exists1Resp.body)
    RedisProcessor.set("n_test", s"""{"name":"jzy"}""", 4)
    val exists2Resp = RedisProcessor.exists("n_test")
    assert(exists2Resp.body)
    val get2Resp = RedisProcessor.get("n_test")
    assert(JsonHelper.toJson(get2Resp.body).get("name").asText() == "jzy")
    Thread.sleep(4000)
    val exists3Resp = RedisProcessor.exists("n_test")
    assert(!exists3Resp.body)
    val get3Resp = RedisProcessor.get("n_test")
    assert(get3Resp.body == null)

    RedisProcessor.del("hash_test")
    RedisProcessor.hmset("hash_test", Map("f1" -> "v1", "f2" -> "v2"))
    RedisProcessor.hset("hash_test", "f3", "v3")
    assert(RedisProcessor.hget("hash_test", "f3").body == "v3")
    assert(RedisProcessor.hexists("hash_test", "f3").body)
    val hashVals = RedisProcessor.hgetall("hash_test").body
    assert(hashVals.size == 3 && hashVals("f1") == "v1" && hashVals("f2") == "v2" && hashVals("f3") == "v3")
    RedisProcessor.hdel("hash_test", "f3")
    assert(!RedisProcessor.hexists("hash_test", "f3").body)
    RedisProcessor.del("hash_test")
    assert(!RedisProcessor.exists("hash_test").body)

    RedisProcessor.del("list_test")
    RedisProcessor.lmset("list_test", List("v1", "v2"))
    RedisProcessor.lpush("list_test", "v3")
    RedisProcessor.lset("list_test", "v1_new", 2)
    assert(RedisProcessor.llen("list_test").body == 3)
    assert(RedisProcessor.lpop("list_test").body == "v3")
    assert(RedisProcessor.llen("list_test").body == 2)
    assert(RedisProcessor.lindex("list_test", 1).body == "v1_new")
    val listVals = RedisProcessor.lget("list_test").body
    assert(listVals.size == 2 && listVals == List("v2", "v1_new"))


    RedisProcessor.del("int_test")
    RedisProcessor.incr("int_test", 10)
    RedisProcessor.incr("int_test", 10)
    assert(RedisProcessor.get("int_test").body.toLong == 20)
    RedisProcessor.decr("int_test", 4)
    RedisProcessor.decr("int_test", 2)
    assert(RedisProcessor.get("int_test").body.toLong == 14)
  }

}


