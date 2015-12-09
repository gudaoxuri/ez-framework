package com.asto.ez.framework.storage

import scala.beans.BeanProperty

trait IdModel extends BaseModel {

  @BeanProperty
  @Id("")
  var id: String = _

}







