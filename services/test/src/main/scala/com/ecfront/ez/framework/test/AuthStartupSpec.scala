package com.ecfront.ez.framework.test

import com.ecfront.ez.framework.core.EZManager

trait AuthStartupSpec extends GatewayStartupSpec {

  lazy val authRpcPackage: String = "com.ecfront.ez"

  lazy val authConfig: String =
    s"""
       | {
       |        "customLogin": false,
       |        "defaultOrganizationCode": "",
       |        "loginKeepSeconds": 0
       | }
     """.stripMargin

  lazy val jdbcConfig: String =
    s"""
       | {
       |        "url": "jdbc:mysql://127.0.0.1:3306/ez?characterEncoding=UTF-8&autoReconnect=true",
       |        "userName": "root",
       |        "password": "123456"
       | }
     """.stripMargin

  override def before(): Unit = {
    startGateway
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
         |    "cache": $cacheConfig,
         |    "rpc": {
         |      "package": "$authRpcPackage"
         |    },
         |    "services": {
         |      "auth":$authConfig,
         |      "jdbc":$jdbcConfig
         |    }
         |  },
         |  "args": {
         |  }
         |}
       """.stripMargin)
    EZManager.start()
  }

}
