package com.ecfront.ez.framework.storage

import scala.beans.BeanProperty

@Entity("APP表")
case class App() extends IdModel {
  @BeanProperty var name: String = _

  @OneToMany(mapping = "account", relField = "app_id", fetch = true)
  @BeanProperty var accountIds: List[String] = List()
  @OneToMany(mapping = "account", relField = "app_id", fetch = true)
  @BeanProperty var accountInfos: Map[String, Account] = Map[String,Account]()
}

@Entity("账户表")
case class Account() extends IdModel with StatusModel{
  @BeanProperty var name: String = _

  @BeanProperty var app_id: String = _
  @ManyToMany(mapping = "Role", master = true, fetch = true)
  @BeanProperty var roleIds: List[String] = List()
  @ManyToMany(mapping = "Role", master = true, fetch = true)
  @BeanProperty var roleInfos: Map[String, Role] = Map[String,Role]()
}

@Entity(desc = "角色表")
case class Role() extends IdModel {
  @Desc("code")
  @Index
  @Unique
  @BeanProperty var code: String = _
  @Desc("name")
  @Index
  @BeanProperty var name: String = _

  @ManyToMany(mapping = "Account", master = false, fetch = false)
  @BeanProperty var accountIds: List[String] = List()
  @ManyToMany(mapping = "Resource", master = true, fetch = true)
  @BeanProperty var resourceIds: List[String] = List()

}

@Entity("资源表")
case class Resource() extends IdModel {
  @BeanProperty
  @Text var name: String = _
}


@Entity("日志表")
//@SeqId
case class Log() extends IdModel {
  @BeanProperty
  @Text var name: String = _
}
