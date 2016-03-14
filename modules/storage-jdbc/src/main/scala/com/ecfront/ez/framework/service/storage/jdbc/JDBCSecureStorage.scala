package com.ecfront.ez.framework.service.storage.jdbc

import com.ecfront.ez.framework.service.storage.foundation.{SecureModel, SecureStorage}

trait JDBCSecureStorage[M <: SecureModel] extends JDBCBaseStorage[M] with SecureStorage[M] {

}
