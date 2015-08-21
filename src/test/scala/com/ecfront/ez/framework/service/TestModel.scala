package com.ecfront.ez.framework.service

import com.ecfront.storage.Entity

import scala.beans.BeanProperty

@Entity("")
case class TestModel() extends SecureModel {
  @BeanProperty var name: String = _
  @BeanProperty var bool: Boolean = _
  @BeanProperty
  @com.ecfront.common.Ignore var age: Int = _
}


case class TestVO() extends IdVO {
  @BeanProperty var name: String = _
  @BeanProperty var bool: Boolean = _
  @BeanProperty var age: Int = _
}
