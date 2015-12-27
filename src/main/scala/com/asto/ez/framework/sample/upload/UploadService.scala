package com.asto.ez.framework.sample.upload

import com.asto.ez.framework.rpc.{HTTP, RPC}
import com.asto.ez.framework.scaffold.SimpleRPCService
import com.asto.ez.framework.storage.BaseStorage

@RPC("/public/upload/")
@HTTP
object UploadService extends SimpleRPCService[Upload_Model] {
  override protected val storageObj: BaseStorage[Upload_Model] = Upload_Model
}
