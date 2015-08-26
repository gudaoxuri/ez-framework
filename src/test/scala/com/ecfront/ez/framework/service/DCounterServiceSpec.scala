package com.ecfront.ez.framework.service

import com.ecfront.ez.framework.BasicSpec
import com.ecfront.ez.framework.service.common.DCounterService


class DCounterServiceSpec extends BasicSpec {

  test("DCounter测试") {

    val counter = DCounterService("test_counter")
    counter.set(0)

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





