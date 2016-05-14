package com.ecfront.ez.framework.service.auth

import com.ecfront.common.Resp
import com.ecfront.ez.framework.service.auth.model.EZ_Account
import com.ecfront.ez.framework.service.storage.foundation._
import io.vertx.core.json.JsonObject

import scala.beans.BeanProperty

/**
  * 带操作信息的实体基类
  * 默认情况下操作信息会根据上下文自动注入
  */
trait OrganizationModel extends BaseModel {

  @Index
  @BeanProperty var organization_code: String = _

}

object OrganizationModel

trait OrganizationStorage[M <: OrganizationModel] extends BaseStorage[M] {

  override def filterByModel(model: M, context: EZStorageContext): Resp[M] = {
    if (context.optAccount != "" && (
      context.optAccount != EZ_Account.SYSTEM_ACCOUNT_LOGIN_ID || context.optOrganization != "")) {
      // 非系统管理员
      if (model.id == null || model.id.isEmpty) {
        // 新建,只能创建自己组织的信息
        model.organization_code = context.optOrganization
        Resp.success(model)
      } else {
        val m = doGetById(model.id, context)
        if (m && m.body != null && m.body.organization_code == context.optOrganization) {
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

  override def filterById(id: Any, context: EZStorageContext): Resp[(String, List[Any])] = {
    if (ServiceAdapter.mongoStorage) {
      filterByCond(s"""{"id":"$id"}""", List(), context)
    } else {
      filterByCond(s"""id = ? """, List(id), context)
    }
  }

  override def filterByCond(condition: String, parameters: List[Any], context: EZStorageContext): Resp[(String, List[Any])] = {
    val orgCode = context.optOrganization
    val result = if (context.optAccount == "" || (
      context.optAccount == EZ_Account.SYSTEM_ACCOUNT_LOGIN_ID && context.optOrganization == "")) {
      // 没有登录(内部逻辑处理) 或 登录为sysadmin 时不需要过滤
      if (condition != null && condition.nonEmpty) {
        (condition, parameters)
      } else {
        ("", parameters)
      }
    } else {
      if (ServiceAdapter.mongoStorage) {
        if (condition != null && condition.nonEmpty) {
          (new JsonObject(condition).put("organization_code", orgCode).encode(), parameters)
        } else {
          (s"""{"organization_code":"$orgCode"}""", parameters)
        }
      } else {
        if (condition != null && condition.nonEmpty) {
          (s" organization_code = ? AND " + condition, List(orgCode) ++ parameters)
        } else {
          (s" organization_code = ? ", List(orgCode) ++ parameters)
        }
      }
    }
    Resp.success(result)
  }

}

