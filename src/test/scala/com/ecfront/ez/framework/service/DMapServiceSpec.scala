package com.ecfront.ez.framework.service

import java.util.concurrent.CountDownLatch
import java.util.{Timer, TimerTask}

import com.ecfront.ez.framework.service.common.DMapService


class DMapServiceSpec extends BasicSpec {

  test("DMap测试") {

    val map = DMapService[Long]("test_map")
    map.clear()

    val timer=new Timer()
    timer.schedule(new TimerTask {
      override def run(): Unit = {
        map.put("a",System.currentTimeMillis())
      }
    },0,1000)
    timer.schedule(new TimerTask {
      override def run(): Unit = {
        map.foreach({
          (k, v) =>
            println(">>a:" +v)
        })
      }
    },0,10000)
    new CountDownLatch(1).await()
  }

}





