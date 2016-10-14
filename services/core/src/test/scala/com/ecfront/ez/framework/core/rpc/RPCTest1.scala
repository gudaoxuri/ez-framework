package com.ecfront.ez.framework.core.rpc

import com.ecfront.common.Resp

import scala.beans.BeanProperty

@RPC("/test1/")
class RPCTest1 {

  @GET("")
  def t1(args: Map[String, String]): Resp[Void] = {
    Resp.success(null)
  }

  @POST("post")
  def t2(args: Map[String, String], body: String): Resp[String] = {
    Resp.success(body)
  }

  @PUT("/put")
  def t3(args: Map[String, String], body: BodyTest): Resp[BodyTest] = {
    Resp.success(body)
  }

  @DELETE(":id/")
  def t4(args: Map[String, String]): Resp[String] = {
    Resp.success(args("id"))
  }

  @WS("/ws/:id/:id2/")
  def t5(args: Map[String, String], body: Map[String, Any]): Resp[Boolean] = {
    assert(args("id") == "11" && args("id2") == "222")
    Resp.success(body("a").asInstanceOf[Boolean])
  }

}

class BodyTest {
  @BeanProperty @Require
  var a: String = _
}

object BodyTest {
  def apply(a: String): BodyTest = {
    val b = new BodyTest()
    b.a = a
    b
  }
}
