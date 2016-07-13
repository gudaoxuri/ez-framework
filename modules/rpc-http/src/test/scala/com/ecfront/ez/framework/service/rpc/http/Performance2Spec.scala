package com.ecfront.ez.framework.service.rpc.http

import java.util.concurrent.{CountDownLatch, Executor, Executors}

import com.ecfront.ez.framework.core.test.BasicSpec
import com.typesafe.scalalogging.slf4j.LazyLogging
import io.vertx.core.http._
import io.vertx.core._

class Performance2Spec extends BasicSpec {

  test("performance test") {
    val vertx = Vertx.vertx()
    vertx.createHttpServer()
      .requestHandler(new Handler[HttpServerRequest] {
        override def handle(event: HttpServerRequest): Unit = {
          vertx.executeBlocking(new Handler[Future[String]] {
            override def handle(e: Future[String]): Unit = {
              logger.info("waiting")
              // Some block query
              if(event.path().contains("test"))
                Thread.sleep(10000000)
              e.complete("")
            }
          }, false, new Handler[AsyncResult[String]] {
            override def handle(e: AsyncResult[String]): Unit = {
              event.response().end("ok")
            }
          })
        }
      }).listen(8080, "127.0.0.1")
    new CountDownLatch(1).await()
  }


}

case class RunTest(resp: HttpServerResponse) extends Runnable with LazyLogging{
  override def run(): Unit = {
    logger.info("waiting")
    Thread.sleep(10000000)
    resp.end("ok")
  }
}
