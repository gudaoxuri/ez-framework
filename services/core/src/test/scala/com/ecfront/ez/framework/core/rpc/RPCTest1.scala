package com.ecfront.ez.framework.core.rpc

import com.ecfront.common.Resp

import scala.beans.BeanProperty

@RPC("/test1/","测试","")
class RPCTest1 {

  @GET("","获取xx","","")
  def t1(args: Map[String, String]): Resp[Void] = {
    Resp.success(null)
  }

  @POST("post","提交xx","","||String|uri|true","||String|url")
  def t2(args: Map[String, String], body: String): Resp[String] = {
    Resp.success(body)
  }

  @PUT("/put","更新xx",
    """
      NOTE: 补充说明
    ""","|a|String|modify|true","")
  def t3(args: Map[String, String], body: BodyTest): Resp[BodyTest] = {
    Resp.success(body)
  }

  @DELETE(":id/","删除xx","","")
  def t4(args: Map[String, String]): Resp[String] = {
    Resp.success(args("id"))
  }

  @WS("/ws/:id/:id2/","WSxx","",
    """
      |aa|String|附加1|false
      |bb|String|附加2|false
    ""","||Boolean|成功或失败")
  def t5(args: Map[String, String], body: Map[String, Any]): Resp[Boolean] = {
    assert(args("id") == "11" && args("id2") == "222")
    Resp.success(body("a").asInstanceOf[Boolean])
  }

  @PUT("/put2","更新xx",
    """
      NOTE: 补充说明
    ""","|a|String|modify|true","")
  def t13(args: Map[String, String], body: BodyTest): Resp[List[BodyTest]] = {
    Resp.success(List())
  }

}

class BodyTest {
  @Label("aaa")
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
