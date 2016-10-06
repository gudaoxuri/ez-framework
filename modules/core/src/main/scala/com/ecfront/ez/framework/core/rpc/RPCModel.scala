package com.ecfront.ez.framework.core.rpc

import scala.beans.BeanProperty

class APIDTO {

  @BeanProperty var channel: String = _
  @BeanProperty var method: String = _
  @BeanProperty var path: String = _

}

object APIDTO {

  def apply(channel: String, method: String, path: String): APIDTO = {
    val dto = new APIDTO()
    dto.channel = channel
    dto.method = method
    dto.path = path
    dto
  }
}

object Channel extends Enumeration {
  type Channel = Value
  val HTTP, WS, EB = Value
}

object Method extends Enumeration {
  type Method = Value
  val GET, POST, PUT, DELETE, PUB_SUB, REQ_RESP, REPLY, WS = Value
}





