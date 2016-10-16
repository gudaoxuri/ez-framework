package com.ecfront.ez.framework.test

import com.ecfront.common.Resp
import com.ecfront.ez.framework.core.EZManager

trait GatewayStartupSpec extends MockStartupSpec{

  def cacheConfig: String =
    s"""
       |{
       |      "address": "127.0.0.1:6379"
       |}
   """.stripMargin

  def gatewayRpcPackage: String = "com.ecfront.ez"

  def gatewayPort: Int = 8080

  def gatewayWSPort: Int = 8081

  protected def startGateway: Resp[String] = {
    EZManager.start(
      s"""
         |{
         |  "ez": {
         |    "app": "",
         |    "module": "",
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
