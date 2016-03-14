package com.ecfront.ez.framework.service.auth.manage

import com.ecfront.ez.framework.service.auth.{EZAuthContext, EZ_Organization}
import com.ecfront.ez.framework.service.rpc.foundation.RPC
import com.ecfront.ez.framework.service.rpc.http.HTTP
import com.ecfront.ez.framework.service.rpc.http.scaffold.SimpleHttpService
import com.ecfront.ez.framework.service.storage.foundation.BaseStorage

@RPC("/auth/manage/organization/")
@HTTP
object OrganizationService extends SimpleHttpService[EZ_Organization, EZAuthContext] {

  override protected val storageObj: BaseStorage[EZ_Organization] = EZ_Organization

}