
package com.ecfront.ez.framework.cluster.rabbitmq

import java.util.concurrent.CountDownLatch

import com.ecfront.ez.framework.test.MockStartupSpec

import scala.collection.mutable.ArrayBuffer


class MQSpec extends MockStartupSpec {

  val cluster = RabbitmqCluster

  test("mq test") {
    // pub-sub
    val pubSubCdl = new CountDownLatch(4)
    new Thread(new Runnable {
      override def run() = {
        cluster.mq.subscribe("test_pub_sub")({
          (msg, args) =>
            assert(msg == "msg")
            assert(args("h") == "1")
            logger.info("1 pub_sub>>" + msg)
            pubSubCdl.countDown()
        })
        pubSubCdl.await()
      }
    }).start()

    new Thread(new Runnable {
      override def run() = {
        cluster.mq.subscribe("test_pub_sub")({
          (msg, args) =>
            assert(msg == "msg")
            assert(args("h") == "1")
            logger.info("2 pub_sub>>" + msg)
            pubSubCdl.countDown()
        })
        pubSubCdl.await()
      }
    }).start()

    new Thread(new Runnable {
      override def run() = {
        cluster.mq.subscribe("test_pub_sub/a")({
          (msg, args) =>
            assert(1 == 2)
        })
        new CountDownLatch(1).await()
      }
    }).start()

    cluster.mq.publish("test_pub_sub", "msg", Map("h" -> "1"))
    Thread.sleep(100)
    cluster.mq.publish("test_pub_sub", "msg", Map("h" -> "1"))
    pubSubCdl.await()

    // req-resp
    val conflictFlag = ArrayBuffer[String]()
    new Thread(new Runnable {
      override def run() = {
        cluster.mq.response("test_rep_resp")({
          (msg, args) =>
            if (conflictFlag.contains(msg)) {
              assert(1 == 2)
            } else {
              conflictFlag += msg
              assert(args("h") == "1")
              logger.info("1 req_resp>>" + msg)
            }
        })
        new CountDownLatch(1).await()
      }
    }).start()

    new Thread(new Runnable {
      override def run() = {
        cluster.mq.response("test_rep_resp")({
          (msg, args) =>
            if (conflictFlag.contains(msg)) {
              assert(1 == 2)
            } else {
              assert(args("h") == "1")
              logger.info("2 req_resp>>" + msg)
            }
        })
        new CountDownLatch(1).await()
      }
    }).start()

    new Thread(new Runnable {
      override def run() = {
        cluster.mq.response("test_rep_resp/a")({
          (msg, args) =>
            assert(1 == 2)
        })
        new CountDownLatch(1).await()
      }
    }).start()

    cluster.mq.request("test_rep_resp", "msg1", Map("h" -> "1"))
    cluster.mq.request("test_rep_resp", "msg2", Map("h" -> "1"))

    Thread.sleep(1000)

  }

}

