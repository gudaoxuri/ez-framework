package com.asto.ez.framework.function

import java.util.concurrent.CountDownLatch

import com.asto.ez.framework.helper.RedisHelper
import com.ecfront.common.JsonHelper
import com.typesafe.scalalogging.slf4j.LazyLogging
import io.vertx.core.Vertx
import org.scalatest.FunSuite

import scala.concurrent.ExecutionContext.Implicits.global

class RedisHelperSpec extends FunSuite with LazyLogging {

  test("RedisHelper Test") {
    val cdl = new CountDownLatch(1)

    RedisHelper.init(Vertx.vertx(), "192.168.4.99", 6379, 0)

    RedisHelper.exists("dop_test").onSuccess {
      case exists1Resp =>
        assert(!exists1Resp.body)
        RedisHelper.get("dop_test").onSuccess {
          case get1Resp =>
            assert(get1Resp.body == null)
            RedisHelper.set("dop_test", s"""{"name":"jzy"}""", 10).onSuccess {
              case setResp =>
                RedisHelper.exists("dop_test").onSuccess {
                  case exists2Resp =>
                    assert(exists2Resp.body)
                    RedisHelper.get("dop_test").onSuccess {
                      case get2Resp =>
                        assert(JsonHelper.toJson(get2Resp.body).get("name").asText() == "jzy")
                        Thread.sleep(10000)
                        RedisHelper.exists("dop_test").onSuccess {
                          case exists3Resp =>
                            assert(!exists3Resp.body)
                            RedisHelper.get("dop_test").onSuccess {
                              case get3Resp =>
                                assert(get3Resp.body == null)
                                cdl.countDown()
                            }
                        }
                    }
                }
            }
        }
    }

    cdl.await()
  }

}


