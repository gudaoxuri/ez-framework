package com.ecfront.ez.framework.module.keylog

import com.ecfront.ez.framework.service.SecureModel
import com.ecfront.storage.Entity

import scala.beans.BeanProperty

@Entity("Key Logs")
case class KeyLogModel() extends SecureModel {
  @BeanProperty var code: String = _
  @BeanProperty var message: String = _
  @BeanProperty var loginId: String = _
}