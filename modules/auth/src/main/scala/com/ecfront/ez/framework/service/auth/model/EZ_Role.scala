package com.ecfront.ez.framework.service.auth.model

import com.ecfront.common.Resp
import com.ecfront.ez.framework.service.auth.{CacheManager, ServiceAdapter}
import com.ecfront.ez.framework.service.storage.foundation._
import com.ecfront.ez.framework.service.storage.jdbc.{JDBCSecureStorage, JDBCStatusStorage}
import com.ecfront.ez.framework.service.storage.mongo.{MongoSecureStorage, MongoStatusStorage}

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
  @BeanProperty var resource_codes: List[String] = List[String]()
  @BeanProperty var organization_code: String = _

}

object EZ_Role extends SecureStorageAdapter[EZ_Role, EZ_Role_Base]
  with StatusStorageAdapter[EZ_Role, EZ_Role_Base] with EZ_Role_Base {

  // 默认系统管理员角色
  val SYSTEM_ROLE_FLAG = "system"
  // 默认普通用户角色
  val USER_ROLE_FLAG = "user"

  override protected val storageObj: EZ_Role_Base =
    if (ServiceAdapter.mongoStorage) EZ_Role_Mongo else EZ_Role_JDBC

  def apply(flag: String, name: String, resourceCodes: List[String],organizationCode:String=ServiceAdapter.defaultOrganizationCode): EZ_Role = {
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

}

trait EZ_Role_Base extends SecureStorage[EZ_Role] with StatusStorage[EZ_Role] {

  override def preSave(model: EZ_Role, context: EZStorageContext): Resp[EZ_Role] = {
    preSaveOrUpdate(model, context)
  }

  override def preUpdate(model: EZ_Role, context: EZStorageContext): Resp[EZ_Role] = {
    preSaveOrUpdate(model, context)
  }

  override def preSaveOrUpdate(model: EZ_Role, context: EZStorageContext): Resp[EZ_Role] = {
    if (model.flag == null || model.flag.trim.isEmpty) {
      Resp.badRequest("Require【flag】")
    } else {
      if (model.flag.contains(BaseModel.SPLIT)) {
        Resp.badRequest(s"【flag】can't contains ${BaseModel.SPLIT}")
      } else {
        if (model.organization_code == null) {
          model.organization_code = ServiceAdapter.defaultOrganizationCode
        }
        if (model.resource_codes == null) {
          model.resource_codes = List()
        }
        model.code = assembleCode(model.flag, model.organization_code)
        super.preSaveOrUpdate(model, context)
      }
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

  override def postSave(saveResult: EZ_Role, context: EZStorageContext): Resp[EZ_Role] = {
    if (saveResult.enable) {
      CacheManager.addResourceByRole(saveResult.code, saveResult.resource_codes)
    }
    super.postSave(saveResult, context)
  }

  override def postUpdate(updateResult: EZ_Role, context: EZStorageContext): Resp[EZ_Role] = {
    if (updateResult.enable) {
      CacheManager.addResourceByRole(updateResult.code, updateResult.resource_codes)
    }
    super.postUpdate(updateResult, context)
  }

  override def postSaveOrUpdate(saveOrUpdateResult: EZ_Role, context: EZStorageContext): Resp[EZ_Role] = {
    if (saveOrUpdateResult.enable) {
      CacheManager.addResourceByRole(saveOrUpdateResult.code, saveOrUpdateResult.resource_codes)
    }
    super.postSaveOrUpdate(saveOrUpdateResult, context)
  }

  override def postDeleteById(id: Any, context: EZStorageContext): Resp[Void] = {
    val role = super.getById(id).body
    CacheManager.removeResourceByRole(role.code)
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

}



