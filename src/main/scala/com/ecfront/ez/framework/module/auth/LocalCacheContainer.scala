package com.ecfront.ez.framework.module.auth

import com.ecfront.ez.framework.module.auth.manage.{ResourceService, RoleService}

object LocalCacheContainer {

  private[ez] val resources = collection.mutable.HashSet[String]()
  private[ez] val roles = collection.mutable.Map[String,Set[String]]()

  def init(): Unit = {
    RoleService.__findAll().get.foreach {
      role =>
        addRole(role.id, role.resource_ids.keySet)
    }
    ResourceService.__findAll().get.foreach {
      resource =>
        addResource(resource.id)
    }
  }

  def addRole(roleCode: String, resourceCodes: Set[String]): Unit = {
    roles(roleCode) = resourceCodes
  }

  def removeRole(roleCode: String): Unit = {
    roles -= roleCode
  }

  def removeAllRole(): Unit = {
    roles.clear()
  }

  def addResource(resourceCode: String): Unit = {
    resources += resourceCode
  }

  def removeResource(resourceCode: String): Unit = {
    resources -= resourceCode
  }

  def removeAllResource(): Unit = {
    resources.clear()
  }

  def matchInRoles(resourceCode: String, roleCodes: Set[String]) = roleCodes.exists(roles(_).contains(resourceCode))

  def existResource(resourceCode: String) = resources.contains(resourceCode)

  init()

}
