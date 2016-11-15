package com.ecfront.ez.framework.core.rpc

import com.ecfront.common.{JsonHelper, Resp, StandardCode}
import com.ecfront.ez.framework.core.logger.Logging
import com.ecfront.ez.framework.core.rpc.Method.Method
import com.fasterxml.jackson.databind.JsonNode

/**
  * Resp返回值封装的HTTP 请求操作
  *
  * 包含了对HTTP GET POST PUT DELETE 四类常用操作
  *
  */
object RespHttpClientProcessor extends Logging {

  /**
    * GET 请求
    *
    * @param url         请求URL
    * @param contentType 请求类型，默认为 application/json; charset=utf-8
    * @return 请求结果，string类型
    */
  def get[E: Manifest](url: String, contentType: String = "application/json; charset=utf-8"): Resp[E] = {
    request[E](Method.GET, url, null, contentType)
  }

  /**
    * POST 请求
    *
    * @param url         请求URL
    * @param body        请求体
    * @param contentType 请求类型，默认为 application/json; charset=utf-8
    * @return 请求结果，string类型
    */
  def post[E: Manifest](url: String, body: Any, contentType: String = "application/json; charset=utf-8"): Resp[E] = {
    request[E](Method.POST, url, body, contentType)
  }

  /**
    * PUT 请求
    *
    * @param url         请求URL
    * @param body        请求体
    * @param contentType 请求类型，默认为 application/json; charset=utf-8
    * @return 请求结果，string类型
    */
  def put[E: Manifest](url: String, body: Any, contentType: String = "application/json; charset=utf-8"): Resp[E] = {
    request[E](Method.PUT, url, body, contentType)
  }

  /**
    * DELETE 请求
    *
    * @param url         请求URL
    * @param contentType 请求类型，默认为 application/json; charset=utf-8
    * @return 请求结果，string类型
    */
  def delete[E: Manifest](url: String, contentType: String = "application/json; charset=utf-8"): Resp[E] = {
    request[E](Method.DELETE, url, null, contentType)
  }

  private def request[E](method: Method, url: String, body: Any, contentType: String)(implicit m: Manifest[E]): Resp[E] = {
    val resp = HttpClientProcessor.request(method, url, body, contentType, Map())
    val json = JsonHelper.toJson(resp)
    val code = json.get("code").asText()
    if (code == StandardCode.SUCCESS) {
      val jsonBody = json.get("body")
      if (jsonBody == null) {
        Resp[E](StandardCode.SUCCESS, "")
      } else {
        val body = jsonBody match {
          case obj: JsonNode =>
            JsonHelper.toObject[E](obj)
          case _ =>
            jsonBody.asInstanceOf[E]
        }
        Resp.success[E](body)
      }
    } else {
      Resp[E](code, json.get("message").asText())
    }
  }

}

