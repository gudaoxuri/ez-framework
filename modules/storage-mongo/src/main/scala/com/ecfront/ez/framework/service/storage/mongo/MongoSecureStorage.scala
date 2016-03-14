package com.ecfront.ez.framework.service.storage.mongo

import com.ecfront.ez.framework.service.storage.foundation.{SecureModel, SecureStorage}

trait MongoSecureStorage[M <: SecureModel] extends MongoBaseStorage[M] with SecureStorage[M] {

}
