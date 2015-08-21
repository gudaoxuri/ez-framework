package com.ecfront.ez.framework.helper

import java.io.File
import java.net.SocketException
import java.util

import com.ecfront.common.Resp
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.message.BasicNameValuePair
import org.apache.http.{NameValuePair, HttpEntity}
import org.apache.http.client.methods._
import org.apache.http.entity.{FileEntity, StringEntity}
import org.apache.http.impl.client.{HttpClients, CloseableHttpClient}
import org.apache.http.util.EntityUtils

import scala.language.implicitConversions

object HttpHelper extends LazyLogging {

  val httpClient: CloseableHttpClient = HttpClients.createDefault

  def post(url: String, body: AnyRef, header: Map[String, String] = Map(), customFun: => HttpEntity => String = {
    EntityUtils.toString
  }): Resp[String] = {
    execute(new HttpPost(url), body, header, customFun)
  }

  def put(url: String, body: AnyRef, header: Map[String, String] = Map(), customFun: => HttpEntity => String = {
    EntityUtils.toString
  }): Resp[String] = {
    execute(new HttpPut(url), body, header, customFun)
  }

  def get(url: String, header: Map[String, String] = Map(), customFun: => HttpEntity => String = {
    EntityUtils.toString
  }): Resp[String] = {
    execute(new HttpGet(url), null, header, customFun)
  }

  def delete(url: String, header: Map[String, String] = Map(), customFun: => HttpEntity => String = {
    EntityUtils.toString
  }): Resp[String] = {
    execute(new HttpDelete(url), null, header, customFun)
  }

  def upload(url: String, file: File, header: Map[String, String] = Map(), customFun: => HttpEntity => String = {
    EntityUtils.toString
  }): Resp[String] = {
    execute(new HttpPost(url), file, header, customFun)
  }

  private def execute(method: HttpRequestBase, body: AnyRef, header: Map[String, String] = Map(), customFun: => HttpEntity => String = {
    EntityUtils.toString
  }, retry: Int = 0): Resp[String] = {
    logger.debug(s"HTTP [${method.getMethod}] request : ${method.getURI}")
    if (header != null) {
      header.foreach(h => method.addHeader(h._1, h._2))
    }
    if (body != null) {
      val entity = body match {
        case b: String => new StringEntity(b, "UTF-8")
        case b: Map[_, _] =>
          val m = new util.ArrayList[NameValuePair]()
          b.asInstanceOf[Map[String, Any]].foreach {
            entry =>
              m.add(new BasicNameValuePair(entry._1, entry._2.toString))
          }
          new UrlEncodedFormEntity(m, "UTF-8")
        case b: File =>
          new FileEntity(b)
      }
      method.asInstanceOf[HttpEntityEnclosingRequestBase].setEntity(entity)
    }
    try {
      val response = httpClient.execute(method)
      Resp.success(customFun(response.getEntity))
    } catch {
      case e: SocketException =>
        if (retry <= 5) {
          Thread.sleep(500)
          logger.warn(s"HTTP [${method.getMethod}] request  ${method.getURI} ERROR. retry [${retry + 1}] .")
          execute(method, body, header, customFun, retry + 1)
        } else {
          logger.warn(s"HTTP [${method.getMethod}] request : ${method.getURI} ERROR.", e)
          Resp.unknown(e.getMessage)
        }
      case e: Exception =>
        logger.warn(s"HTTP [${method.getMethod}] request : ${method.getURI} ERROR.", e)
        Resp.unknown(e.getMessage)
    }
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
}



