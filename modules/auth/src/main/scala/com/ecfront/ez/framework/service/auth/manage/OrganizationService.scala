package com.ecfront.ez.framework.service.auth.manage

import com.ecfront.ez.framework.core.helper.FileType
import com.ecfront.ez.framework.service.auth.EZAuthContext
import com.ecfront.ez.framework.service.auth.model.EZ_Organization
import com.ecfront.ez.framework.service.rpc.foundation.RPC
import com.ecfront.ez.framework.service.rpc.http.HTTP
import com.ecfront.ez.framework.service.rpc.http.scaffold.SimpleHTTPService
import com.ecfront.ez.framework.service.storage.foundation.BaseStorage

/**
  * 组织（租户）管理
  */
@RPC("/auth/manage/organization/")
@HTTP
object OrganizationService extends SimpleHTTPService[EZ_Organization, EZAuthContext] {

  override protected val storageObj: BaseStorage[EZ_Organization] = EZ_Organization

  // 只能上传图片类型
  override protected def allowUploadTypes = List(FileType.TYPE_IMAGE)

}