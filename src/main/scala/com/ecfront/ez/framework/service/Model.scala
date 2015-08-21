package com.ecfront.ez.framework.service

import com.ecfront.storage.Id

import scala.beans.BeanProperty

abstract class IdModel extends Serializable {
  @BeanProperty
  @Id var id: String = _
}

object IdModel {
  val ID_FLAG = "id"
}

abstract class SecureModel extends IdModel {
  @BeanProperty var create_user: String = _
  @BeanProperty var create_time: Long = _
  @BeanProperty var update_user: String = _
  @BeanProperty var update_time: Long = _
}

object SecureModel {
  val SYSTEM_USER_FLAG = "system"
}
