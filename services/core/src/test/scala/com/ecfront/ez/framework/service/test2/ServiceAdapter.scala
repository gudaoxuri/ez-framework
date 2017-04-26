package com.ecfront.ez.framework.service.test2

import com.ecfront.common.Resp
import com.ecfront.ez.framework.core.EZServiceAdapter

object ServiceAdapter extends EZServiceAdapter[Test2Context] {

  override def init(parameter: Test2Context): Resp[String] = {
    assert(parameter.field1 == "1")
    Resp.success("")
  }

  override def destroy(parameter: Test2Context): Resp[String] = {
    assert(parameter.field1 == "1")
    Resp.success("")
  }

  override lazy val dependents: collection.mutable.Set[String] = collection.mutable.Set("test1","test3.sub1")

  override var serviceName: String = "test2"

}

case class Test2Context(field1: String)


