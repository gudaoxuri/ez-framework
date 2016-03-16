package com.ecfront.ez.framework.service.distributed

import com.ecfront.ez.framework.core.test.MockStartupSpec


class DCountDownLatchServiceSpec extends MockStartupSpec {

  test("DCountDownLatch测试") {

    val countDownLatch = DCountDownLatchService("test_cdl")
    countDownLatch.set(10)
    assert(countDownLatch.get == 10)
    countDownLatch.set(1)
    assert(countDownLatch.get == 1)

    new Thread(new Runnable {
      override def run(): Unit = {
        Thread.sleep(5000)
        countDownLatch.countDown()
      }
    }).start()
    countDownLatch.await()
    val countDownLatch2 = DCountDownLatchService("test_cdl")
    assert(countDownLatch2.get == 0)
    countDownLatch.delete()
    assert(countDownLatch2.get == 0)
  }

}





