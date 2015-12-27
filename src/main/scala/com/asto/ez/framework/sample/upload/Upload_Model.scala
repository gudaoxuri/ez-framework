package com.asto.ez.framework.sample.upload

import com.asto.ez.framework.storage.Entity
import com.asto.ez.framework.storage.mongo.{MongoBaseModel, MongoBaseStorage}

@Entity("")
class Upload_Model extends MongoBaseModel

object Upload_Model extends MongoBaseStorage[Upload_Model]
