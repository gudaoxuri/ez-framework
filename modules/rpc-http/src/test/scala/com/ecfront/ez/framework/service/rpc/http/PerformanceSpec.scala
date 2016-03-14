package com.ecfront.ez.framework.service.rpc.http

import java.util.concurrent.CountDownLatch

import com.ecfront.ez.framework.core.test.MockStartupSpec
import com.ecfront.ez.framework.service.rpc.http.test.EZ_Resource

class PerformanceSpec extends MockStartupSpec {

  test("性能测试") {

    EZ_Resource.deleteByCond("")

    val threads = for (i <- 0 to 2000)
      yield new Thread(new Runnable {
        override def run(): Unit = {
          val result = HttpClientProcessor.post("http://127.0.0.1:8080/resource/", EZ_Resource(System.nanoTime() + "", "GET", "/2s"))
        }
      })
    threads.foreach(_.start())
    Thread.sleep(10000)
    for (i <- 0 to 2000)
      yield new Thread(new Runnable {
        override def run(): Unit = {
          HttpClientProcessor.post("http://127.0.0.1:8080/resource/", EZ_Resource(System.nanoTime() + "", "GET", "/2s"))
        }
      }).start()
    new CountDownLatch(1).await()

  }


}
