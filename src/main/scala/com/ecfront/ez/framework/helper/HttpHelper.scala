package com.ecfront.ez.framework.helper

import java.io.File
import java.net.SocketException

import com.ecfront.common.JsonHelper
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.apache.http.client.methods._
import org.apache.http.entity.{FileEntity, StringEntity}
import org.apache.http.impl.client.{CloseableHttpClient, HttpClients}
import org.apache.http.util.EntityUtils
import org.apache.http.{HttpHeaders, NoHttpResponseException}

import scala.language.implicitConversions

object HttpHelper extends LazyLogging {

  val httpClient: CloseableHttpClient = HttpClients.createDefault

  def post(url: String, body: AnyRef, header: Map[String, String] = Map(), contentType: String = "application/json; charset=utf-8"): String = {
    execute(new HttpPost(url), body, header, contentType)
  }

  def put(url: String, body: AnyRef, header: Map[String, String] = Map(), contentType: String = "application/json; charset=utf-8"): String = {
    execute(new HttpPut(url), body, header, contentType)
  }

  def get(url: String, header: Map[String, String] = Map(), contentType: String = "application/json; charset=utf-8"): String = {
    execute(new HttpGet(url), null, header, contentType)
  }

  def delete(url: String, header: Map[String, String] = Map(), contentType: String = "application/json; charset=utf-8"): String = {
    execute(new HttpDelete(url), null, header, contentType)
  }

  def upload(url: String, file: File, header: Map[String, String] = Map()): String = {
    execute(new HttpPost(url), file, header, null)
  }

  implicit def toSafe(str: String): Object {def safe: String} = new {
    def safe = {
      if (str != null && str.nonEmpty) {
        str.replaceAll("&", "&amp;").replaceAll("\\<", "&lt;").replaceAll("\\>", "&gt;").replaceAll("'", "&apos;").replaceAll("\"", "&quot;")
      } else {
        ""
      }
    }
  }

  private def execute(method: HttpRequestBase, body: AnyRef, header: Map[String, String] = Map(), contentType: String, retry: Int = 0): String = {
    logger.debug(s"HTTP [${method.getMethod}] request : ${method.getURI}")
    if (header != null) {
      header.foreach(h => method.addHeader(h._1, h._2))
    }
    if (contentType != null) {
      method.setHeader(HttpHeaders.CONTENT_TYPE, contentType)
    }
    if (body != null) {
      val entity = body match {
        case b: String => new StringEntity(b, "UTF-8")
        /*        case b: Map[_, _] =>
                  val m = new util.ArrayList[NameValuePair]()
                  b.asInstanceOf[Map[String, Any]].foreach {
                    entry =>
                      m.add(new BasicNameValuePair(entry._1, entry._2.toString))
                  }
                  new UrlEncodedFormEntity(m, "UTF-8")*/
        case b: File =>
          new FileEntity(b)
        case _ => new StringEntity(JsonHelper.toJsonString(body), "UTF-8")
      }
      method.asInstanceOf[HttpEntityEnclosingRequestBase].setEntity(entity)
    }
    try {
      val response = httpClient.execute(method)
      EntityUtils.toString(response.getEntity)
    } catch {
      case e if e.getClass == classOf[SocketException] || e.getClass == classOf[NoHttpResponseException] =>
        if (retry <= 5) {
          Thread.sleep(500)
          logger.warn(s"HTTP [${method.getMethod}] request  ${method.getURI} ERROR. retry [${retry + 1}] .")
          execute(method, body, header, contentType, retry + 1)
        } else {
          logger.warn(s"HTTP [${method.getMethod}] request : ${method.getURI} ERROR.", e)
          throw e
        }
      case e: Exception =>
        logger.warn(s"HTTP [${method.getMethod}] request : ${method.getURI} ERROR.", e)
        throw e
    }
  }
}



