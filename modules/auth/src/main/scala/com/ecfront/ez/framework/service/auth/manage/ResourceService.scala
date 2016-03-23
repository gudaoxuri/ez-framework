package com.ecfront.ez.framework.service.auth.manage

import com.ecfront.ez.framework.service.auth.EZAuthContext
import com.ecfront.ez.framework.service.auth.model.EZ_Resource
import com.ecfront.ez.framework.service.rpc.foundation.RPC
import com.ecfront.ez.framework.service.rpc.http.HTTP
import com.ecfront.ez.framework.service.rpc.http.scaffold.SimpleHttpService
import com.ecfront.ez.framework.service.storage.foundation.BaseStorage

/**
  * 资源管理
  */
@RPC("/auth/manage/resource/")
@HTTP
object ResourceService extends SimpleHttpService[EZ_Resource, EZAuthContext] {

  override protected val storageObj: BaseStorage[EZ_Resource] = EZ_Resource

}