package com.ecfront.ez.framework.service.auth

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

  override def appendByModel(model: M, context: EZStorageContext): M = {
    if (context.optAccount!="" && (
      context.optAccount != EZ_Account.SYSTEM_ACCOUNT_LOGIN_ID || context.optOrganization != "")) {
      model.organization_code = context.optOrganization
    }
    model
  }

  override def appendById(id: Any, context: EZStorageContext): String = {
    if (ServiceAdapter.mongoStorage) {
      appendByCond(s"""{"id":"$id"}""", context)
    } else {
      appendByCond(s"""id = ? """, context)
    }
  }

  override def appendByCond(condition: String, context: EZStorageContext): String = {
    val orgCode = context.optOrganization
    if (context.optAccount=="" || (
      context.optAccount == EZ_Account.SYSTEM_ACCOUNT_LOGIN_ID && context.optOrganization == "")) {
      // 没有登录(内部逻辑处理) 或 登录为sysadmin 时不需要过滤
      if (condition != null && condition.nonEmpty) {
        condition
      } else {
        ""
      }
    } else {
      if (ServiceAdapter.mongoStorage) {
        if (condition != null && condition.nonEmpty) {
          new JsonObject(condition).put("organization_code", orgCode).encode()
        } else {
          s"""{"organization_code":"$orgCode"}"""
        }
      } else {
        if (condition != null && condition.nonEmpty) {
          condition + s" organization_code = '$orgCode' AND "
        } else {
          s" organization_code = '$orgCode'"
        }
      }
    }
  }

}

