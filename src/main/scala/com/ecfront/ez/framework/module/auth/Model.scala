package com.ecfront.ez.framework.module.auth

import com.ecfront.ez.framework.service.{IdModel, SecureModel}
import com.ecfront.storage._

import scala.beans.BeanProperty


/**
 * 角色实体，id=code
 */
@Entity("Roles")
case class Role() extends SecureModel {
  @BeanProperty var name: String = _
  @ManyToMany(mapping = "Resource", master = false, fetch = false)
  @BeanProperty var resourceIds: List[String] = List()
}

/**
 * 资源实体，id=uri
 */
@Entity("Resources")
case class Resource() extends SecureModel {
  @BeanProperty var name: String = _
  @Index
  @BeanProperty var method: String = _
  @ManyToMany(mapping = "Role", master = false, fetch = true)
  @BeanProperty var roleIds: List[String] = List()
}

/**
 * 账号实体，id=account
 */
@Entity("Accounts")
case class Account() extends SecureModel {
  @BeanProperty var password: String = _
  @Index
  @BeanProperty var name: String = _
  @BeanProperty var email: String = _
  @Index
  @BeanProperty var extId: String = _
  @BeanProperty
  @Text var extInfo: String = _
  @ManyToMany(mapping = "Role", master = true, fetch = true)
  @BeanProperty var roleIds: List[String] = List()
}

/**
 * Login Info Instances ,id = token
 */
@Entity("Login Info")
case class LoginInfo() extends IdModel {
  @BeanProperty var name: String = _
  @BeanProperty var roleIds: List[String] = _
  @BeanProperty var extId: String = _
  @BeanProperty var lastLoginTime: Long = _
}

/**
 * 认证类型
 */
object AuthType extends Enumeration {
  type AuthType = Value
  val LOGIN, LOGOUT, AUTHENTICATION = Value
}
