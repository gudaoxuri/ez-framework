package com.ecfront.ez.framework.gateway

import com.ecfront.ez.framework.service.jdbc._

import scala.beans.BeanProperty


@Entity("Resource")
case class EZ_Resource() extends BaseModel with SecureModel with StatusModel {

  @Unique
  @Require
  @Label("Code")
  @BeanProperty var code: String = _
  @Require
  @Label("Method")
  @BeanProperty var method: String = _
  @Require
  @Label("URI")
  @BeanProperty var uri: String = _

}

object EZ_Resource extends BaseStorage[EZ_Resource] with SecureStorage[EZ_Resource] with StatusStorage[EZ_Resource] {

  def apply(code: String, method: String, uri: String): EZ_Resource = {
    val res = EZ_Resource()
    res.code = code
    res.method = method
    res.uri = uri
    res.enable = true
    res
  }

}



