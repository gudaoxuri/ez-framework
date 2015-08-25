package com.ecfront.ez.framework.rpc

import java.util.concurrent.CountDownLatch

import com.ecfront.common.{JsonHelper, Resp}
import com.ecfront.ez.framework.rpc.RPC.EChannel
import com.ecfront.ez.framework.rpc.RPC.EChannel.EChannel
import org.jsoup.nodes.Document
import org.scalatest.FunSuite

import scala.beans.BeanProperty

class FunSpec extends FunSuite {

  test("功能测试") {
    jsonFunTest(EChannel.HTTP)
    jsonFunTest(EChannel.EVENT_BUS)
    jsonFunTest(EChannel.WEB_SOCKETS)
    xmlFunTest()
  }

  def jsonFunTest(channel: EChannel) {

    println(s"===============Start ${channel.toString}===================")
    println(s"=====Start Server=====")

    val latch = new CountDownLatch(8)

    val server = RPC.server.setHost("127.0.0.1").setPort(808).setChannel(channel).setAny({
      (method, uri, parameters, body, inject) =>
        assert(method == "PUT")
        assert(uri == "/index/not/1/?a=1")
        assert(JsonHelper.toObject(body, classOf[TestModel]).name == "测试")
        Resp.success(body)
    }).startup()
      .get("/number/", {
      (param, _, _) =>
        Resp.success(1L)
    }).get("/boolean/", {
      (param, _, _) =>
        Resp.success(true)
    }).get("/index/", {
      (param, _, _) =>
        assert(param("a") == "1")
        Resp.success("完成")
    }).post[String]("/index/", classOf[String], {
      (param, body, _) =>
        Resp.success(body)
    }).put[TestModel]("/index/:id/", classOf[TestModel], {
      (param, body, _) =>
        assert(body.name == "测试")
        assert(param.get("id").get == "test")
        Resp.success(body)
    }).put[TestModel]("/custom/:id/", classOf[TestModel], {
      (param, body, _) =>
        assert(body.name == "测试")
        assert(param.get("id").get == "test")
        //Result custom type
        body
    }).put[List[TestIdModel]]("/test/", classOf[List[TestIdModel]], {
      (param, body, _) =>
        val res = JsonHelper.toGenericObject[List[TestIdModel]](body)
        assert(res.head.cid == "1")
        assert(res.head.createTime == 123456789)
        assert(res.head.name == "sunisle")
        Resp.success(res)
    })

    val m = TestIdModel()
    m.cid = "1"
    m.createTime = 123456789
    m.name = "sunisle"

    Thread.sleep(5000)

    println(s"=====Start Async Client=====")

    val client = RPC.client.setHost("127.0.0.1").setPort(808).setChannel(channel).startup()
      .get[Long]("/number/", classOf[Long], {
      result =>
        assert(result.code == "200")
        assert(result.body == 1L)
        latch.countDown()
    }).get[Boolean]("/boolean/", classOf[Boolean], {
      result =>
        assert(result.code == "200")
        assert(result.body)
        latch.countDown()
    }).get[String]("/index/?a=1", classOf[String], {
      result =>
        assert(result.code == "200")
        assert(result.body == "完成")
        latch.countDown()
    }).post[String]("/index/", "测试", classOf[String], {
      result =>
        assert(result.code == "200")
        assert(result.body == "测试")
        latch.countDown()
    }).put[TestModel]("/index/test/", TestModel("测试"), classOf[TestModel], {
      result =>
        assert(result.code == "200")
        assert(result.body.name == "测试")
        latch.countDown()
    }).put[List[TestIdModel]]("/test/", List(m), classOf[List[TestIdModel]], {
      result =>
        //TODO Can‘t package TestIdModel with list
        val res = JsonHelper.toGenericObject[List[TestIdModel]](result.body)
        assert(res.head.cid == "1")
        assert(res.head.createTime == 123456789)
        assert(res.head.name == "sunisle")
        latch.countDown()
    }).put[TestModel]("/index/test/", TestModel("测试"))
    if (channel != EChannel.EVENT_BUS) {
      client.put[TestModel]("/index/not/1/?a=1", TestModel("测试"), classOf[TestModel], {
        result =>
          assert(result)
          latch.countDown()
      })
    } else {
      latch.countDown()
    }

    //raw json
    client.raw.put[TestModel]("/custom/test/", TestModel("测试"), classOf[TestModel], {
      result =>
        assert(result.name == "测试")
        latch.countDown()
    })

    latch.await()

    println(s"=====Start Sync Client=====")

    assert(client.getSync[Long]("/number/", classOf[Long]).get.body == 1L)
    assert(client.getSync[Boolean]("/boolean/", classOf[Boolean]).get.body)
    assert(client.getSync[String]("/index/?a=1", classOf[String]).get.body == "完成")
    assert(client.postSync[String]("/index/", "测试", classOf[String]).get.body == "测试")
    assert(client.putSync[TestModel]("/index/test/", TestModel("测试"), classOf[TestModel]).get.body.name == "测试")
    client.putSync[TestModel]("/index/test/", TestModel("测试"))
    assert(client.raw.putSync[TestModel]("/custom/test/", TestModel("测试"), classOf[TestModel]).get.name == "测试")

    println(s"=====Finish=====")

    server.shutdown()
    client.shutdown()
  }

  def xmlFunTest(): Unit = {
    val server = RPC.server.setPort(3001).startup()
      .put[Document]("/custom/:id/", classOf[Document], {
      (param, body, _) =>
        assert(body.select("city").size()>0)
        body
    })
    //raw xml must channel=false and request class = scala.xml.Node
    val latch = new CountDownLatch(1)
    val xmlClient = RPC.client.setPort(3001).startup().raw
    xmlClient.get[Document]("http://flash.weather.com.cn:80/wmaps/xml/china.xml", classOf[Document], {
      result =>
        assert(result.select("city").size()>0)
        xmlClient.put[Document]("/custom/test/",result.outerHtml(), classOf[Document], {
          result2 =>
            assert(result2.select("city").size()>0)
            latch.countDown()
        })
    })
    latch.await()
    server.shutdown()
    xmlClient.shutdown()
  }
}


case class TestModel(name: String)

trait Id {
  @BeanProperty var cid: String = _
}

trait Ext extends Id {
  @BeanProperty var createTime: Long = _
}

case class TestIdModel() extends Ext {
  @BeanProperty var name: String = _
}

