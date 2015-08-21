package com.ecfront.ez.framework.rpc

import java.util.concurrent.CountDownLatch

import com.ecfront.ez.framework.rpc.RPC.EChannel
import com.ecfront.ez.framework.rpc.RPC.EChannel._
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.scalatest.FunSuite

class RobustSpec extends FunSuite with LazyLogging {

  test("Robust测试") {
    jsonFunTest(EChannel.HTTP)
    jsonFunTest(EChannel.EVENT_BUS)
    jsonFunTest(EChannel.WEB_SOCKETS)
  }

  def jsonFunTest(channel: EChannel) {
    val latch = new CountDownLatch(10)
    val server = RPC.server.setPort(808).setChannel(channel)
    server.startup()
      .post[String]("/index/", classOf[String], {
      (param, body, _) =>
        Thread.sleep(10000)
        latch.countDown()
        throw new Exception("error")
    })
    Thread.sleep(1000)
    val client = RPC.client.setPort(808).setChannel(channel)
    client.startup()
    val start = System.currentTimeMillis()
    for (i <- 0 to 10) {
      new Thread(new Runnable {
        override def run(): Unit = {
          client.postSync[String]("/index/", "测试", classOf[String])
          logger.debug("########## Finished >>" + i)
        }
      }).start()
    }
    latch.await()
    println(">>>>>>> Total use：" + (System.currentTimeMillis() - start) / 1000 + "s")
    server.shutdown()
    client.shutdown()
  }

}




