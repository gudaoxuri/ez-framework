package com.ecfront.ez.framework.core.dist

import com.ecfront.ez.framework.core.EZ
import com.ecfront.ez.framework.core.test.MockStartupSpec


class LockServiceSpec extends MockStartupSpec {

  test("lock test") {

    val lock = EZ.dist.lock("test_lock")
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
        val lock = EZ.dist.lock("test_lock")
        assert(lock.tryLock())
        logger.info("locked")
        Thread.sleep(10000)
        lock.unLock()
        logger.info("unlock")
      }
    })
    t3.start()

    Thread.sleep(1000)
    val t4 = new Thread(new Runnable {
      override def run(): Unit = {
        val lock = EZ.dist.lock("test_lock")
        while (!lock.tryLock()) {
          logger.info("waiting 1 unlock")
          Thread.sleep(100)
        }
      }
    })
    t4.start()

    val t5 = new Thread(new Runnable {
      override def run(): Unit = {
        val lock = EZ.dist.lock("test_lock")
        while (!lock.tryLock(5000)) {
          logger.info("waiting 2 unlock")
          Thread.sleep(100)
        }
      }
    })
    t5.start()

    t3.join()
    t4.join()
    t5.join()
  }

}





