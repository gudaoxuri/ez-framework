package com.ecfront.ez.framework.service.test3.sub1

import com.ecfront.common.Resp
import com.ecfront.ez.framework.core.EZServiceAdapter

object ServiceAdapter extends EZServiceAdapter[Test3Sub1Context] {

  override def init(parameter: Test3Sub1Context): Resp[String] = {
    assert(parameter.field1 == "1")
    Resp.success("")
  }

  override def destroy(parameter: Test3Sub1Context): Resp[String] = {
    assert(parameter.field1 == "1")
    Resp.success("")
  }

}

case class Test3Sub1Context(field1: String)


