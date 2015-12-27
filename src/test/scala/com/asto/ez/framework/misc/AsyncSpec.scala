package com.asto.ez.framework.misc

import java.util.concurrent.CountDownLatch

import com.asto.ez.framework.BasicSpec
import scala.concurrent.ExecutionContext.Implicits.global
import scala.async.Async.{async, await}
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class AsyncSpec extends BasicSpec {

  test("Exception Test") {
    Await.result(exceptionTest(),Duration.Inf)
    new CountDownLatch(1).await()
  }

  def exceptionTest() = async {
   await(doAsyncTest())
  }

  def doAsyncTest():Future[Void]= {
    throw new Exception("error")
  }

}


