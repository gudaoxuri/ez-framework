package com.ecfront.ez.framework.rpc

import java.util.concurrent.CountDownLatch

import com.ecfront.common.Resp
import com.ecfront.ez.framework.rpc.RPC.EChannel
import com.ecfront.ez.framework.rpc.RPC.EChannel._
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.scalatest.FunSuite

class PerformanceSpec extends FunSuite with LazyLogging {

  test("性能测试") {
    perfTest(EChannel.EVENT_BUS)
    perfTest(EChannel.WEB_SOCKETS)
    perfTest(EChannel.HTTP)
  }

  def perfTest(channel: EChannel) {

    val server = RPC.server.setPort(808).setChannel(channel).startup()
      .put[TestModel]("/index/:id/", classOf[TestModel], {
      (param, body, _) =>
        Resp.success(body)
    })

    val client = RPC.client.setPort(808).setChannel(channel).startup()
    client.putSync[TestModel]("/index/test/", TestModel("测试"), classOf[TestModel])

    val latch = new CountDownLatch(2000)
    val threads = for (i <- 0 to 2000)
      yield new Thread(new Runnable {
        override def run(): Unit = {
          //          val res = JsonHelper.toGenericObject[Resp[TestModel]](HttpHelper.put("http://127.0.0.1:808/index/test/",TestModel("测试")).body)
          //          assert(res.code == "200")
          //          assert(res.body.name == "测试")
          //          println(">>>>>>> Current Count：" + latch.getCount)

          client.put[TestModel]("/index/test/", TestModel("测试"), classOf[TestModel], {
            result =>
          assert(result.code == "200")
          assert(result.body.name == "测试")
          println(">>>>>>> Current Count：" + latch.getCount)
           latch.countDown()
          })

          /*val ss = HttpHelper.put("http://127.0.0.1:808/index/test/",TestModel("测试"))
          val result = JsonHelper.toGenericObject[Resp[TestModel]](ss)
          assert(result.code == "200")
          assert(result.body.name == "测试")
          println(">>>>>>> Current Count：" + latch.getCount)
          latch.countDown()*/
        }
      })
    val start = System.currentTimeMillis()
    threads.foreach(_.start())
    latch.await()
    val end = System.currentTimeMillis()
    println(">>>>>>> Total use：" + (end - start) / 1000 + "s")

    server.shutdown()
    client.shutdown()
  }

  test("HTTP性能测试") {
    RPC.server.setPort(808).setChannel(EChannel.HTTP).startup()
      .get("/index/:id/", {
        (param, _, _) =>
          Resp.success("OK")
      })
    val latch = new CountDownLatch(1)
    latch.await()

  }

}
