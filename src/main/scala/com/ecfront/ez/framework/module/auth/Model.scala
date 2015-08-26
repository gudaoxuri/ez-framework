package com.ecfront.ez.framework.module.auth

import com.ecfront.ez.framework.service.{IdModel, SecureModel}
import com.ecfront.storage._

import scala.beans.BeanProperty

/**
 * 资源实体，id=method@uri
 */
@Entity("Resources")
case class Resource() extends SecureModel {
  @BeanProperty var name: String = _
  @ManyToMany(mapping = "Role", labelField = "name", master = false, fetch = true)
  @BeanProperty var role_ids: List[String] = List[String]()
}

/**
 * 角色实体，id=code
 */
@Entity("Roles")
case class Role() extends SecureModel {
  @BeanProperty var name: String = _
  @ManyToMany(mapping = "Resource", labelField = "name", master = true, fetch = true)
  @BeanProperty var resource_ids: Map[String, String] = Map[String,String]()
}


/**
 * 账号实体，id=account
 */
@Entity("Accounts")
case class Account() extends SecureModel {
  @Index
  @BeanProperty var name: String = _
  @BeanProperty var password: String = _
  @BeanProperty var email: String = _
  @Index
  @BeanProperty var ext_id: String = _
  @BeanProperty
  @Text var ext_info: String = _
  @ManyToMany(mapping = "Role", labelField = "name", master = true, fetch = true)
  @BeanProperty var role_ids: Map[String, String] = Map[String,String]()
}

@Entity("Token Info")
case class TokenInfo() extends IdModel {
  @BeanProperty var login_id: String = _
  @BeanProperty var login_name: String = _
  @BeanProperty var role_ids: Map[String, String] = _
  @BeanProperty var ext_id: String = _
  @BeanProperty var last_login_time: Long = _
}


