package com.ecfront.ez.framework.service.rpc.http

import java.io.File

import com.ecfront.common.JsonHelper
import com.ecfront.ez.framework.core.test.MockStartupSpec
import com.ecfront.ez.framework.service.rpc.foundation.ReqFile

class HttpClientProcessorSpec extends MockStartupSpec {

  test("HttpClient Test") {
    val result = HttpClientProcessor.post(
      "http://127.0.0.1:8080/resource/file/",
      ReqFile(new File(this.getClass.getResource("/").getPath + "IMG_20160403_195547.jpg"),"photo"))
    println(result)
  }

  test("Resp parse Test") {
    println(parse[List[String]](s"""["1","2"]"""))
    println(parse[String](s"""1"""))
    println(parse[Int](s"""1"""))
  }

  def parse[E](str: String)(implicit mf: Manifest[E]): E = {
    mf.runtimeClass match {
      case m if m == classOf[String] => str.asInstanceOf[E]
      case m if m == classOf[Int] => str.toInt.asInstanceOf[E]
      case m if m == classOf[Long] => str.toLong.asInstanceOf[E]
      case _ => JsonHelper.toObject[E](str)
    }

  }

}
