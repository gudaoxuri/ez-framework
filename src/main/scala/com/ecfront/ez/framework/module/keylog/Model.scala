package com.ecfront.ez.framework.module.keylog

import com.ecfront.ez.framework.storage.{Entity, SecureModel}

import scala.beans.BeanProperty

@Entity("Key Logs")
case class EZ_Key_Log() extends SecureModel {
  @BeanProperty var code: String = _
  @BeanProperty var message: String = _
  @BeanProperty var login_Id: String = _
  @BeanProperty var organization_id: String = _
}