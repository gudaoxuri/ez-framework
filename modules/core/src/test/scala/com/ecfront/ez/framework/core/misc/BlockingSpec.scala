package com.ecfront.ez.framework.core.misc

import java.util.concurrent.CountDownLatch

import io.vertx.core.{AsyncResult, Handler, Vertx}
import org.scalatest.FunSuite

class BlockingSpec extends FunSuite {

  private val vertx=Vertx.vertx()

  test("Bocking Test") {

    for( i <- 1 to 2){
      test(i)
    }
    new CountDownLatch(1).await()

  }

  def test(i: Int): Unit = {
    vertx.executeBlocking(new Handler[io.vertx.core.Future[Void]] {
      override def handle(event: io.vertx.core.Future[Void]): Unit = {
        event.complete()
      }
    },true, new Handler[AsyncResult[Void]] {
      override def handle(event: AsyncResult[Void]): Unit = {
        Thread.sleep(10000)
      }
    })
  }

}
