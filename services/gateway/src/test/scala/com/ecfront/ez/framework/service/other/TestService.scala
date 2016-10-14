package com.ecfront.ez.framework.service.other

import com.ecfront.common.Resp
import com.ecfront.ez.framework.core.rpc._
import com.ecfront.ez.framework.service.jdbc.BaseStorage
import com.ecfront.ez.framework.service.jdbc.scaffold.SimpleRPCService

@RPC("/test/")
object TestService extends SimpleRPCService[EZ_Test] {

  @GET("xml/str")
  def testgetXmlStr(parameter: Map[String, String]): Resp[String] = {
    Resp.success(
      s"""
         |<?xml version="1.0"?>
         |<china dn="day">
         |<city quName="黑龙江" pyName="heilongjiang" cityname="哈尔滨" state1="0" state2="0" stateDetailed="晴" tem1="7" tem2="-4" windState="北风3-4级转小于3级"/>
         |</china>
       """.stripMargin)
  }

  @POST("xml/str/")
  def testPostXmlStr(parameter: Map[String, String], body: String): Resp[String] = {
    Resp.success(body)
  }

  @POST("xml/str/error/")
  def testPostXmlStrError(parameter: Map[String, String], body: String): Resp[String] = {
    Resp.badRequest("some error")
  }

  @POST("file/")
  def uploadFile(parameter: Map[String, String], body: String): Resp[String] = {
    Resp.success(body)
  }

  @GET("downfile/")
  def downFile(parameter: Map[String, String]): Resp[DownloadFile] = {
    Resp.success(DownloadFile(this.getClass.getResource("/").getPath + "logback.xml", "logback.xml"))
  }

  @GET("longtime/")
  def longTime(parameter: Map[String, String]): Resp[String] = {
    Thread.sleep(10000)
    Resp.success("ok")
  }

  @WS("")
  def save(parameter: Map[String, String], body: EZ_Test): Resp[EZ_Test] = {
    Resp.success(body)
  }


  override protected val storageObj: BaseStorage[EZ_Test] = EZ_Test
}