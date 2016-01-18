package com.asto.ez.framework

import com.asto.ez.framework.auth.EZ_Token_Info

case class EZContext() {
  var token: String = _
  var login_info: EZ_Token_Info = _
  var method: String = _
  var templateUri: String = _
  var realUri: String = _
  var parameters: Map[String, String] = _
  var remoteIP: String = _
  var accept: String = _
  var contentType: String = _
}

object EZContext {

  def build() = {
    val context = EZContext()
    context.token = ""
    context.login_info = null
    context.method = ""
    context.templateUri = ""
    context.realUri = ""
    context.parameters = Map()
    context.remoteIP = ""
    context.accept = ""
    context.contentType = ""
    context
  }

}