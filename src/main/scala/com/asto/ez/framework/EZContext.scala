package com.asto.ez.framework

case class EZContext() {
  var token: String = _
  var login_Id: String = _
  var organization_code: String = _
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
    context.login_Id = ""
    context.organization_code = ""
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