package com.ecfront.ez.framework.service.auth.test

import com.ecfront.ez.framework.core.EZManager
import com.ecfront.ez.framework.core.test.BasicSpec

class AuthStartupSpec extends BasicSpec {

  def cacheAddress:String = "127.0.0.1:6379"

  before {
    EZManager.start(
      s"""
         |{
         |  "ez": {
         |    "app": "",
         |    "module": "",
         |    "cache": {
         |      "address": "$cacheAddress"
         |    },
         |    "rpc":{
         |      "package":"com.ecfront.ez"
         |    },
         |    "services": {
         |      "gateway": {
         |        "host": "0.0.0.0",
         |        "port": 8080,
         |        "wsPort": 8081,
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
    EZManager.start(
      s"""
         |{
         |  "ez": {
         |    "app": "",
         |    "module": "",
         |    "cache": {
         |      "address": "$cacheAddress"
         |    },
         |    "rpc": {
         |      "package": "com.ecfront.ez"
         |    },
         |    "services": {
         |      "auth": {
         |        "customLogin": false,
         |        "defaultOrganizationCode": "",
         |        "loginKeepSeconds": 0
         |      },
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
    EZManager.start()
  }

}
