package com.ecfront.ez.framework.service.weixin

import com.ecfront.common.JsonHelper
import com.ecfront.ez.framework.service.rpc.http.HttpClientProcessor
import com.typesafe.scalalogging.slf4j.LazyLogging

trait BaseProcessor extends LazyLogging {

  val BASIC_URL = "https://api.weixin.qq.com/cgi-bin/"

  def getDataByTokenUrl(uri: String, body: Any = null): Map[String, Any] = {
    val u = if (uri.contains("?")) {
      uri + "&"
    } else {
      uri + "?"
    }
    getDataByBasicUrl(u + "access_token=" + AccessTokenManager.getAccessToken, body)
  }


  def getDataByBasicUrl(uri: String, body: Any = null): Map[String, Any] = {
    getDataByRawUrl(BASIC_URL + uri, body)
  }

  def getDataByRawUrl(uri: String, body: Any = null): Map[String, Any] = {
    val resp =
      if (body == null) {
        HttpClientProcessor.get(uri)
      } else {
        HttpClientProcessor.post(uri, body)
      }
    val result = JsonHelper.toObject[Map[String, Any]](resp)
    if (result.contains("errcode")) {
      throw new Exception(s"Request error [${result("errcode")}] ${result("errmsg")}")
    } else {
      result
    }
  }


}
