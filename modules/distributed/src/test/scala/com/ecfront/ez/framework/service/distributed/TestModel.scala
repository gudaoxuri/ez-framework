package com.ecfront.ez.framework.service.distributed

import scala.beans.BeanProperty

case class TestModel() {
  @BeanProperty var id: String = _
  @BeanProperty var name: String = _
  @BeanProperty var bool: Boolean = _
  @BeanProperty
  @com.ecfront.common.Ignore var age: Int = _
}

