package com.ecfront.ez.framework.service.storage.mongo

import com.ecfront.ez.framework.service.storage.foundation.{SecureModel, SecureStorage}

/**
  * Mongo带操作信息的持久化实现
  *
  * @tparam M 实体类型
  */
trait MongoSecureStorage[M <: SecureModel] extends MongoBaseStorage[M] with SecureStorage[M]
