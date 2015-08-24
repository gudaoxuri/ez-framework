package com.ecfront.ez.framework.module.auth.manage

import com.ecfront.common.Resp
import com.ecfront.ez.framework.module.auth.Role
import com.ecfront.ez.framework.module.core.EZReq
import com.ecfront.ez.framework.service.{SyncService, BasicService}
import com.ecfront.ez.framework.service.protocols.JDBCService
import com.ecfront.lego.core.component.BasicService
import com.ecfront.lego.core.foundation.{IdModelExt, LReq}
import com.ecfront.lego.rbac.foundation.Role
import com.ecfront.service.SyncService
import com.ecfront.service.protocols.JDBCService

object RoleService extends JDBCService[Role, EZReq] with SyncService[Role, EZReq] with BasicService {

  override protected def _preSave(model: Role, request: Option[EZReq]): Resp[Any] = {
    model.id = model.code + IdModelExt.ID_SPLIT_FLAG + (if (!LReq.isLEGO(request.get) || model.appId == null) request.get.appId else model.appId)
    Resp.success(model)
  }

}
