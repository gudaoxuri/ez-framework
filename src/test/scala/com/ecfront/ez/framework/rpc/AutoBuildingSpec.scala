package com.ecfront.ez.framework.rpc

import com.ecfront.common.Resp
import com.ecfront.ez.framework.rpc.RPC.EChannel
import com.ecfront.ez.framework.rpc.RPC.EChannel._
import org.scalatest.FunSuite

class AutoBuildingSpec extends FunSuite {

  test("自动构建测试") {
    autoBuildingTest(EChannel.HTTP)
    autoBuildingTest(EChannel.EVENT_BUS)
    autoBuildingTest(EChannel.WEB_SOCKETS)
  }

  def autoBuildingTest(channel: EChannel) {

    val server = RPC.server.setPort(808).setChannel(channel).startup().autoBuilding(AService)

    Thread.sleep(1000)

    val client = RPC.client.setPort(808).setChannel(channel).startup()

    assert(client.getSync[TestModel]("/test1/1/", classOf[TestModel]).get.body.name == "测试")
    assert(client.postSync[String]("/test1/", TestModel("测试"), classOf[String]).get.body == "OK")
    assert(client.putSync[String]("/test1/1/", TestModel("测试"), classOf[String]).get.body == "OK")
    assert(client.deleteSync[String]("/test1/1/", classOf[String]).get.body == "OK")

    server.shutdown()
    client.shutdown()

    val server1 = RPC.server.setPort(808).setChannel(channel).setPreExecuteInterceptor({
      (method, uri, parameters, InterceptorInfo) =>
        if (method == Method.PUT && uri == "/prefix/test2/:id/") {
          Resp.unAuthorized("认证失败！")
        } else {
          Resp.success(User("1", "admin"))
        }
    }).startup().autoBuilding("com.ecfront.ez.framework.rpc")

    Thread.sleep(1000)

    val client1 = RPC.client.setPort(808).setChannel(channel).startup()

    assert(client1.getSync[TestModel]("/test1/1/", classOf[TestModel]).get.body.name == "测试")
    assert(client1.postSync[String]("/test1/", TestModel("测试"), classOf[String]).get.body == "OK")
    assert(client1.putSync[String]("/test1/1/", TestModel("测试"), classOf[String]).get.body == "OK")
    assert(client1.deleteSync[String]("/test1/1/", classOf[String]).get.body == "OK")
    assert(client1.getSync[TestModel]("/prefix/test2/1/", classOf[TestModel]).get.body.name == "测试")
    assert(client1.putSync[String]("/prefix/test2/1/", TestModel("测试"), classOf[String]).get.message == "认证失败！")
    assert(client1.postSync[String]("/prefix/test2/", TestModel("测试"), classOf[String]).get.body == "OK")
    assert(client1.deleteSync[String]("/prefix/test2/1/", classOf[String]).get.body == "OK")

    server1.shutdown()
    client1.shutdown()

    val server2 = RPC.server.setPort(8088).setChannel(channel).setFormatUri("/base" + _).setPreExecuteInterceptor({
      (method, uri, parameters, interceptorInfo) =>
        assert(!parameters.contains("auth"))
        assert(!parameters.contains("version"))
        assert(interceptorInfo("auth") == "123456")
        assert(interceptorInfo("version") == "1.0")
        if (method == Method.PUT && uri == "/base/prefix/test2/:id/") {
          Resp.unAuthorized("认证失败！")
        } else {
          Resp.success(User("1", "admin"))
        }
    }).setPostExecuteInterceptor({
      result =>
        result
    }).startup().autoBuilding(BService())

    Thread.sleep(1000)

    val client2 = RPC.client.setPort(8088).setChannel(channel).setPreExecuteInterceptor({
      (method, uri, inject) =>
        Resp.success(Map("auth" -> "123456", "version" -> "1.0"))
    }).startup()

    assert(client2.getSync[TestModel]("/base/prefix/test2/1/", classOf[TestModel]).get.body.name == "测试")
    assert(client2.postSync[String]("/base/prefix/test2/", TestModel("测试"), classOf[String]).get.body == "OK")
    assert(client2.putSync[String]("/base/prefix/test2/1/", TestModel("测试"), classOf[String]).get.message == "认证失败！")
    assert(client2.deleteSync[String]("/base/prefix/test2/1/", classOf[String]).get.body == "OK")

    server2.shutdown()
    client2.shutdown()

  }

}

@RPC("")
@EVENT_BUS
@HTTP
@WEB_SOCKETS
object AService {

  @POST("/test1/")
  def postTest(parameter: Map[String, String], req: TestModel, inject: Option[Any]): Resp[String] = {
    assert(req.name == "测试")
    Resp.success("OK")
  }

  @PUT("/test1/:id/")
  def putTest(parameter: Map[String, String], req: TestModel, inject: Option[Any]): Resp[String] = {
    assert(parameter("id") == "1")
    Resp.success("OK")
  }

  @GET("/test1/:id/")
  def getTest(parameter: Map[String, String], inject: Option[Any]): Resp[TestModel] = {
    assert(parameter("id") == "1")
    Resp.success(TestModel("测试"))
  }

  @DELETE("/test1/:id/")
  def deleteTest(parameter: Map[String, String], inject: Option[Any]): Resp[String] = {
    assert(parameter("id") == "1")
    Resp.success("OK")
  }
}

@RPC("/prefix/")
case class BService() {

  @POST("test2/")
  @EVENT_BUS
  @HTTP
  @WEB_SOCKETS
  def postTest(parameter: Map[String, String], req: TestModel, inject: Option[User]): Resp[String] = {
    assert(req.name == "测试")
    assert(inject.get.id == "1")
    assert(inject.get.name == "admin")
    Resp.success("OK")
  }

  @PUT("test2/:id/")
  @EVENT_BUS
  @HTTP
  @WEB_SOCKETS
  def putTest(parameter: Map[String, String], req: TestModel, inject: Option[User]): Resp[String] = {
    //不应该进入
    assert(1 == 2)
    Resp.success("OK")
  }

  @GET("test2/:id/")
  @EVENT_BUS
  @HTTP
  @WEB_SOCKETS
  def getTest(parameter: Map[String, String], inject: Option[User]): Resp[TestModel] = {
    assert(parameter("id") == "1")
    assert(inject.get.id == "1")
    assert(inject.get.name == "admin")
    Resp.success(TestModel("测试"))
  }

  @DELETE("test2/:id/")
  @EVENT_BUS
  @HTTP
  @WEB_SOCKETS
  def deleteTest(parameter: Map[String, String], inject: Option[User]): Resp[String] = {
    assert(parameter("id") == "1")
    assert(inject.get.id == "1")
    assert(inject.get.name == "admin")
    Resp.success("OK")
  }

}

case class User(id: String, name: String)
