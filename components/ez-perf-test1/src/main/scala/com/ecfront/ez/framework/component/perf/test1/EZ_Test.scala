package com.ecfront.ez.framework.component.perf.test1

import com.ecfront.ez.framework.service.jdbc._

import scala.beans.BeanProperty

@Entity("ez_test")
case class EZ_Test() extends BaseModel with SecureModel with StatusModel {

  @UUID
  @BeanProperty var bus_uuid: String = _
  @Require @Unique
  @Desc("code",100,0)
  @BeanProperty var code: String = _
  @Require
  @Desc("name",100,0)
  @BeanProperty var name: String = _

}

object EZ_Test extends BaseStorage[EZ_Test] with SecureStorage[EZ_Test] with StatusStorage[EZ_Test] {

  def apply(code: String, name: String,enable:Boolean=true): EZ_Test = {
    val res = EZ_Test()
    res.code = code
    res.name = name
    res.enable = enable
    res
  }

}



