package com.ecfront.ez.framework.service.rpc.foundation

import com.ecfront.ez.framework.service.storage.foundation.EZStorageContext

import scala.beans.BeanProperty
import scala.language.implicitConversions

class EZRPCContext {

  @BeanProperty var method: String = _
  @BeanProperty var templateUri: String = _
  @BeanProperty var realUri: String = _
  @BeanProperty var parameters: Map[String, String] = _
  @BeanProperty var remoteIP: String = _
  @BeanProperty var accept: String = _
  @BeanProperty var contentType: String = _

}

object EZRPCContext {

  implicit def toStorageContext(rpcContext: EZRPCContext): EZStorageContext = {
    EZStorageContext()
  }

}
