package com.ecfront.ez.framework.rpc

import java.util.concurrent.CountDownLatch

import com.ecfront.common.Resp
import com.ecfront.ez.framework.rpc.RPC.EChannel
import org.scalatest.FunSuite

class MultiSpec extends FunSuite {

  test("多服务测试") {
    RPC.server.setChannel(EChannel.HTTP).setPort(8001).startup().get("/test/", {
      (param, _, _) =>
        Resp.success("OK1")
    })
    RPC.server.setChannel(EChannel.HTTP).setPort(8002).startup().get("/test/", {
      (param, _, _) =>
        Resp.success("OK2")
    })
    RPC.server.setChannel(EChannel.HTTP).setPort(8003).startup().get("/test/", {
      (param, _, _) =>
        Resp.success("OK3")
    })
    RPC.server.setChannel(EChannel.EVENT_BUS).setPort(8004).startup().get("/test/", {
      (param, _, _) =>
        Resp.success("OK4")
    })
    /*RPC.server.setChannel(EChannel.EVENT_BUS).setPort(8005).startup().get("/test/", {
      (param, _, _) =>
        Resp.success("OK5")
    })
    RPC.server.setChannel(EChannel.EVENT_BUS).setPort(8006).startup().get("/test/", {
      (param, _, _) =>
        Resp.success("OK6")
    })*/
    RPC.server.setChannel(EChannel.WEB_SOCKETS).setPort(8007).startup().get("/test/", {
      (param, _, _) =>
        Resp.success("OK7")
    })
    RPC.server.setChannel(EChannel.WEB_SOCKETS).setPort(8008).startup().get("/test/", {
      (param, _, _) =>
        Resp.success("OK8")
    })
    RPC.server.setChannel(EChannel.WEB_SOCKETS).setPort(8009).startup().get("/test/", {
      (param, _, _) =>
        Resp.success("OK9")
    })

    val latch = new CountDownLatch(7)

    RPC.client.setChannel(EChannel.HTTP).setPort(8001).startup().get[String]("/test/", classOf[String], {
      result =>
        assert(result.code == "200")
        assert(result.body == "OK1")
        latch.countDown()
    })
    RPC.client.setChannel(EChannel.HTTP).setPort(8002).startup().get[String]("/test/", classOf[String], {
      result =>
        assert(result.code == "200")
        assert(result.body == "OK2")
        latch.countDown()
    })
    RPC.client.setChannel(EChannel.HTTP).setPort(8003).startup().get[String]("/test/", classOf[String], {
      result =>
        assert(result.code == "200")
        assert(result.body == "OK3")
        latch.countDown()
    })
    RPC.client.setChannel(EChannel.EVENT_BUS).setPort(8004).startup().get[String]("/test/", classOf[String], {
      result =>
        assert(result.code == "200")
        assert(result.body == "OK4")
        latch.countDown()
    })
    /* RPC.client.setChannel(EChannel.EVENT_BUS).setPort(8005).startup().get[String]("/test/", classOf[String], {
       result =>
         assert(result.code == "200")
         assert(result.body == "OK5")
         latch.countDown()
     })
     RPC.client.setChannel(EChannel.EVENT_BUS).setPort(8006).startup().get[String]("/test/", classOf[String], {
       result =>
         assert(result.code == "200")
         assert(result.body == "OK6")
         latch.countDown()
     })*/
    RPC.client.setChannel(EChannel.WEB_SOCKETS).setPort(8007).startup().get[String]("/test/", classOf[String], {
      result =>
        assert(result.code == "200")
        assert(result.body == "OK7")
        latch.countDown()
    })
    RPC.client.setChannel(EChannel.WEB_SOCKETS).setPort(8008).startup().get[String]("/test/", classOf[String], {
      result =>
        assert(result.code == "200")
        assert(result.body == "OK8")
        latch.countDown()
    })
    RPC.client.setChannel(EChannel.WEB_SOCKETS).setPort(8009).startup().get[String]("/test/", classOf[String], {
      result =>
        assert(result.code == "200")
        assert(result.body == "OK9")
        latch.countDown()
    })
    latch.await()
  }

}


