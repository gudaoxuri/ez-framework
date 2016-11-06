package com.ecfront.ez.framework.core.rpc.apidoc

import scala.collection.mutable.ArrayBuffer

case class APIDocSectionVO(fileName: String, name: String, desc: String) {
  val items = new ArrayBuffer[APIDocItemVO]()
}

case class APIDocItemVO(name: String, method: String, uri: String, reqbody: Class[_], respStr: String, desc: String = "", reqExt: String = "", respExt: String = "")
