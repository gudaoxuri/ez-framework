package com.asto.ez.framework.auth.manage

import com.asto.ez.framework.auth.EZ_Account
import com.asto.ez.framework.rpc.{HTTP, RPC}
import com.asto.ez.framework.scaffold.SimpleRPCService

@RPC("/auth/manage/account/")
@HTTP
object AccountService extends SimpleRPCService[EZ_Account]