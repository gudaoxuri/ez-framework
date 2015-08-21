package com.ecfront.ez.framework.service

import scala.beans.BeanProperty

trait IdVO extends Serializable {
  @BeanProperty var id: String = _
}

trait SecureVO extends IdVO {
  @BeanProperty var create_user: String = _
  @BeanProperty var create_time: Long = _
  @BeanProperty var update_user: String = _
  @BeanProperty var update_time: Long = _
}





