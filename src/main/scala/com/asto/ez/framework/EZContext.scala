package com.asto.ez.framework

case class EZContext() {
  var remoteIP: String = _
  var userId: String = _
  var orgId: String = _
}

object EZContext {

  def build() = {
    val context =EZContext()
    context.remoteIP = ""
    context.userId = ""
    context.orgId = ""
    context
  }

}
