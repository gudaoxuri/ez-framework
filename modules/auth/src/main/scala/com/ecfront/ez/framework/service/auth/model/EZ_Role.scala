package com.ecfront.ez.framework.service.auth.model

import com.ecfront.common.Resp
import com.ecfront.ez.framework.service.auth.ServiceAdapter
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
  @Label("Code")
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

  val SYSTEM_ROLE_CODE = "system"
  val USER_ROLE_CODE = "user"

  override protected val storageObj: EZ_Role_Base =
    if (ServiceAdapter.mongoStorage) EZ_Role_Mongo else EZ_Role_JDBC

  def apply(flag: String, name: String, resourceCodes: List[String]): EZ_Role = {
    val role = EZ_Role()
    role.flag = flag
    role.name = name
    role.organization_code = ""
    role.enable = true
    role.resource_codes = resourceCodes
    role
  }

  override def findByCodes(codes: List[String]): Resp[List[EZ_Role]] = storageObj.findByCodes(codes)

  override def getByCode(code: String): Resp[EZ_Role] = storageObj.getByCode(code)

  override def deleteByCode(code: String): Resp[Void] = storageObj.deleteByCode(code)

}

trait EZ_Role_Base extends SecureStorage[EZ_Role] with StatusStorage[EZ_Role] {

  override def preSave(model: EZ_Role, context: EZStorageContext): Resp[EZ_Role] = {
    if (model.flag == null || model.flag.trim.isEmpty) {
      Resp.badRequest("Require【flag】")
    } else {
      model.code = assembleCode(model.flag, model.organization_code)
      super.preSave(model, context)
    }
  }

  override def preUpdate(model: EZ_Role, context: EZStorageContext): Resp[EZ_Role] = {
    if (model.flag == null || model.flag.trim.isEmpty) {
      Resp.badRequest("Require【flag】")
    } else {
      model.code = assembleCode(model.flag, model.organization_code)
      super.preUpdate(model, context)
    }
  }

  def assembleCode(flag: String, organization_code: String): String = {
    organization_code + BaseModel.SPLIT + flag
  }

  def findByCodes(codes: List[String]): Resp[List[EZ_Role]]

  def getByCode(code: String): Resp[EZ_Role]

  def deleteByCode(code: String): Resp[Void]

}

object EZ_Role_Mongo extends MongoSecureStorage[EZ_Role] with MongoStatusStorage[EZ_Role] with EZ_Role_Base {

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



