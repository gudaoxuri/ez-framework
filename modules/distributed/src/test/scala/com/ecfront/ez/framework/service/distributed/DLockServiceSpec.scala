package com.ecfront.ez.framework.service.distributed

import com.ecfront.ez.framework.core.test.MockStartupSpec


class DLockServiceSpec extends MockStartupSpec {

  test("DLock测试") {

    val lock = DLockService("test_lock")
    lock.delete()

    val t1 = new Thread(new Runnable {
      override def run(): Unit = {
        try {
          lock.lock()
          println("Lock > " + Thread.currentThread().getId)
          Thread.sleep(500)
        } finally {
          println("UnLock > " + Thread.currentThread().getId)
          lock.unLock()
        }
      }
    })
    t1.start()

    t1.join()

    val t3 = new Thread(new Runnable {
      override def run(): Unit = {
        for (i <- 0 to 100) {
          Thread.sleep(10)
          lock.tryLock()
        }
      }
    })
    t3.start()

    val t4 = new Thread(new Runnable {
      override def run(): Unit = {
        for (i <- 0 to 100) {
          Thread.sleep(4)
          lock.delete()
        }
      }
    })
    t4.start()

    t3.join()
    t4.join()
  }

}





