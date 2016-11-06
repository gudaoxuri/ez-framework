package com.ecfront.ez.framework.service.tpsi

import java.util.concurrent.CountDownLatch

import com.ecfront.common.Resp
import com.ecfront.ez.framework.core.rpc.{REPLY, RESP, RPC, SUB}
import com.fasterxml.jackson.databind.JsonNode

import scala.beans.BeanProperty

@RPC("/tpsi/","","")
object TPSITestService extends TPSIService {

  val counter = new CountDownLatch(3)

  @REPLY("reply/")
  def reply(parameter: Map[String, String], body: TPSITestObj): Resp[TPSITestObj] = {
    assert(parameter("id") == "1")
    assert(body.t == "测试")
    assert(body.d == 2.2)
    exec(parameter("id"), "reply", body)
  }

  @SUB("sub/")
  def sub(parameter: Map[String, String], body: TPSITestObj): Resp[TPSITestObj] = {
    assert(parameter("id") == "1")
    assert(body.t == "测试")
    assert(body.d == 2.2)
    exec(parameter("id"), "sub", body)
  }

  @RESP("resp/")
  def resp(parameter: Map[String, String], body: TPSITestObj): Resp[TPSITestObj] = {
    assert(parameter("id") == "1")
    assert(body.t == "测试")
    assert(body.d == 2.2)
    exec(parameter("id"), "resp", body)
  }

  override protected def init(args: JsonNode): Unit = {
    assert(args.get("tt").asText() == "字段")
  }

  def exec(id: String, funName: String, body: TPSITestObj): Resp[TPSITestObj] = {
    execute(id, funName, {
      Thread.sleep(100)
      body
    }, {
      body =>
        counter.countDown()
        Resp.success(body.asInstanceOf[TPSITestObj])
    })
  }


}

class TPSITestObj {
  @BeanProperty
  var t: String = _
  @BeanProperty
  var d: BigDecimal = _
}

object TPSITestObj {
  def apply(t: String, d: BigDecimal): TPSITestObj = {
    val obj = new TPSITestObj()
    obj.t = t
    obj.d = d
    obj
  }
}
