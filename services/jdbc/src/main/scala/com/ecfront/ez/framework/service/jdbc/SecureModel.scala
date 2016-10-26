package com.ecfront.ez.framework.service.jdbc

import java.util.Date

import com.ecfront.common.Resp
import com.ecfront.ez.framework.core.EZ
import com.ecfront.ez.framework.core.helper.TimeHelper

import scala.beans.BeanProperty

/**
  * 带操作信息的实体基类
  * 默认情况下操作信息会根据上下文自动注入
  */
trait SecureModel extends BaseModel {

  @Desc("Create Organization", 200, 0)
  @BeanProperty var create_org: String = _
  @Index
  @Desc("Create User", 200, 0)
  @BeanProperty var create_user: String = _
  @Index
  @Desc("Create Time", 20, 0)
  @BeanProperty var create_time: Long = _
  @Desc("Update Organization", 200, 0)
  @BeanProperty var update_org: String = _
  @Index
  @Desc("Update User", 200, 0)
  @BeanProperty var update_user: String = _
  @Index
  @Desc("Update Time", 20, 0)
  @BeanProperty var update_time: Long = _

}

object SecureModel {

  val CREATE_USER_FLAG = "create_user"
  val CREATE_ORG_FLAG = "create_org"
  val CREATE_TIME_FLAG = "create_time"
  val UPDATE_USER_FLAG = "update_user"
  val UPDATE_ORG_FLAG = "update_org"
  val UPDATE_TIME_FLAG = "update_time"

}

trait SecureStorage[M <: SecureModel] extends BaseStorage[M] {

  override def preSave(model: M): Resp[M] = {
    wrapSecureSave(model)
    super.preSave(model)
  }

  override def preUpdate(model: M): Resp[M] = {
    wrapSecureUpdate(model)
    super.preUpdate(model)
  }

  override def preSaveOrUpdate(model: M): Resp[M] = {
    wrapSecureSave(model)
    super.preSaveOrUpdate(model)
  }

  /**
    * 注入操作信息
    *
    * @param model 实体对象
    */
  private def wrapSecureSave(model: M): Unit = {
    val now = TimeHelper.msf.format(new Date()).toLong
    model.create_user = EZ.context.optAccCode
    model.create_time = now
    model.create_org = EZ.context.optOrgCode
    model.update_user = EZ.context.optAccCode
    model.update_time = now
    model.update_org = EZ.context.optOrgCode
  }

  /**
    * 注入操作信息
    *
    * @param model 实体对象
    */
  private def wrapSecureUpdate(model: M): Unit = {
    val now = TimeHelper.msf.format(new Date()).toLong
    model.update_user = EZ.context.optAccCode
    model.update_time = now
    model.update_org = EZ.context.optOrgCode
  }

}