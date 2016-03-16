package com.ecfront.ez.framework.service.distributed

import com.ecfront.ez.framework.core.test.MockStartupSpec


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

}





