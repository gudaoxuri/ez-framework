package com.ecfront.ez.framework.module.auth

import com.ecfront.ez.framework.storage._

import scala.beans.BeanProperty

/**
 * 资源实体，id=method@uri
 */
@Entity("Resources")
case class EZ_Resource() extends SecureModel with StatusModel {
  @Index
  @BeanProperty var name: String = _
  @ManyToMany(mapping = "EZ_Role", master = false, fetch = true)
  @BeanProperty var role_ids: List[String] = List[String]()
}

/**
 * 角色实体，id=code
 */
@Entity("Roles")
case class EZ_Role() extends SecureModel with StatusModel {
  @Index
  @BeanProperty var name: String = _
  @ManyToMany(mapping = "EZ_Resource", master = true, fetch = true)
  @BeanProperty var resource_ids: Map[String, EZ_Organization] = Map[String,EZ_Organization]()
}

/**
 * 组织实体
 */
@Entity("Organizations")
case class EZ_Organization() extends SecureModel with StatusModel {
  @Index
  @BeanProperty var name: String = _
  @BeanProperty var image: String = _
  @OneToMany(mapping = "EZ_Account", relField = "organization_id", fetch = false)
  @BeanProperty var account_ids: List[String] = List[String]()
}

/**
 * 账号实体，id=account
 */
@Entity("Accounts")
case class EZ_Account() extends SecureModel with StatusModel {
  @Index
  @BeanProperty var name: String = _
  @BeanProperty var image: String = _
  @BeanProperty var password: String = _
  @Index
  @BeanProperty var email: String = _
  @Index
  @BeanProperty var ext_id: String = _
  @BeanProperty
  @Text var ext_info: String = _
  @Index
  @BeanProperty var organization_id: String = _
  @ManyToMany(mapping = "EZ_Role", master = true, fetch = true)
  @BeanProperty var role_ids: Map[String, EZ_Role] = Map[String,EZ_Role]()
}

@Entity("Token Info")
case class EZ_Token_Info() extends IdModel {
  @BeanProperty var login_id: String = _
  @BeanProperty var login_name: String = _
  @BeanProperty var organization_id: String = _
  @BeanProperty var organization_name: String = _
  @BeanProperty var role_ids_json: String = _
  @BeanProperty var ext_id: String = _
  @BeanProperty var last_login_time: Long = _
}


