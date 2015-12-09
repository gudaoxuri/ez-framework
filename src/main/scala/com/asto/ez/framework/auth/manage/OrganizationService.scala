package com.asto.ez.framework.auth.manage

import com.asto.ez.framework.auth.EZ_Organization
import com.asto.ez.framework.rpc.{HTTP, RPC}
import com.asto.ez.framework.scaffold.SimpleRPCService

@RPC("/auth/manage/organization/")
@HTTP
object OrganizationService extends SimpleRPCService[EZ_Organization]