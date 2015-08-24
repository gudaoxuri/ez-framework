package com.ecfront.ez.framework.module.auth.manage

import com.ecfront.ez.framework.module.auth.Resource
import com.ecfront.ez.framework.module.core.EZReq
import com.ecfront.ez.framework.service.protocols.JDBCService
import com.ecfront.ez.framework.service.{BasicService, SyncService}

object ResourceService extends JDBCService[Resource, EZReq] with SyncService[Resource, EZReq] with BasicService
