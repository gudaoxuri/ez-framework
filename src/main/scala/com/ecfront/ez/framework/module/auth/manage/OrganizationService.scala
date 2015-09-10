package com.ecfront.ez.framework.module.auth.manage

import com.ecfront.common.Req
import com.ecfront.ez.framework.module.SimpleRPCService
import com.ecfront.ez.framework.module.auth.EZ_Organization
import com.ecfront.ez.framework.rpc._
import com.ecfront.ez.framework.service.protocols.JDBCService

@RPC("/auth/manage/organization/")
@HTTP
object OrganizationService extends SimpleRPCService[EZ_Organization, Req] with JDBCService[EZ_Organization, Req]