package com.ecfront.ez.framework.service.jdbc

import com.ecfront.common.Resp
import com.ecfront.ez.framework.core.EZ

import scala.beans.BeanProperty

/**
  * 带操作信息的实体基类
  * 默认情况下操作信息会根据上下文自动注入
  */
trait OrganizationModel extends BaseModel {

  @BeanProperty var organization_code: String = _

}

object OrganizationModel

trait OrganizationStorage[M <: OrganizationModel] extends BaseStorage[M] {

  override def filterByModel(model: M): Resp[M] = {
    if (EZ.context.optAccCode.nonEmpty && (
      EZ.context.optAccCode != BaseModel.SYSTEM_ACCOUNT_LOGIN_ID || EZ.context.optOrgCode.nonEmpty)) {
      // 非系统管理员
      if (model.id == null || model.id.isEmpty) {
        // 新建,只能创建自己组织的信息
        model.organization_code = EZ.context.optOrgCode
        Resp.success(model)
      } else {
        val m = doGetById(model.id)
        if (m && m.body != null && m.body.organization_code == EZ.context.optOrgCode) {
          // 更新的是自己组织下的信息
          Resp.success(model)
        } else {
          // 不存在此id或不在自己组织下
          Resp.notFound("Account not exist")
        }
      }
    } else {
      Resp.success(model)
    }
  }

  override def filterById(id: Any): Resp[(String, List[Any])] = {
    filterByCond(s"""${_entityInfo.idFieldName} = ? """, List(id))
  }

  override def filterByUUID(uuid: String): Resp[(String, List[Any])] = {
    filterByCond(s"""${_entityInfo.uuidFieldName} = ? """, List(uuid))
  }

  override def filterByCond(condition: String, parameters: List[Any]): Resp[(String, List[Any])] = {
    val result = if (EZ.context.optAccCode.isEmpty || (
      EZ.context.optAccCode == BaseModel.SYSTEM_ACCOUNT_LOGIN_ID && EZ.context.optOrgCode.isEmpty)) {
      // 没有登录(内部逻辑处理) 或 登录为sysadmin 时不需要过滤
      if (condition != null && condition.nonEmpty) {
        (condition, parameters)
      } else {
        ("", parameters)
      }
    } else {
      if (condition != null && condition.nonEmpty) {
        (s" organization_code = '${EZ.context.optOrgCode}' AND " + condition, parameters)
      } else {
        (s" organization_code = '${EZ.context.optOrgCode}'", parameters)
      }
    }
    Resp.success(result)
  }

}

