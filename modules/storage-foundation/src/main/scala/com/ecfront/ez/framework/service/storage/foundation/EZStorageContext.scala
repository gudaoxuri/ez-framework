package com.ecfront.ez.framework.service.storage.foundation

import scala.beans.BeanProperty

case class EZStorageContext(
                             @BeanProperty val optAccount: String = "",
                             @BeanProperty val optOrganization: String = "")
