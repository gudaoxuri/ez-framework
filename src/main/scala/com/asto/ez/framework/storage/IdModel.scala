package com.asto.ez.framework.storage

import com.asto.ez.framework.storage.jdbc.Id

import scala.beans.BeanProperty

trait IdModel extends BaseModel {

  @BeanProperty
  @Id("")
  var id: String = _

}







