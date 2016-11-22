package com.ecfront.ez.framework.service.gateway

import java.io.File
import java.util.Date
import java.util.concurrent.CountDownLatch

import com.ecfront.common.{JsonHelper, StandardCode}
import com.ecfront.ez.framework.core.rpc._
import com.ecfront.ez.framework.core.{EZ, EZManager}
import com.ecfront.ez.framework.service.jdbc.Page
import com.ecfront.ez.framework.service.other.EZ_Test
import com.ecfront.ez.framework.test.BasicSpec
import org.joox.JOOX._

class GatewaySpec extends BasicSpec {

  test("gateway service test") {
    EZManager.start(
      s"""
         |{
         |  "ez": {
         |    "app": "",
         |    "module": "",
         |    "cluster":{
         |      "userName":"user",
         |      "password":"password",
         |      "host":"127.0.0.1",
         |      "port":5672,
         |      "virtualHost":"ez",
         |      "defaultTopicExchangeName":"ex_topic",
         |      "defaultRPCExchangeName":"ex_rpc",
         |      "defaultQueueExchangeName":"ex_queue"
         |    },
         |    "cache": {
         |      "address": "127.0.0.1:6379"
         |    },
         |    "rpc":{
         |      "package":"com.ecfront.ez.framework.service.gateway"
         |    },
         |    "services": {
         |      "gateway": {
         |        "host": "0.0.0.0",
         |        "port": 8080,
         |        "wsPort": 8081,
         |        "monitor": {
         |          "slow": {
         |            "time": 100,  // 最慢时间，单位毫秒
         |            "includes": [],
         |            "excludes": []
         |          }
         |        },
         |        /*"antiDDoS":{
         |          "reqRatePerMinute":100,
         |          "illegalReqRatePerMinute":100
         |        },*/
         |        "metrics":{},
         |        "publicUriPrefix":"/public/",
         |        "resourcePath": "c:/tmp/",
         |        "accessControlAllowOrigin": "*"
         |      }
         |    }
         |  },
         |  "args": {
         |  }
         |}
       """.stripMargin)
    new CountDownLatch(1).await()
  }

  test("gateway client test") {
    EZManager.start(
      s"""
         |{
         |  "ez": {
         |    "app": "",
         |    "module": "",
         |    "cluster":{
         |      "userName":"user",
         |      "password":"password",
         |      "host":"127.0.0.1",
         |      "port":5672,
         |      "virtualHost":"ez",
         |      "defaultTopicExchangeName":"ex_topic",
         |      "defaultRPCExchangeName":"ex_rpc",
         |      "defaultQueueExchangeName":"ex_queue"
         |    },
         |    "cache": {
         |      "address": "127.0.0.1:6379"
         |    },
         |    "rpc":{
         |      "package":"com.ecfront.ez.framework.service.other"
         |    },
         |    "services": {
         |      "jdbc": {
         |        "url": "jdbc:mysql://127.0.0.1:3306/ez?characterEncoding=UTF-8&autoReconnect=true",
         |        "userName": "root",
         |        "password": "123456"
         |      }
         |    }
         |  },
         |  "args": {
         |  }
         |}
       """.stripMargin)
   /* val result = HttpClientProcessor.post(U("test/file/"),
      ReqFile(new File(this.getClass.getResource("/").getPath + "IMG_20160403_195547.jpg"), "photo"))
    println(result)*/
    addAuthInfo()
    simpleRPCTest()
    xmlTest()
    /* fileTest()*/
    authTest()
    // httpsTest("127.0.0.1", 8081)
    println("====================\r\n性能测试\r\n====================")
    performanceTest()
    println("====================\r\n手工测试\r\n====================")
    new CountDownLatch(1).await()

    /**
      * 下载文件
      * http://127.0.0.1:8080/test/downfile/?__ez_token_=testToken
      * WS
      * 打开ws.html
      **/
  }

  def U(path: String): String = {
    s"http://127.0.0.1:8080/$path?__ez_token__=testToken"
  }

  def addAuthInfo(): Unit = {
    val opt = OptInfo("testToken", "testAccCode", "testLoginId", "testName", "", "", "", "testOrg", "", Set("user"), new Date(), "", "")
    EZ.cache.set(RPCProcessor.TOKEN_INFO_FLAG + "testToken", JsonHelper.toJsonString(opt))
    assert(RespHttpClientProcessor.get[EZ_Test](U("test/1/")).message.contains("Organization"))
    EZ.eb.publish("/ez/auth/rbac/organization/add/", s"""{"code":""}""")
    Thread.sleep(100)
  }

  def simpleRPCTest(): Unit = {
    EZ_Test.deleteByCond("")
    // get
    assert(RespHttpClientProcessor.get[EZ_Test](U("test/1/")).body == null)
    // list
    assert(RespHttpClientProcessor.get[List[EZ_Test]](U("test/")).body.isEmpty)
    // save
    val res = EZ_Test()
    // 缺少必填
    assert(RespHttpClientProcessor.post[EZ_Test](U("test/"), res).code == StandardCode.BAD_REQUEST)
    var test = RespHttpClientProcessor.post[EZ_Test](U("test/"), EZ_Test("a", "aa")).body
    assert(test.id != null && test.bus_uuid != null && test.code == "a" && test.name == "aa"
      && test.create_org == "" && test.create_time != 0 && test.create_user == "testAccCode"
      && test.update_org == "" && test.update_time != 0 && test.update_user == "testAccCode"
      && test.create_time == test.update_time)
    // 唯一性检查
    assert(RespHttpClientProcessor.post[EZ_Test](U("test/"), EZ_Test("a", "aa")).code == StandardCode.BAD_REQUEST)

    // update
    test.name = "a_name"
    test = RespHttpClientProcessor.put[EZ_Test](U(s"test/${test.id}/"), test).body
    assert(test.id != null && test.bus_uuid != null && test.code == "a" && test.name == "a_name"
      && test.create_time != test.update_time)
    test.code = "aaaa"
    test.name = "aa_name"
    test = RespHttpClientProcessor.put[EZ_Test](U(s"test/uuid/${test.bus_uuid}/"), test).body
    assert(test.id != null && test.bus_uuid != null && test.code == "aaaa" && test.name == "aa_name")

    // get
    test = RespHttpClientProcessor.get[EZ_Test](U(s"test/${test.id}/")).body
    assert(test.id != null && test.bus_uuid != null && test.code == "aaaa" && test.name == "aa_name")
    test = RespHttpClientProcessor.get[EZ_Test](U(s"test/uuid/${test.bus_uuid}/")).body
    assert(test.id != null && test.bus_uuid != null && test.code == "aaaa" && test.name == "aa_name")

    // disable & enable
    RespHttpClientProcessor.get[EZ_Test](U(s"test/${test.id}/disable/"))
    assert(!RespHttpClientProcessor.get[EZ_Test](U(s"test/${test.id}/")).body.enable)
    RespHttpClientProcessor.get[EZ_Test](U(s"test/${test.id}/enable/"))
    assert(RespHttpClientProcessor.get[EZ_Test](U(s"test/${test.id}/")).body.enable)
    RespHttpClientProcessor.get[EZ_Test](U(s"test/uuid/${test.bus_uuid}/disable/"))
    assert(!RespHttpClientProcessor.get[EZ_Test](U(s"test/uuid/${test.bus_uuid}/")).body.enable)
    RespHttpClientProcessor.get[EZ_Test](U(s"test/uuid/${test.bus_uuid}/enable/"))
    assert(RespHttpClientProcessor.get[EZ_Test](U(s"test/uuid/${test.bus_uuid}/")).body.enable)

    // delete
    RespHttpClientProcessor.delete[EZ_Test](U(s"test/uuid/${test.bus_uuid}/"))
    assert(RespHttpClientProcessor.get[EZ_Test](U(s"test/uuid/${test.bus_uuid}/")).body == null)

    // list
    HttpClientProcessor.post(U("test/"), EZ_Test("1", "1s"))
    HttpClientProcessor.post(U("test/"), EZ_Test("2", "2s"))
    HttpClientProcessor.post(U("test/"), EZ_Test("3", "3s"))
    HttpClientProcessor.post(U("test/"), EZ_Test("4", "4s"))
    HttpClientProcessor.post(U("test/"), EZ_Test("5", "5s", enable = false))

    var list = RespHttpClientProcessor.get[List[EZ_Test]](U("test/")).body
    assert(list.length == 5)
    list = RespHttpClientProcessor.get[List[EZ_Test]](U("test/enable/")).body
    assert(list.length == 4)
    var page = RespHttpClientProcessor.get[Page[EZ_Test]](U("test/page/2/2/")).body
    assert(page.pageNumber == 2)
    assert(page.pageSize == 2)
    assert(page.pageTotal == 3)
    assert(page.recordTotal == 5)
    assert(page.objects.size == 2)
    page = RespHttpClientProcessor.get[Page[EZ_Test]](U("test/enable/page/2/2/")).body
    assert(page.pageNumber == 2)
    assert(page.pageSize == 2)
    assert(page.pageTotal == 2)
    assert(page.recordTotal == 4)
    assert(page.objects.size == 2)

  }

  def xmlTest(): Unit = {
    var xmlStr = HttpClientProcessor.get(U("test/xml/str/"), "text/xml; charset=utf-8")
    assert($(xmlStr).find("city").size() > 0)
    xmlStr = HttpClientProcessor.post(U("test/xml/str/"), $(xmlStr).document(), "text/xml; charset=utf-8")
    assert($(xmlStr).find("city").size() > 0)
    xmlStr = HttpClientProcessor.post(U("test/xml/str"), $(xmlStr).document(), "text/xml; charset=utf-8")
    assert($(xmlStr).find("city").size() > 0)
    xmlStr = HttpClientProcessor.post(U("test/xml/str/error/"), $(xmlStr).document(), "text/xml; charset=utf-8")
    assert($(xmlStr).find("error").size() > 0)
    xmlStr = HttpClientProcessor.get(
      "http://flash.weather.com.cn:80/wmaps/xml/china.xml", "text/xml; charset=utf-8")
    assert($(xmlStr).find("city").size() > 0)
  }

  def fileTest(): Unit = {
    val result = HttpClientProcessor.post(U("test/file/"),
      ReqFile(new File(this.getClass.getResource("/").getPath + "IMG_20160403_195547.jpg"), "photo"))
    println(result)
  }

  def httpsTest(): Unit = {
    val page = RespHttpClientProcessor.get[Page[EZ_Test]](U("test/enable/page/2/2/")).body
    assert(page.pageNumber == 2)
    assert(page.pageSize == 2)
    assert(page.pageTotal == 2)
    assert(page.recordTotal == 4)
    assert(page.objects.size == 2)
  }

  def authTest(): Unit = {
    assert(RespHttpClientProcessor.get[EZ_Test]("http://127.0.0.1:8080/test/1/").message.contains("【token】not exist"))
    assert(RespHttpClientProcessor.get[EZ_Test]("http://127.0.0.1:8080/test/1/?__ez_token__=abc").message.contains("Token NOT exist"))
    assert(RespHttpClientProcessor.get[EZ_Test](U("test/1/")))

    EZ.eb.publish("/ez/auth/rbac/resource/add/", Map("method" -> "*", "uri" -> "/test/*"))
    Thread.sleep(1000)
    assert(RespHttpClientProcessor.get[EZ_Test](U("test/1/")).message.contains("no access to"))
    EZ.eb.publish("/ez/auth/rbac/role/add/", s"""{"code":"user","resource_codes":["*@/test/*"]}""")
    Thread.sleep(1000)
    assert(RespHttpClientProcessor.get[EZ_Test](U("test/1/")))

    EZ.eb.publish("/ez/auth/rbac/role/remove/", s"""{"code":"user"}""")
    Thread.sleep(1000)
    assert(RespHttpClientProcessor.get[EZ_Test](U("test/1/")).message.contains("no access to"))
    EZ.eb.publish("/ez/auth/rbac/resource/remove/", Map("code" -> "*@/test/*"))
    Thread.sleep(1000)
    assert(RespHttpClientProcessor.get[EZ_Test](U("test/1/")))
  }

  def performanceTest(): Unit = {
    val c = new CountDownLatch(4000 + 500)
    val threads = for (i <- 0 until 2000)
      yield new Thread(new Runnable {
        override def run(): Unit = {
          val resp = RespHttpClientProcessor.post[EZ_Test](U("test/"), EZ_Test(System.nanoTime() + i + "", "perf1"))
          if (!resp) {
            logger.error(JsonHelper.toJsonString(resp))
            throw new Exception("")
          }
          c.countDown()
        }
      })
    threads.foreach(_.start())
    Thread.sleep(10000)
    for (i <- 0 until 2000)
      yield new Thread(new Runnable {
        override def run(): Unit = {
          val resp = RespHttpClientProcessor.post[EZ_Test](U("test/"), EZ_Test(System.nanoTime() + i + "", "perf2"))
          if (!resp) {
            logger.error(JsonHelper.toJsonString(resp))
            throw new Exception("")
          }
          c.countDown()
        }
      }).start()
    for (i <- 0 until 500)
      yield new Thread(new Runnable {
        override def run(): Unit = {
          assert(RespHttpClientProcessor.get[String](U("test/longtime/")).body == "ok")
          c.countDown()
        }
      }).start()
    c.await()
  }

}


