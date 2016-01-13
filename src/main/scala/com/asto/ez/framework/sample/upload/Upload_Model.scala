package com.asto.ez.framework.sample.upload

import com.asto.ez.framework.storage.{BaseModel, Entity}
import com.asto.ez.framework.storage.mongo.MongoBaseStorage

@Entity("")
class Upload_Model extends BaseModel

object Upload_Model extends MongoBaseStorage[Upload_Model]
