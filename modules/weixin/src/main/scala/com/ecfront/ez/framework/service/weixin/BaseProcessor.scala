package com.ecfront.ez.framework.service.weixin

import com.ecfront.common.JsonHelper
import com.ecfront.ez.framework.service.rpc.http.HttpClientProcessor
import com.typesafe.scalalogging.slf4j.LazyLogging

trait BaseProcessor extends LazyLogging {

  val BASIC_URL = "https://api.weixin.qq.com/cgi-bin/"

  def getData(uri: String): Map[String, Any] = {
    val resp = HttpClientProcessor.get(BASIC_URL + uri)
    val result = JsonHelper.toObject[Map[String, Any]](resp)
    if (result.contains("errcode")) {
      throw new Exception(s"Request error [${result("errcode")}] ${result("errmsg")}")
    } else {
      result
    }
  }

}
