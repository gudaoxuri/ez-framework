package com.ecfront.ez.framework.core.rpc

import com.ecfront.common.Resp
import com.ecfront.ez.framework.core.logger.Logging

@RPC("/test2/","测试2","")
class RPCTest2 extends Logging {

  @SUB("","订阅","","","")
  def t1(args: Map[String, String], body: String): Resp[Void] = {
    logger.info(">>>>>>>>>>>>>>>> sub")
    assert(body == "sub")
    Resp.success(null)
  }

  @RESP("resp","点对点","","","")
  def t2(args: Map[String, String], body: BodyTest): Resp[Void] = {
    logger.info(">>>>>>>>>>>>>>>> resp")
    assert(body.a == "resp")
    Resp.success(null)
  }

  @REPLY("/reply","回复","","","")
  def t3(args: Map[String, String], body: BodyTest): Resp[BodyTest] = {
    Resp.success(body)
  }

}


