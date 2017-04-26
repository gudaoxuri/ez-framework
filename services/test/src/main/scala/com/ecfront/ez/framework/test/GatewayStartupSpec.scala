package com.ecfront.ez.framework.test

import com.ecfront.common.Resp
import com.ecfront.ez.framework.core.EZManager

trait GatewayStartupSpec extends MockStartupSpec{

  lazy val cacheConfig =
    s"""
       |{
       |      "address": "127.0.0.1:6379"
       |}
   """.stripMargin

  lazy val clusterConfig: String =
    """
      |{
      |      "userName":"user",
      |      "password":"password",
      |      "host":"127.0.0.1",
      |      "port":5672,
      |      "virtualHost":"ez",
      |      "defaultTopicExchangeName":"ex_topic",
      |      "defaultRPCExchangeName":"ex_rpc",
      |      "defaultQueueExchangeName":"ex_queue"
      |}
    """.stripMargin

  lazy val gatewayRpcPackage: String = "com.ecfront.ez"

  lazy val gatewayPort: Int = 8080

  lazy val gatewayWSPort: Int = 8081

  protected def startGateway: Resp[String] = {
    EZManager.start(
      s"""
         |{
         |  "ez": {
         |    "app": "",
         |    "module": "",
         |    "cluster":$clusterConfig,
         |    "cache": $cacheConfig,
         |    "rpc":{
         |      "package":"$gatewayRpcPackage"
         |    },
         |    "services": {
         |      "gateway": {
         |        "host": "0.0.0.0",
         |        "port": $gatewayPort,
         |        "wsPort": $gatewayWSPort,
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
  }

  override def before(): Unit = {
    startGateway
    super.before()
  }

}
