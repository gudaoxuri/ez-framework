package com.ecfront.ez.framework.gateway

import com.ecfront.common.Resp
import com.ecfront.ez.framework.core.rpc.{DownloadFile, GET, POST, RPC}
import com.ecfront.ez.framework.service.jdbc.BaseStorage
import com.ecfront.ez.framework.service.jdbc.scaffold.SimpleRPCService
import org.joox.JOOX._
import org.w3c.dom.Document

@RPC("/resource/")
object ResourceService extends SimpleRPCService[EZ_Resource] {

  @POST("test/:s-s/:Sd")
  def test(parameter: Map[String, String], body: List[EZ_Resource]): Resp[String] = {
    // 泛型，error
    Resp.success("")
  }

  @GET("xml/")
  def testgetXml(parameter: Map[String, String]): Resp[Document] = {
    Resp.success($(
      s"""
         |<?xml version="1.0"?>
         |<china dn="day">
         |<city quName="黑龙江" pyName="heilongjiang" cityname="哈尔滨" state1="0" state2="0" stateDetailed="晴" tem1="7" tem2="-4" windState="北风3-4级转小于3级"/>
         |</china>
       """.stripMargin
    ).document())
  }

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

  @POST("xml/")
  def testPostXml(parameter: Map[String, String], body: Document): Resp[Document] = {
    Resp.success(body)
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

  @GET("longtime/")
  def longTime(parameter: Map[String, String]): Resp[String] = {
    Thread.sleep(10000000)
    Resp.success("ok")
  }

  @GET("downfile/")
  def downFile(parameter: Map[String, String]): Resp[DownloadFile] = {
    Resp.success(DownloadFile(this.getClass.getResource("/").getPath + "logback.xml", "logback.xml"))
  }

  override protected val storageObj: BaseStorage[EZ_Resource] = EZ_Resource
}