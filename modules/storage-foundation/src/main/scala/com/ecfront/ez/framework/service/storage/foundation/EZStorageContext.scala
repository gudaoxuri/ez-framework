package com.ecfront.ez.framework.service.storage.foundation

import scala.beans.BeanProperty

/**
  * 持久化上下文
  * @param optAccount 操作账号
  * @param optOrganization 操作者组织
  */
case class EZStorageContext(
                             @BeanProperty optAccount: String = "",
                             @BeanProperty optOrganization: String = "")
