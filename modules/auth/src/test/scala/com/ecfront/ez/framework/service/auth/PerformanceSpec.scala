package com.ecfront.ez.framework.service.auth

import java.util.concurrent.CountDownLatch

import com.ecfront.ez.framework.core.test.MockStartupSpec
import io.vertx.core.{Handler, Vertx}
import io.vertx.core.http.HttpClientResponse

class PerformanceSpec extends MockStartupSpec {

  test("性能测试") {

    for(i <-0 to 10){
      Vertx.vertx.createHttpClient().get(8080,"127.0.0.1","/public/test/wait/",new Handler[HttpClientResponse] {
        override def handle(event: HttpClientResponse): Unit = {
          logger.info("wait success")
        }
      }).end()
    }
    for(i <-0 to 10){
      Vertx.vertx.createHttpClient().get(8080,"127.0.0.1","/public/test/immediately/",new Handler[HttpClientResponse] {
        override def handle(event: HttpClientResponse): Unit = {
          logger.info("immediately success")
        }
      }).end()
    }

    new CountDownLatch(1).await()

  }


}
