package com.asto.ez.framework.auth.manage

import com.asto.ez.framework.auth.EZ_Resource
import com.asto.ez.framework.rpc.{HTTP, RPC}
import com.asto.ez.framework.scaffold.SimpleRPCService

@RPC("/auth/manage/resource/")
@HTTP
object ResourceService extends SimpleRPCService[EZ_Resource]