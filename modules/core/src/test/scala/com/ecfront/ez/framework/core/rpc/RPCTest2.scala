package com.ecfront.ez.framework.core.rpc

import com.ecfront.common.Resp
import com.typesafe.scalalogging.slf4j.LazyLogging

@RPC("/test2/")
class RPCTest2 extends LazyLogging {

  @SUB("")
  def t1(args: Map[String, String], body: String): Resp[Void] = {
    logger.info(">>>>>>>>>>>>>>>> sub")
    assert(body == "sub")
    Resp.success(null)
  }

  @RESP("resp")
  def t2(args: Map[String, String], body: BodyTest): Resp[Void] = {
    logger.info(">>>>>>>>>>>>>>>> resp")
    assert(body.a == "resp")
    Resp.success(null)
  }

  @REPLY("/reply")
  def t3(args: Map[String, String], body: BodyTest): Resp[BodyTest] = {
    Resp.success(body)
  }

}


