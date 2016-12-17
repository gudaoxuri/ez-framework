package com.ecfront.ez.framework.core.rpc

import java.io.File
import java.net.SocketException
import java.util.Date

import com.ecfront.common.JsonHelper
import com.ecfront.ez.framework.core.logger.Logging
import com.ecfront.ez.framework.core.rpc.Method.Method
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods._
import org.apache.http.entity.mime.{HttpMultipartMode, MultipartEntityBuilder}
import org.apache.http.entity.{ContentType, StringEntity}
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
  def get(url: String, contentType: String = "application/json; charset=utf-8", header: Map[String, String] = Map(), charset: String = "UTF-8", timeout: Int = -1): String = {
    request(Method.GET, url, null, contentType, header, charset, timeout)
  }

  /**
    * POST 请求
    *
    * @param url         请求URL
    * @param body        请求体
    * @param contentType 请求类型，默认为 application/json; charset=utf-8
    * @return 请求结果，string类型
    */
  def post(url: String, body: Any, contentType: String = "application/json; charset=utf-8", header: Map[String, String] = Map(), charset: String = "UTF-8", timeout: Int = -1): String = {
    request(Method.POST, url, body, contentType, header, charset, timeout)
  }

  /**
    * PUT 请求
    *
    * @param url         请求URL
    * @param body        请求体
    * @param contentType 请求类型，默认为 application/json; charset=utf-8
    * @return 请求结果，string类型
    */
  def put(url: String, body: Any, contentType: String = "application/json; charset=utf-8", header: Map[String, String] = Map(), charset: String = "UTF-8", timeout: Int = -1): String = {
    request(Method.PUT, url, body, contentType, header, charset, timeout)
  }

  /**
    * DELETE 请求
    *
    * @param url         请求URL
    * @param contentType 请求类型，默认为 application/json; charset=utf-8
    * @return 请求结果，string类型
    */
  def delete(url: String, contentType: String = "application/json; charset=utf-8", header: Map[String, String] = Map(), charset: String = "UTF-8", timeout: Int = -1): String = {
    request(Method.DELETE, url, null, contentType, header, charset, timeout)
  }

  private[core] def request(methodStr: Method, url: String, body: Any, contentType: String, header: Map[String, String], charset: String = "UTF-8", timeout: Int = -1, retry: Int = 0): String = {
    val method = methodStr match {
      case Method.GET => new HttpGet(url)
      case Method.POST => new HttpPost(url)
      case Method.PUT => new HttpPut(url)
      case Method.DELETE => new HttpDelete(url)
    }
    if (timeout != -1) {
      method.setConfig(RequestConfig.custom().setSocketTimeout(timeout).setConnectTimeout(timeout).build())
    }
    val realContextType = getRealContextType(body, contentType)
    logger.debug(s"HTTP [${method.getMethod}] request : ${method.getURI}")
    if (header != null && header.nonEmpty) {
      header.foreach(h => method.addHeader(h._1, h._2))
    }
    if (contentType != null) {
      method.setHeader(HttpHeaders.CONTENT_TYPE, realContextType)
    }
    if (body != null) {
      val entity = realContextType.toLowerCase match {
        case t if t.toLowerCase.contains("application/x-www-form-urlencoded") && body.isInstanceOf[Map[_, _]] =>
          val m = new java.util.ArrayList[NameValuePair]()
          body.asInstanceOf[Map[String, Any]].foreach {
            entry =>
              m.add(new BasicNameValuePair(entry._1, entry._2.toString))
          }
          new UrlEncodedFormEntity(m, charset)
        case t if t.toLowerCase.contains("xml") =>
          body match {
            case b: Document =>
              new StringEntity($(b).toString, charset)
            case b: String =>
              new StringEntity(b, charset)
            case _ =>
              logger.error(s"Not support return type [${body.getClass.getName}] by xml")
              new StringEntity("", charset)
          }
        case t if t.toLowerCase.contains("multipart/form-data") =>
          val (fileName, file, fieldName) = body match {
            case reqFile: ReqFile =>
              val fileName = if (reqFile.fileName == null) {
                reqFile.file.getName.substring(0, reqFile.file.getName.lastIndexOf(".")) + "_" + System.nanoTime() + "." + reqFile.file.getName.substring(reqFile.file.getName.lastIndexOf(".") + 1)
              } else {
                reqFile.fileName
              }
              (fileName, reqFile.file, reqFile.fieldName)
            case _ =>
              val file = body.asInstanceOf[File]
              (file.getName, file, "file")
          }
          method.addHeader("Content-Transfer-Encoding", "binary")
          MultipartEntityBuilder.create()
            .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
            .addBinaryBody(fieldName, file, ContentType.APPLICATION_OCTET_STREAM, fileName)
            .build()
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
          new StringEntity(str, charset)
      }
      method.asInstanceOf[HttpEntityEnclosingRequestBase].setEntity(entity)
    }
    var response: CloseableHttpResponse = null
    try {
      response = httpClient.execute(method)
      EntityUtils.toString(response.getEntity, charset)
    } catch {
      case e if e.getClass == classOf[SocketException]
        || e.getClass == classOf[NoHttpResponseException]
        || e.getClass == classOf[MalformedChunkCodingException] =>
        // 同络错误重试5次
        if (retry <= 5) {
          Thread.sleep(500)
          logger.warn(s"HTTP [${method.getMethod}] request  ${method.getURI} ERROR. retry [${retry + 1}] .")
          request(methodStr, url, body, contentType, header, charset, retry + 1)
        } else {
          logger.warn(s"HTTP [${method.getMethod}] request : ${method.getURI} ERROR.", e)
          throw e
        }
      case e: Throwable =>
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

