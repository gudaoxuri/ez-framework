package com.ecfront.ez.framework.service.auth.model

import com.ecfront.common.{Ignore, Resp}
import com.ecfront.ez.framework.service.auth.{CacheManager, ServiceAdapter}
import com.ecfront.ez.framework.service.storage.foundation._
import com.ecfront.ez.framework.service.storage.jdbc.{JDBCProcessor, JDBCSecureStorage, JDBCStatusStorage}
import com.ecfront.ez.framework.service.storage.mongo.{MongoProcessor, MongoSecureStorage, MongoStatusStorage}
import io.vertx.core.json.JsonObject

import scala.beans.BeanProperty

/**
  * 角色实体
  */
@Entity("Role")
case class EZ_Role() extends BaseModel with SecureModel with StatusModel {

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
  @Ignore var exchange_resource_codes: List[String] = _
  @BeanProperty var resource_codes: List[String] = _
  @BeanProperty var organization_code: String = _

}

object EZ_Role extends SecureStorageAdapter[EZ_Role, EZ_Role_Base]
  with StatusStorageAdapter[EZ_Role, EZ_Role_Base] with EZ_Role_Base {

  // 资源关联表，在useRelTable=true中启用
  var TABLE_REL_ROLE_RESOURCE = "ez_rel_role_resource"

  // 默认系统管理员角色
  val SYSTEM_ROLE_FLAG = "system"
  // 默认普通用户角色
  val USER_ROLE_FLAG = "user"

  override protected val storageObj: EZ_Role_Base =
    if (ServiceAdapter.mongoStorage) EZ_Role_Mongo else EZ_Role_JDBC

  def apply(flag: String, name: String, resourceCodes: List[String], organizationCode: String = ServiceAdapter.defaultOrganizationCode): EZ_Role = {
    val role = EZ_Role()
    role.flag = flag
    role.name = name
    role.resource_codes = resourceCodes
    role.organization_code = organizationCode
    role.enable = true
    role
  }

  override def findByOrganizationCode(organizationCode: String): Resp[List[EZ_Role]] = storageObj.findByOrganizationCode(organizationCode)

  override def findEnableByOrganizationCode(organizationCode: String): Resp[List[EZ_Role]] = storageObj.findEnableByOrganizationCode(organizationCode)

  override def findByCodes(codes: List[String]): Resp[List[EZ_Role]] = storageObj.findByCodes(codes)

  override def getByCode(code: String): Resp[EZ_Role] = storageObj.getByCode(code)

  override def deleteByCode(code: String): Resp[Void] = storageObj.deleteByCode(code)

  override def saveOrUpdateRelRoleData(roleCode: String, resourceCodes: List[String]): Resp[Void] = storageObj.saveOrUpdateRelRoleData(roleCode, resourceCodes)

  override def deleteRelRoleData(roleCode: String): Resp[Void] = storageObj.deleteRelRoleData(roleCode)

  override def getRelRoleData(roleCode: String): Resp[List[String]] = storageObj.getRelRoleData(roleCode)

}

trait EZ_Role_Base extends SecureStorage[EZ_Role] with StatusStorage[EZ_Role] {

  override def preSave(model: EZ_Role, context: EZStorageContext): Resp[EZ_Role] = {
    preSaveOrUpdate(model, context)
  }

  override def preUpdate(model: EZ_Role, context: EZStorageContext): Resp[EZ_Role] = {
    preSaveOrUpdate(model, context)
  }

  override def preSaveOrUpdate(model: EZ_Role, context: EZStorageContext): Resp[EZ_Role] = {
    if (ServiceAdapter.useRelTable) {
      model.exchange_resource_codes = model.resource_codes
      model.resource_codes = null
    }
    if (model.id == null || model.id.trim == "") {
      if (model.flag == null || model.flag.trim.isEmpty) {
        Resp.badRequest("Require【flag】")
      } else {
        if (model.flag.contains(BaseModel.SPLIT)) {
          Resp.badRequest(s"【flag】can't contains ${BaseModel.SPLIT}")
        } else {
          model.code = assembleCode(model.flag, model.organization_code)
          if (model.organization_code == null) {
            model.organization_code = ServiceAdapter.defaultOrganizationCode
          }
          super.preSave(model, context)
        }
      }
    } else {
      if (!EZ_Role.existById(model.id).body) {
        Resp.notFound("")
      } else {
        model.code = null
        model.flag = null
        model.organization_code = null
        super.preUpdate(model, context)
      }
    }
  }

  override def postSave(saveResult: EZ_Role, preResult: EZ_Role, context: EZStorageContext): Resp[EZ_Role] = {
    postSaveOrUpdate(saveResult, preResult, context)
  }

  override def postUpdate(updateResult: EZ_Role, preResult: EZ_Role, context: EZStorageContext): Resp[EZ_Role] = {
    postSaveOrUpdate(updateResult, preResult, context)
  }

  override def postSaveOrUpdate(saveOrUpdateResult: EZ_Role, preResult: EZ_Role, context: EZStorageContext): Resp[EZ_Role] = {
    if (preResult.id == null || preResult.id.trim == "") {
      if (ServiceAdapter.useRelTable) {
        saveOrUpdateRelRoleData(saveOrUpdateResult.code, preResult.exchange_resource_codes)
        saveOrUpdateResult.resource_codes = preResult.exchange_resource_codes
      }
      if (saveOrUpdateResult.enable) {
        CacheManager.addResourceByRole(saveOrUpdateResult.code, saveOrUpdateResult.resource_codes)
      }
      super.postUpdate(saveOrUpdateResult, preResult, context)
    } else {
      if (ServiceAdapter.useRelTable) {
        if (preResult.exchange_resource_codes != null && preResult.exchange_resource_codes.nonEmpty) {
          saveOrUpdateRelRoleData(saveOrUpdateResult.code, preResult.exchange_resource_codes)
          saveOrUpdateResult.resource_codes = preResult.exchange_resource_codes
        } else {
          saveOrUpdateResult.resource_codes = getRelRoleData(saveOrUpdateResult.code).body
        }
      }
      if (saveOrUpdateResult.enable) {
        CacheManager.addResourceByRole(saveOrUpdateResult.code, saveOrUpdateResult.resource_codes)
      }
      super.postSave(saveOrUpdateResult, preResult, context)
    }
  }

  override def postEnableById(id: Any, context: EZStorageContext): Resp[Void] = {
    val role = super.getById(id).body
    CacheManager.addResourceByRole(role.code, role.resource_codes)
    super.postEnableById(id, context)
  }

  override def postDisableById(id: Any, context: EZStorageContext): Resp[Void] = {
    val role = super.getById(id).body
    CacheManager.removeResourceByRole(role.code)
    super.postDisableById(id, context)
  }

  override def postGetEnabledByCond(condition: String, parameters: List[Any], getResult: EZ_Role, context: EZStorageContext): Resp[EZ_Role] = {
    postGetX(getResult)
    super.postGetEnabledByCond(condition, parameters, getResult, context)
  }

  override def postGetById(id: Any, getResult: EZ_Role, context: EZStorageContext): Resp[EZ_Role] = {
    postGetX(getResult)
    super.postGetById(id, getResult, context)
  }

  override def postGetByCond(condition: String, parameters: List[Any], getResult: EZ_Role, context: EZStorageContext): Resp[EZ_Role] = {
    postGetX(getResult)
    super.postGetByCond(condition, parameters, getResult, context)
  }

  override def postFindEnabled(condition: String, parameters: List[Any], findResult: List[EZ_Role], context: EZStorageContext): Resp[List[EZ_Role]] = {
    postSearch(findResult)
    super.postFindEnabled(condition, parameters, findResult, context)
  }

  override def postPageEnabled(condition: String, parameters: List[Any],
                               pageNumber: Long, pageSize: Int, pageResult: Page[EZ_Role], context: EZStorageContext): Resp[Page[EZ_Role]] = {
    postSearch(pageResult.objects)
    super.postPageEnabled(condition, parameters, pageNumber, pageSize, pageResult, context)
  }

  override def postFind(condition: String, parameters: List[Any], findResult: List[EZ_Role], context: EZStorageContext): Resp[List[EZ_Role]] = {
    postSearch(findResult)
    super.postFind(condition, parameters, findResult, context)
  }

  override def postPage(condition: String, parameters: List[Any],
                        pageNumber: Long, pageSize: Int, pageResult: Page[EZ_Role], context: EZStorageContext): Resp[Page[EZ_Role]] = {
    postSearch(pageResult.objects)
    super.postPage(condition, parameters, pageNumber, pageSize, pageResult, context)
  }

  protected def postSearch(findResult: List[EZ_Role]): Unit = {
    findResult.foreach {
      result =>
        postGetX(result)
    }
  }

  protected def postGetX(getResult: EZ_Role): Unit = {
    if (getResult != null) {
      if (ServiceAdapter.useRelTable) {
        getResult.resource_codes = getRelRoleData(getResult.code).body
      }
    }
  }

  override def postDeleteById(id: Any, context: EZStorageContext): Resp[Void] = {
    val objR = doGetById(id, context)
    if (objR && objR.body != null) {
      CacheManager.removeResourceByRole(objR.body.code)
      deleteRelRoleData(objR.body.code)
    }
    super.postDeleteById(id, context)
  }

  override def preDeleteByCond(condition: String, parameters: List[Any],
                               context: EZStorageContext): Resp[(String, List[Any])] =
    Resp.notImplemented("")

  override def preUpdateByCond(newValues: String, condition: String, parameters: List[Any],
                               context: EZStorageContext): Resp[(String, String, List[Any])] =
    Resp.notImplemented("")

  def assembleCode(flag: String, organization_code: String): String = {
    organization_code + BaseModel.SPLIT + flag
  }

  def findByOrganizationCode(organizationCode: String): Resp[List[EZ_Role]]

  def findEnableByOrganizationCode(organizationCode: String): Resp[List[EZ_Role]]

  def findByCodes(codes: List[String]): Resp[List[EZ_Role]]

  def getByCode(code: String): Resp[EZ_Role]

  def deleteByCode(code: String): Resp[Void]

  def saveOrUpdateRelRoleData(roleCode: String, resourceRoles: List[String]): Resp[Void]

  def deleteRelRoleData(roleCode: String): Resp[Void]

  def getRelRoleData(roleCode: String): Resp[List[String]]

}

object EZ_Role_Mongo extends MongoSecureStorage[EZ_Role] with MongoStatusStorage[EZ_Role] with EZ_Role_Base {

  override def findByOrganizationCode(organizationCode: String): Resp[List[EZ_Role]] = {
    find(s"""{"organization_code":"$organizationCode"}""")
  }

  override def findEnableByOrganizationCode(organizationCode: String): Resp[List[EZ_Role]] = {
    findEnabled(s"""{"organization_code":"$organizationCode"}""")
  }

  override def findByCodes(codes: List[String]): Resp[List[EZ_Role]] = {
    if (codes != null && codes.nonEmpty) {
      val strCodes = codes.mkString("\"", ",", "\"")
      find( s"""{"code":{"$$in":[$strCodes]}}""")
    } else {
      Resp.success(List())
    }
  }

  override def getByCode(code: String): Resp[EZ_Role] = {
    getByCond(s"""{"code":"$code"}""")
  }

  override def deleteByCode(code: String): Resp[Void] = {
    deleteByCond(s"""{"code":"$code"}""")
  }

  override def saveOrUpdateRelRoleData(roleCode: String, resourceRoles: List[String]): Resp[Void] = {
    deleteRelRoleData(roleCode)
    resourceRoles.foreach {
      resourceRole =>
        MongoProcessor.save(EZ_Role.TABLE_REL_ROLE_RESOURCE,
          new JsonObject(s"""{"role_code":"$roleCode","resource_code":"$resourceRole"}""")
        )
    }
    Resp.success(null)
  }

  override def deleteRelRoleData(roleCode: String): Resp[Void] = {
    MongoProcessor.deleteByCond(EZ_Role.TABLE_REL_ROLE_RESOURCE,
      new JsonObject(s"""{"role_code":"$roleCode"}"""))
  }

  override def getRelRoleData(roleCode: String): Resp[List[String]] = {
    val resp = MongoProcessor.find(EZ_Role.TABLE_REL_ROLE_RESOURCE,
      new JsonObject(s"""{"role_code":"$roleCode"}"""), null, 0, classOf[JsonObject])
    if (resp) {
      Resp.success(resp.body.map(_.getString("resource_code")))
    } else {
      resp
    }
  }

}

object EZ_Role_JDBC extends JDBCSecureStorage[EZ_Role] with JDBCStatusStorage[EZ_Role] with EZ_Role_Base {

  override def findByOrganizationCode(organizationCode: String): Resp[List[EZ_Role]] = {
    find(s"""organization_code = ?""", List(organizationCode))
  }

  override def findEnableByOrganizationCode(organizationCode: String): Resp[List[EZ_Role]] = {
    findEnabled(s"""organization_code = ?""", List(organizationCode))
  }

  override def findByCodes(codes: List[String]): Resp[List[EZ_Role]] = {
    if (codes != null && codes.nonEmpty) {
      find( s"""code IN (${codes.map(_ => "?").mkString(",")})""", codes)
    } else {
      Resp.success(List())
    }
  }

  override def getByCode(code: String): Resp[EZ_Role] = {
    getByCond(s"""code = ?""", List(code))
  }

  override def deleteByCode(code: String): Resp[Void] = {
    deleteByCond( s"""code = ?""", List(code))
  }

  override def saveOrUpdateRelRoleData(roleCode: String, resourceRoles: List[String]): Resp[Void] = {
    deleteRelRoleData(roleCode)
    JDBCProcessor.batch(
      s"""INSERT INTO ${EZ_Role.TABLE_REL_ROLE_RESOURCE} ( role_code , resource_code ) VALUES ( ? , ? )""",
      resourceRoles.map {
        resourceRole => List(roleCode, resourceRole)
      })
  }

  override def deleteRelRoleData(roleCode: String): Resp[Void] = {
    JDBCProcessor.update(s"""DELETE FROM ${EZ_Role.TABLE_REL_ROLE_RESOURCE} WHERE role_code  = ? """, List(roleCode))
  }

  override def getRelRoleData(roleCode: String): Resp[List[String]] = {
    val resp = JDBCProcessor.find(
      s"""SELECT resource_code FROM ${EZ_Role.TABLE_REL_ROLE_RESOURCE} WHERE role_code  = ? """,
      List(roleCode), classOf[JsonObject])
    if (resp) {
      Resp.success(resp.body.map(_.getString("resource_code")))
    } else {
      resp
    }
  }

}



