package com.ecfront.ez.framework.service.auth.model

import com.ecfront.common.Resp
import com.ecfront.ez.framework.service.auth.ServiceAdapter
import com.ecfront.ez.framework.service.storage.foundation._
import com.ecfront.ez.framework.service.storage.jdbc.{JDBCSecureStorage, JDBCStatusStorage}
import com.ecfront.ez.framework.service.storage.mongo.{MongoSecureStorage, MongoStatusStorage, SortEnum}

import scala.beans.BeanProperty

/**
  * 菜单实体
  */
@Entity("Menu")
case class EZ_Menu() extends SecureModel with StatusModel {

  @Unique
  @Label("Code") // organization_code@uri
  @BeanProperty var code: String = _
  @Require
  @Label("URI")
  @BeanProperty var uri: String = _
  @Require
  @Label("Name")
  @BeanProperty var name: String = _
  @BeanProperty var icon: String = ""
  @BeanProperty var translate: String = ""
  @BeanProperty var role_codes: List[String] = List[String]()
  @BeanProperty var parent_code: String = ""
  @BeanProperty var sort: Int = 0
  @BeanProperty var organization_code: String = _

}

object EZ_Menu extends SecureStorageAdapter[EZ_Menu, EZ_Menu_Base]
  with StatusStorageAdapter[EZ_Menu, EZ_Menu_Base] with EZ_Menu_Base {

  override protected val storageObj: EZ_Menu_Base =
    if (ServiceAdapter.mongoStorage) EZ_Menu_Mongo else EZ_Menu_JDBC

  def apply(uri: String, name: String, parent_code: String, roleCodes: List[String], icon: String = "", translate: String = "", sort: Int = 0): EZ_Menu = {
    val menu = EZ_Menu()
    menu.uri = uri
    menu.name = name
    menu.parent_code = parent_code
    menu.icon = icon
    menu.translate = translate
    menu.role_codes = roleCodes
    menu.sort = sort
    menu.organization_code = ""
    menu.enable = true
    menu
  }

  override def findWithSort(): Resp[List[EZ_Menu]] = storageObj.findWithSort()

  override def findEnableWithSort(): Resp[List[EZ_Menu]] = storageObj.findEnableWithSort()

  override def findByOrganizationCodeWithSort(organizationCode: String): Resp[List[EZ_Menu]] =
    storageObj.findByOrganizationCodeWithSort(organizationCode)

  override def findEnableByOrganizationCodeWithSort(organizationCode: String): Resp[List[EZ_Menu]] =
    storageObj.findEnableByOrganizationCodeWithSort(organizationCode)

}

trait EZ_Menu_Base extends SecureStorage[EZ_Menu] with StatusStorage[EZ_Menu] {

  override def preSave(model: EZ_Menu, context: EZStorageContext): Resp[EZ_Menu] = {
    preSaveOrUpdate(model, context)
  }

  override def preUpdate(model: EZ_Menu, context: EZStorageContext): Resp[EZ_Menu] = {
    preSaveOrUpdate(model, context)
  }

  override def preSaveOrUpdate(model: EZ_Menu, context: EZStorageContext): Resp[EZ_Menu] = {
    if (model.uri.contains(BaseModel.SPLIT)) {
      Resp.badRequest(s"【uri】can't contains ${BaseModel.SPLIT}")
    } else {
      if (model.organization_code == null) {
        model.organization_code = ""
      }
      if (model.role_codes == null) {
        model.role_codes = List()
      }
      if (model.icon == null) {
        model.icon = ""
      }
      if (model.translate == null) {
        model.translate = ""
      }
      if (model.parent_code == null) {
        model.parent_code = ""
      }
      model.code = assembleCode(model.uri, model.organization_code)
      super.preSaveOrUpdate(model, context)
    }
  }

  def assembleCode(uri: String, organizationCode: String): String = {
    organizationCode + BaseModel.SPLIT + uri
  }

  def findWithSort(): Resp[List[EZ_Menu]]

  def findEnableWithSort(): Resp[List[EZ_Menu]]

  def findByOrganizationCodeWithSort(organizationCode: String): Resp[List[EZ_Menu]]

  def findEnableByOrganizationCodeWithSort(organizationCode: String): Resp[List[EZ_Menu]]

}

object EZ_Menu_Mongo extends MongoSecureStorage[EZ_Menu] with MongoStatusStorage[EZ_Menu] with EZ_Menu_Base {

  override def findWithSort(): Resp[List[EZ_Menu]] = {
    findWithOpt(s"""{}""", Map("sort" -> SortEnum.DESC))
  }

  override def findEnableWithSort(): Resp[List[EZ_Menu]] = {
    findWithOpt(s"""{"enable":true}""", Map("sort" -> SortEnum.DESC))
  }

  override def findByOrganizationCodeWithSort(organizationCode: String): Resp[List[EZ_Menu]] = {
    findWithOpt(s"""{"organization_code":"$organizationCode"}""", Map("sort" -> SortEnum.DESC))
  }

  override def findEnableByOrganizationCodeWithSort(organizationCode: String): Resp[List[EZ_Menu]] = {
    findWithOpt(s"""{"enable":true,"organization_code":"$organizationCode"}""", Map("sort" -> SortEnum.DESC))
  }

}

object EZ_Menu_JDBC extends JDBCSecureStorage[EZ_Menu] with JDBCStatusStorage[EZ_Menu] with EZ_Menu_Base {

  override def findWithSort(): Resp[List[EZ_Menu]] = {
    find(s"1=1 ORDER BY sort DESC")
  }

  override def findEnableWithSort(): Resp[List[EZ_Menu]] = {
    find(s"enable = ? ORDER BY sort DESC", List(true))
  }

  override def findByOrganizationCodeWithSort(organizationCode: String): Resp[List[EZ_Menu]] = {
    find(s"organization_code = ? ORDER BY sort DESC", List(organizationCode))
  }

  override def findEnableByOrganizationCodeWithSort(organizationCode: String): Resp[List[EZ_Menu]] = {
    find(s"enable = ? AND organization_code = ? ORDER BY sort DESC", List(true, organizationCode))
  }

}



