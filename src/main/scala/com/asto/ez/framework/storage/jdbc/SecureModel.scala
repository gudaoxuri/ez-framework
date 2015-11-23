package com.asto.ez.framework.storage.jdbc

import java.util.Date

import com.asto.ez.framework.EZContext
import com.asto.ez.framework.helper.TimeHelper
import com.ecfront.common.Resp

import scala.beans.BeanProperty
import scala.concurrent.Future

trait SecureModel extends BaseModel {
  @Index
  @BeanProperty var create_user: String = _
  @Index
  @BeanProperty var create_org: String = _
  @Index
  @BeanProperty var create_time: Long = _
  @Index
  @BeanProperty var update_user: String = _
  @Index
  @BeanProperty var update_time: Long = _
  @Index
  @BeanProperty var update_org: String = _

  override def save(context: EZContext): Future[Resp[Void]] = {
    val cnt = if (context == null) EZContext.build() else context
    val now = TimeHelper.msf.format(new Date()).toLong
    if (create_user == null) {
      create_user = if (cnt.userId != null) cnt.userId else ""
    }
    if (create_time == 0) {
      create_time = now
    }
    if (create_org == null) {
      create_org = if (cnt.orgId != null) cnt.orgId else ""
    }
    if (update_user == null) {
      update_user = if (cnt.userId != null) cnt.userId else ""
    }
    if (update_time == 0) {
      update_time = now
    }
    if (update_org == null) {
      update_org = if (cnt.orgId != null) cnt.orgId else ""
    }
    super.save(context)
  }

  override def update(context: EZContext): Future[Resp[Void]] = {
    val cnt = if (context == null) EZContext.build() else context
    val now = TimeHelper.msf.format(new Date()).toLong
    if (update_user == null) {
      update_user = if (cnt.userId != null) cnt.userId else ""
    }
    if (update_time == 0) {
      update_time = now
    }
    if (update_org == null) {
      update_org = if (cnt.orgId != null) cnt.orgId else ""
    }
    super.update(context)
  }

  override def saveOrUpdate(context: EZContext): Future[Resp[Void]] = {
    val cnt = if (context == null) EZContext.build() else context
    val now = TimeHelper.msf.format(new Date()).toLong
    if (create_user == null) {
      create_user = if (cnt.userId != null) cnt.userId else ""
    }
    if (create_time == 0) {
      create_time = now
    }
    if (create_org == null) {
      create_org = if (cnt.orgId != null) cnt.orgId else ""
    }
    if (update_user == null) {
      update_user = if (cnt.userId != null) cnt.userId else ""
    }
    if (update_time == 0) {
      update_time = now
    }
    if (update_org == null) {
      update_org = if (cnt.orgId != null) cnt.orgId else ""
    }
    super.saveOrUpdate(context)
  }
}

object SecureModel {

  val SYSTEM_USER_FLAG = "system"

  val CREATE_USER_FLAG = "create_user"
  val CREATE_ORG_FLAG = "create_org"
  val CREATE_TIME_FLAG = "create_time"
  val UPDATE_USER_FLAG = "update_user"
  val UPDATE_ORG_FLAG = "update_org"
  val UPDATE_TIME_FLAG = "update_time"

}
