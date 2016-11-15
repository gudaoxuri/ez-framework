package com.ecfront.ez.framework.core.rpc

import java.io.File
import java.net.SocketException
import java.util.Date

import com.ecfront.common.JsonHelper
import com.ecfront.ez.framework.core.logger.Logging
import com.ecfront.ez.framework.core.rpc.Method.Method
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods._
import org.apache.http.entity.{FileEntity, StringEntity}
import org.apache.http.impl.client.{CloseableHttpClient, HttpClients}
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager
import org.apache.http.message.BasicNameValuePair
import org.apache.http.util.EntityUtils
import org.apache.http.{HttpHeaders, MalformedChunkCodingException, NameValuePair, NoHttpResponseException}
import org.joox.JOOX._
import org.w3c.dom.Document

/**
  * HTTP 请求操作
  *
  * 包含了对HTTP GET POST PUT DELETE 四类常用操作
  *
  */
object HttpClientProcessor extends Logging {

  // TODO
  private val cm = new PoolingHttpClientConnectionManager()
  cm.setMaxTotal(100)
  cm.setDefaultMaxPerRoute(100)

  private val httpClient: CloseableHttpClient = HttpClients.custom().setConnectionManager(cm).build()

  def init(): Unit = {
  }

  /**
    * GET 请求
    *
    * @param url         请求URL
    * @param contentType 请求类型，默认为 application/json; charset=utf-8
    * @return 请求结果，string类型
    */
  def get(url: String, contentType: String = "application/json; charset=utf-8", header: Map[String, String] = Map()): String = {
    request(Method.GET, url, null, contentType, header)
  }

  /**
    * POST 请求
    *
    * @param url         请求URL
    * @param body        请求体
    * @param contentType 请求类型，默认为 application/json; charset=utf-8
    * @return 请求结果，string类型
    */
  def post(url: String, body: Any, contentType: String = "application/json; charset=utf-8", header: Map[String, String] = Map()): String = {
    request(Method.POST, url, body, contentType, header)
  }

  /**
    * PUT 请求
    *
    * @param url         请求URL
    * @param body        请求体
    * @param contentType 请求类型，默认为 application/json; charset=utf-8
    * @return 请求结果，string类型
    */
  def put(url: String, body: Any, contentType: String = "application/json; charset=utf-8", header: Map[String, String] = Map()): String = {
    request(Method.PUT, url, body, contentType, header)
  }

  /**
    * DELETE 请求
    *
    * @param url         请求URL
    * @param contentType 请求类型，默认为 application/json; charset=utf-8
    * @return 请求结果，string类型
    */
  def delete(url: String, contentType: String = "application/json; charset=utf-8", header: Map[String, String] = Map()): String = {
    request(Method.DELETE, url, null, contentType, header)
  }

  private[core] def request(methodStr: Method, url: String, body: Any, contentType: String, header: Map[String, String], retry: Int = 0): String = {
    val method = methodStr match {
      case Method.GET => new HttpGet(url)
      case Method.POST => new HttpPost(url)
      case Method.PUT => new HttpPut(url)
      case Method.DELETE => new HttpDelete(url)
    }
    val realContextType = getRealContextType(body, contentType)
    logger.debug(s"HTTP [${method.getMethod}] request : ${method.getURI}")
    if (header != null && header.nonEmpty) {
      header.foreach(h => method.addHeader(h._1, h._2))
    }
    if (contentType != null) {
      method.setHeader(HttpHeaders.CONTENT_TYPE, realContextType)
    }
    method.setHeader(HttpHeaders.ACCEPT_ENCODING, "/")
    if (body != null) {
      val entity = realContextType.toLowerCase match {
        case t if t.toLowerCase.contains("application/x-www-form-urlencoded") && body.isInstanceOf[Map[_, _]] =>
          val m = new java.util.ArrayList[NameValuePair]()
          body.asInstanceOf[Map[String, Any]].foreach {
            entry =>
              m.add(new BasicNameValuePair(entry._1, entry._2.toString))
          }
          new UrlEncodedFormEntity(m, "UTF-8")
        case t if t.toLowerCase.contains("xml") =>
          body match {
            case b: Document =>
              new StringEntity($(b).toString, "UTF-8")
            case b: String =>
              new StringEntity(b, "UTF-8")
            case _ =>
              logger.error(s"Not support return type [${body.getClass.getName}] by xml")
              new StringEntity("", "UTF-8")
          }
        case t if t.toLowerCase.contains("multipart/form-data") =>
          body match {
            case reqFile: ReqFile =>
              val finalFileName = if (reqFile.fileName == null) {
                reqFile.file.getName.substring(0, reqFile.file.getName.lastIndexOf(".")) + "_" + System.nanoTime() + "." + reqFile.file.getName.substring(reqFile.file.getName.lastIndexOf(".") + 1)
              } else {
                reqFile.fileName
              }
              method.setHeader("Content-Disposition", "form-data; name=\"" + reqFile.fieldName + "\"; filename=\"" + finalFileName)
              /*method.setHeader("Content-Type", "application/octet-stream")*/
              method.setHeader("Content-Transfer-Encoding", "binary")
              /*              buffer.appendString("Content-Disposition: form-data; name=\"" + fieldName + "\"; filename=\"" + finalFileName + "\"\r\n")
                            buffer.appendString("Content-Type: application/octet-stream\r\n")
                            buffer.appendString("Content-Transfer-Encoding: binary\r\n")*/
              new FileEntity(reqFile.file)
            case _ =>
              new FileEntity(body.asInstanceOf[File])
          }
        case _ =>
          val str = body match {
            case b: String =>
              body.asInstanceOf[String]
            case b if b.isInstanceOf[Int] || b.isInstanceOf[Long] ||
              b.isInstanceOf[Float] || b.isInstanceOf[Double] ||
              b.isInstanceOf[Char] || b.isInstanceOf[Short] ||
              b.isInstanceOf[BigDecimal] || b.isInstanceOf[Boolean] ||
              b.isInstanceOf[Byte] || b.isInstanceOf[Date]
            =>
              b.toString
            case _ =>
              JsonHelper.toJsonString(body)
          }
          new StringEntity(str, "UTF-8")
      }
      method.asInstanceOf[HttpEntityEnclosingRequestBase].setEntity(entity)
    }

    var response: CloseableHttpResponse = null
    try {
      response = httpClient.execute(method)
      EntityUtils.toString(response.getEntity, "UTF-8")
    } catch {
      case e if e.getClass == classOf[SocketException]
        || e.getClass == classOf[NoHttpResponseException]
        || e.getClass == classOf[MalformedChunkCodingException] =>
        // 同络错误重试5次
        if (retry <= 5) {
          Thread.sleep(500)
          logger.warn(s"HTTP [${method.getMethod}] request  ${method.getURI} ERROR. retry [${retry + 1}] .")
          request(methodStr, url, body, contentType, header, retry + 1)
        } else {
          logger.warn(s"HTTP [${method.getMethod}] request : ${method.getURI} ERROR.", e)
          throw e
        }
      case e: Exception =>
        logger.warn(s"HTTP [${method.getMethod}] request : ${method.getURI} ERROR.", e)
        throw e
    } finally {
      if (response != null) {
        response.close()
      }
    }
  }

  private def getRealContextType(body: Any, contentType: String): String = {
    if (body != null && body.isInstanceOf[File]) {
      "multipart/form-data"
    } else if (body != null && body.isInstanceOf[ReqFile]) {
      "multipart/form-data; boundary=ez_boundary"
    } else {
      contentType
    }
  }

}

