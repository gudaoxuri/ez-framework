package com.ecfront.ez.framework.service.test1

import com.ecfront.common.Resp
import com.ecfront.ez.framework.core.EZServiceAdapter

object ServiceAdapter extends EZServiceAdapter[Test1Context] {

  override def init(parameter: Test1Context): Resp[String] = {
    assert(parameter.field1 == "1")
    assert(parameter.field2 == "2")
    Resp.success("")
  }

  override def destroy(parameter: Test1Context): Resp[String] = {
    assert(parameter.field1 == "1")
    assert(parameter.field2 == "2")
    Resp.success("")
  }

  override var serviceName: String = "test1"

}

case class Test1Context(field1: String, field2: String)


