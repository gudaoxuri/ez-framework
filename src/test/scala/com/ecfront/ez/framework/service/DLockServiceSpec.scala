package com.ecfront.ez.framework.service

import com.ecfront.ez.framework.BasicSpec
import com.ecfront.ez.framework.service.common.DLockService

class DLockServiceSpec extends BasicSpec {

  test("DLock测试") {

    val lock = DLockService("test_lock")
    lock.delete()
    lock.lock()
    println("Lock > " + Thread.currentThread().getId)

    val t1 = new Thread(new Runnable {
      override def run(): Unit = {
        try {
          println("UnLock > " + Thread.currentThread().getId)
          lock.unLock()
        } catch {
          case e: Exception =>
            assert(false)
            e.printStackTrace()
        }
      }
    })
    t1.start()

    t1.join()

    val t3 = new Thread(new Runnable {
      override def run(): Unit = {
        for (i <- 0 to 100) {
          Thread.sleep(10)
          println("tryLock > " + lock.tryLock())
        }
      }
    })
    t3.start()

    val t4 = new Thread(new Runnable {
      override def run(): Unit = {
        for (i <- 0 to 100) {
          Thread.sleep(4)
          println("delete")
          lock.delete()
        }
      }
    })
    t4.start()

    t3.join()
    t4.join()
  }


  test("集群关闭后Lock自动释放测试"){
    val lock = DLockService("test_lock")
    while (true){
      Thread.sleep(1000)
      println("tryLock > " + lock.tryLock())
    }
  }
}





