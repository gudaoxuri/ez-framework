package com.ecfront.ez.framework.service.storage.jdbc

import com.ecfront.ez.framework.service.storage.foundation.{SecureModel, SecureStorage}

/**
  * JDBC带操作信息的持久化实现
  *
  * @tparam M 实体类型
  */
trait JDBCSecureStorage[M <: SecureModel] extends JDBCBaseStorage[M] with SecureStorage[M]
