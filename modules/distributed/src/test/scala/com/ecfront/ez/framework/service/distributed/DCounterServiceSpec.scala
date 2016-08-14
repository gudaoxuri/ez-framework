package com.ecfront.ez.framework.service.distributed

import java.util.concurrent.TimeUnit

import com.ecfront.ez.framework.core.test.MockStartupSpec
import com.ecfront.ez.framework.service.redis.RedisProcessor


class DCounterServiceSpec extends MockStartupSpec {

  test("DCounter测试") {

    val counter = DCounterService("test_counter")

    assert(counter.get == 0)
    counter.set(10)
    assert(counter.get == 10)
    assert(counter.inc() == 11)
    assert(counter.inc() == 12)
    assert(counter.dec() == 11)
    counter.inc(11)
    assert(counter.get == 11)
    counter.delete()
    assert(counter.get == 0)
  }

  test("expire test"){
    println(RedisProcessor.custom().getAtomicLong("a").incrementAndGet())
    RedisProcessor.custom().getAtomicLong("a").expire(1,TimeUnit.SECONDS)
    println(RedisProcessor.custom().getAtomicLong("a").incrementAndGet())
    Thread.sleep(5000)
    println(RedisProcessor.custom().getAtomicLong("a").incrementAndGet())
  }

}





