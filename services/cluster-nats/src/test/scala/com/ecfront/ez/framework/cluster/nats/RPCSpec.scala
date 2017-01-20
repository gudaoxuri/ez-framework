
package com.ecfront.ez.framework.cluster.nats

import java.util.concurrent.CountDownLatch

import com.ecfront.ez.framework.test.MockStartupSpec

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class RPCSpec extends MockStartupSpec {

  val cluster = NatsCluster

  test("rpc test") {
    val conflictFlag = ArrayBuffer[String]()

    // rpc
    conflictFlag.clear()
    new Thread(new Runnable {
      override def run() = {
        cluster.rpc.reply("test_rpc")({
          (msg, args) =>
            if (conflictFlag.contains(msg)) {
              assert(1 == 2)
            } else {
              conflictFlag += msg
              assert(args("h") == "1")
              logger.info("1 rpc>>" + msg)
            }
            (msg, args)
        })
        new CountDownLatch(1).await()
      }
    }).start()

    new Thread(new Runnable {
      override def run() = {
        cluster.rpc.reply("test_rpc")({
          (msg, args) =>
            if (conflictFlag.contains(msg)) {
              assert(1 == 2)
            } else {
              conflictFlag += msg
              assert(args("h") == "1")
              logger.info("2 rpc>>" + msg)
            }
            (msg, args)
        })
        new CountDownLatch(1).await()
      }
    }).start()

    new Thread(new Runnable {
      override def run() = {
        cluster.rpc.reply("test_rpc/a")({
          (msg, args) =>
            assert(1 == 2)
            (msg, args)
        })
        new CountDownLatch(1).await()
      }
    }).start()

    var replyMsg = cluster.rpc.ack("test_rpc", "msg1", Map("h" -> "1"))
    assert(replyMsg._1 == "msg1" && replyMsg._2("h") == "1")
    replyMsg = cluster.rpc.ack("test_rpc", "msg2", Map("h" -> "1"))
    assert(replyMsg._1 == "msg2" && replyMsg._2("h") == "1")

    Thread.sleep(1000)

    // rpc-async
    conflictFlag.clear()
    new Thread(new Runnable {
      override def run() = {
        cluster.rpc.replyAsync("test_rpc_async")({
          (msg, args) =>
            if (conflictFlag.contains(msg)) {
              assert(1 == 2)
            } else {
              conflictFlag += msg
              assert(args("h") == "1")
              logger.info("1 rpc async>>" + msg)
            }
            Future((msg, args))
        })
        new CountDownLatch(1).await()
      }
    }).start()

    new Thread(new Runnable {
      override def run() = {
        cluster.rpc.replyAsync("test_rpc_async")({
          (msg, args) =>
            if (conflictFlag.contains(msg)) {
              assert(1 == 2)
            } else {
              conflictFlag += msg
              assert(args("h") == "1")
              logger.info("2 rpc async>>" + msg)
            }
            Future((msg, args))
        })
        new CountDownLatch(1).await()
      }
    }).start()

    new Thread(new Runnable {
      override def run() = {
        cluster.rpc.replyAsync("test_rpc_async/a")({
          (msg, args) =>
            assert(1 == 2)
            Future((msg, args))
        })
        new CountDownLatch(1).await()
      }
    }).start()

    val rpcAsyncCdl = new CountDownLatch(2)
    cluster.rpc.ackAsync("test_rpc_async", "msg1", Map("h" -> "1"))(
      {
        (msg, args) =>
          assert(msg == "msg1" && args("h") == "1")
          rpcAsyncCdl.countDown()
      }, {
        error =>
          assert(1 == 2)
      }
    )
    cluster.rpc.ackAsync("test_rpc_async", "msg2", Map("h" -> "1"))(
      {
        (msg, args) =>
          assert(msg == "msg2" && args("h") == "1")
          rpcAsyncCdl.countDown()
      }, {
        error =>
          assert(1 == 2)
      }
    )
    Thread.sleep(1000)
    rpcAsyncCdl.await()

  }
}

