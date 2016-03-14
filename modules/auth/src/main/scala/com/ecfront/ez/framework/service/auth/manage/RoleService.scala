package com.ecfront.ez.framework.service.auth.manage

import com.ecfront.common.Resp
import com.ecfront.ez.framework.service.auth.{EZAuthContext, EZ_Role}
import com.ecfront.ez.framework.service.rpc.foundation.RPC
import com.ecfront.ez.framework.service.rpc.http.HTTP
import com.ecfront.ez.framework.service.rpc.http.scaffold.SimpleHttpService
import com.ecfront.ez.framework.service.storage.foundation.BaseStorage

@RPC("/auth/manage/role/")
@HTTP
object RoleService extends SimpleHttpService[EZ_Role, EZAuthContext] {

  override protected val storageObj: BaseStorage[EZ_Role] = EZ_Role

  def appendResources(roleCode: String, appendResourceCodes: List[String]): Resp[Void] = {
    val roleR = EZ_Role.getByCond(s"""{"code":"$roleCode"}""")
    if (roleR) {
      roleR.body.resource_codes ++= appendResourceCodes
      EZ_Role.update(roleR.body)
    } else {
      roleR
    }
  }

}