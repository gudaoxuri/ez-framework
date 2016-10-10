package com.ecfront.ez.framework.service.auth.model

import com.ecfront.common.{Ignore, Resp}
import com.ecfront.ez.framework.core.i18n.I18NProcessor.Impl
import com.ecfront.ez.framework.service.auth.{CacheManager, ServiceAdapter}
import com.ecfront.ez.framework.service.jdbc._
import io.vertx.core.json.JsonObject

import scala.beans.BeanProperty

/**
  * 角色实体
  */
@Entity("Role")
case class EZ_Role() extends BaseModel with SecureModel with StatusModel with OrganizationModel {

  @Unique
  @Require
  @Label("Code") // organization_code@flag
  @BeanProperty var code: String = _
  @Require
  @Label("Flag")
  @BeanProperty var flag: String = _
  @Require
  @Label("Name")
  @BeanProperty var name: String = _
  @Ignore var exchange_resource_codes: Set[String] = _
  @BeanProperty var resource_codes: Set[String] = _

}

object EZ_Role extends SecureStorage[EZ_Role] with StatusStorage[EZ_Role] {

  // 资源关联表
  var TABLE_REL_ROLE_RESOURCE = "ez_rel_role_resource"

  // 默认系统管理员角色
  val SYSTEM_ROLE_FLAG = "system"
  // 组织管理员角色
  val ORG_ADMIN_ROLE_FLAG = "org_admin"
  // 默认普通用户角色
  val USER_ROLE_FLAG = "user"

  def apply(flag: String, name: String, resourceCodes: Set[String], organizationCode: String = ServiceAdapter.defaultOrganizationCode): EZ_Role = {
    val role = EZ_Role()
    role.flag = flag
    role.name = name.x
    role.resource_codes = resourceCodes
    role.organization_code = organizationCode
    role.enable = true
    role
  }

  override def preSave(model: EZ_Role): Resp[EZ_Role] = {
    preSaveOrUpdate(model)
  }

  override def preUpdate(model: EZ_Role): Resp[EZ_Role] = {
    preSaveOrUpdate(model)
  }

  override def preSaveOrUpdate(model: EZ_Role): Resp[EZ_Role] = {
    model.exchange_resource_codes = model.resource_codes
    model.resource_codes = null
    if (model.id == null || model.id.trim == "") {
      if (model.flag == null || model.flag.trim.isEmpty) {
        logger.warn(s"Require【flag】")
        Resp.badRequest("Require【flag】")
      } else {
        if (model.flag.contains(BaseModel.SPLIT)) {
          logger.warn(s"【uri】can't contains ${BaseModel.SPLIT}")
          Resp.badRequest(s"【flag】can't contains ${BaseModel.SPLIT}")
        } else {
          model.code = assembleCode(model.flag, model.organization_code)
          if (model.organization_code == null) {
            model.organization_code = ServiceAdapter.defaultOrganizationCode
          }
          super.preSave(model)
        }
      }
    } else {
      if (!EZ_Role.existById(model.id).body) {
        Resp.notFound("")
      } else {
        model.code = null
        model.flag = null
        model.organization_code = null
        super.preUpdate(model)
      }
    }
  }

  override def postSave(saveResult: EZ_Role, preResult: EZ_Role): Resp[EZ_Role] = {
    postSaveOrUpdate(saveResult, preResult)
  }

  override def postUpdate(updateResult: EZ_Role, preResult: EZ_Role): Resp[EZ_Role] = {
    postSaveOrUpdate(updateResult, preResult)
  }

  override def postSaveOrUpdate(saveOrUpdateResult: EZ_Role, preResult: EZ_Role): Resp[EZ_Role] = {
    if (preResult.id == null || preResult.id.trim == "") {
      saveOrUpdateRelRoleData(saveOrUpdateResult.code, preResult.exchange_resource_codes)
      saveOrUpdateResult.resource_codes = preResult.exchange_resource_codes
      if (saveOrUpdateResult.enable) {
        postAddExt(saveOrUpdateResult)
      }
      super.postSave(saveOrUpdateResult, preResult)
    } else {
      if (preResult.exchange_resource_codes != null && preResult.exchange_resource_codes.nonEmpty) {
        saveOrUpdateRelRoleData(saveOrUpdateResult.code, preResult.exchange_resource_codes)
        saveOrUpdateResult.resource_codes = preResult.exchange_resource_codes
      } else {
        saveOrUpdateResult.resource_codes = getRelRoleData(saveOrUpdateResult.code).body
      }
      if (saveOrUpdateResult.enable) {
        postAddExt(saveOrUpdateResult)
      } else {
        preRemoveExt(saveOrUpdateResult, isDelete = false)
      }
      super.postUpdate(saveOrUpdateResult, preResult)
    }
  }

  override def postEnableById(id: Any): Resp[Void] = {
    postAddExt(doGetById(id).body)
    super.postEnableById(id)
  }

  override def postEnableByUUID(uuid: String): Resp[Void] = {
    postAddExt(doGetByUUID(uuid).body)
    super.postEnableByUUID(uuid)
  }

  override def postEnableByCond(condition: String, parameters: List[Any]): Resp[Void] = {
    doFind(condition, parameters).body.foreach(postAddExt)
    super.postEnableByCond(condition, parameters)
  }

  override def postDisableById(id: Any): Resp[Void] = {
    preRemoveExt(doGetById(id).body, isDelete = false)
    super.postDisableById(id)
  }

  override def postDisableByUUID(uuid: String): Resp[Void] = {
    preRemoveExt(doGetByUUID(uuid).body, isDelete = false)
    super.postDisableByUUID(uuid)
  }

  override def preDisableByCond(condition: String, parameters: List[Any]): Resp[(String, List[Any])] = {
    doFind(condition, parameters).body.foreach(preRemoveExt(_, isDelete = false))
    super.preDisableByCond(condition, parameters)
  }

  override def postDeleteById(id: Any): Resp[Void] = {
    preRemoveExt(doGetById(id).body, isDelete = true)
    super.postDeleteById(id)
  }

  override def postDeleteByUUID(uuid: String): Resp[Void] = {
    preRemoveExt(doGetByUUID(uuid).body, isDelete = true)
    super.postDeleteByUUID(uuid)
  }

  override def preDeleteByCond(condition: String, parameters: List[Any]): Resp[(String, List[Any])] = {
    doFind(condition, parameters).body.foreach(preRemoveExt(_, isDelete = true))
    super.preDeleteByCond(condition, parameters)
  }

  override def postGetEnabledByCond(condition: String, parameters: List[Any], getResult: EZ_Role): Resp[EZ_Role] = {
    postGetExt(getResult)
    super.postGetEnabledByCond(condition, parameters, getResult)
  }

  override def postGetById(id: Any, getResult: EZ_Role): Resp[EZ_Role] = {
    postGetExt(getResult)
    super.postGetById(id, getResult)
  }

  override def postGetByUUID(uuid: String, getResult: EZ_Role): Resp[EZ_Role] = {
    postGetExt(getResult)
    super.postGetByUUID(uuid, getResult)
  }

  override def postGetByCond(condition: String, parameters: List[Any], getResult: EZ_Role): Resp[EZ_Role] = {
    postGetExt(getResult)
    super.postGetByCond(condition, parameters, getResult)
  }

  override def postFindEnabled(condition: String, parameters: List[Any], findResult: List[EZ_Role]): Resp[List[EZ_Role]] = {
    postSearchExt(findResult)
    super.postFindEnabled(condition, parameters, findResult)
  }

  override def postPageEnabled(condition: String, parameters: List[Any],
                               pageNumber: Long, pageSize: Int, pageResult: Page[EZ_Role]): Resp[Page[EZ_Role]] = {
    postSearchExt(pageResult.objects)
    super.postPageEnabled(condition, parameters, pageNumber, pageSize, pageResult)
  }

  override def postFind(condition: String, parameters: List[Any], findResult: List[EZ_Role]): Resp[List[EZ_Role]] = {
    postSearchExt(findResult)
    super.postFind(condition, parameters, findResult)
  }

  override def postPage(condition: String, parameters: List[Any],
                        pageNumber: Long, pageSize: Int, pageResult: Page[EZ_Role]): Resp[Page[EZ_Role]] = {
    postSearchExt(pageResult.objects)
    super.postPage(condition, parameters, pageNumber, pageSize, pageResult)
  }

  def deleteByCode(code: String): Resp[Void] = {
    deleteByCond( s"""code = ?""", List(code))
  }

  override def preUpdateByCond(newValues: String, condition: String, parameters: List[Any]): Resp[(String, String, List[Any])] =
    Resp.notImplemented("")

  def assembleCode(flag: String, organization_code: String): String = {
    organization_code + BaseModel.SPLIT + flag
  }

  def findByCodes(codes: List[String]): Resp[List[EZ_Role]] = {
    if (codes != null && codes.nonEmpty) {
      find( s"""code IN (${codes.map(_ => "?").mkString(",")})""", codes)
    } else {
      Resp.success(List())
    }
  }

  def getByCode(code: String): Resp[EZ_Role] = {
    getByCond(s"""code = ?""", List(code))
  }

  def saveOrUpdateRelRoleData(roleCode: String, resourceRoles: Set[String]): Resp[Void] = {
    deleteRelRoleData(roleCode)
    JDBCProcessor.batch(
      s"""INSERT INTO ${EZ_Role.TABLE_REL_ROLE_RESOURCE} ( role_code , resource_code ) VALUES ( ? , ? )""",
      resourceRoles.map {
        resourceRole => List(roleCode, resourceRole)
      }.toList)
  }

  def deleteRelRoleData(roleCode: String): Resp[Void] = {
    JDBCProcessor.update(s"""DELETE FROM ${EZ_Role.TABLE_REL_ROLE_RESOURCE} WHERE role_code  = ? """, List(roleCode))
  }

  def getRelRoleData(roleCode: String): Resp[Set[String]] = {
    val resp = JDBCProcessor.find(
      s"""SELECT resource_code FROM ${EZ_Role.TABLE_REL_ROLE_RESOURCE} WHERE role_code  = ? """,
      List(roleCode), classOf[JsonObject])
    if (resp) {
      Resp.success(resp.body.map(_.getString("resource_code")))
    } else {
      resp
    }
  }

  private def postSearchExt(findResult: List[EZ_Role]): Unit = {
    findResult.foreach {
      result =>
        postGetExt(result)
    }
  }

  private def postGetExt(getResult: EZ_Role): Unit = {
    if (getResult != null) {
      getResult.resource_codes = getRelRoleData(getResult.code).body
    }
  }

  private def postAddExt(obj: EZ_Role): Unit = {
    if (obj != null) {
      CacheManager.RBAC.addRole(obj)
    }
  }

  private def preRemoveExt(obj: EZ_Role, isDelete: Boolean): Unit = {
    if (obj != null) {
      CacheManager.RBAC.removeRole(obj.code)
      if (isDelete) {
        deleteRelRoleData(obj.code)
      }
    }
  }

}


