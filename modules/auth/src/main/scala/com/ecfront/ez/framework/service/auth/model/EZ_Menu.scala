package com.ecfront.ez.framework.service.auth.model

import com.ecfront.common.{Ignore, Resp}
import com.ecfront.ez.framework.service.auth.ServiceAdapter
import com.ecfront.ez.framework.service.storage.foundation._
import com.ecfront.ez.framework.service.storage.jdbc.{JDBCProcessor, JDBCSecureStorage, JDBCStatusStorage}
import com.ecfront.ez.framework.service.storage.mongo.{MongoProcessor, MongoSecureStorage, MongoStatusStorage, SortEnum}
import io.vertx.core.json.JsonObject

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
  @BeanProperty var icon: String = _
  @BeanProperty var translate: String = _
  @Ignore var exchange_role_codes: List[String] = _
  @BeanProperty var role_codes: List[String] = _
  @BeanProperty var parent_code: String = _
  @BeanProperty var sort: Int = 0
  @BeanProperty var organization_code: String =_

}

object EZ_Menu extends SecureStorageAdapter[EZ_Menu, EZ_Menu_Base]
  with StatusStorageAdapter[EZ_Menu, EZ_Menu_Base] with EZ_Menu_Base {

  // 角色关联表，在useRelTable=true中启用
  var TABLE_REL_MENU_ROLE = "ez_rel_menu_role"

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
    menu.organization_code = ServiceAdapter.defaultOrganizationCode
    menu.enable = true
    menu
  }

  override def findWithSort(): Resp[List[EZ_Menu]] = storageObj.findWithSort()

  override def findEnableWithSort(): Resp[List[EZ_Menu]] = storageObj.findEnableWithSort()

  override def findByOrganizationCodeWithSort(organizationCode: String): Resp[List[EZ_Menu]] =
    storageObj.findByOrganizationCodeWithSort(organizationCode)

  override def findEnableByOrganizationCodeWithSort(organizationCode: String): Resp[List[EZ_Menu]] =
    storageObj.findEnableByOrganizationCodeWithSort(organizationCode)

  override def saveOrUpdateRelRoleData(menuCode: String, roleCodes: List[String]): Resp[Void] = storageObj.saveOrUpdateRelRoleData(menuCode, roleCodes)

  override def deleteRelRoleData(menuCode: String): Resp[Void] = storageObj.deleteRelRoleData(menuCode)

  override def getRelRoleData(menuCode: String): Resp[List[String]] = storageObj.getRelRoleData(menuCode)

}

trait EZ_Menu_Base extends SecureStorage[EZ_Menu] with StatusStorage[EZ_Menu] {

  override def preSave(model: EZ_Menu, context: EZStorageContext): Resp[EZ_Menu] = {
    preSaveOrUpdate(model, context)
  }

  override def preUpdate(model: EZ_Menu, context: EZStorageContext): Resp[EZ_Menu] = {
    preSaveOrUpdate(model, context)
  }

  override def preSaveOrUpdate(model: EZ_Menu, context: EZStorageContext): Resp[EZ_Menu] = {
    if (model.id == null || model.id.trim == "") {
      if (model.uri.contains(BaseModel.SPLIT)) {
        Resp.badRequest(s"【uri】can't contains ${BaseModel.SPLIT}")
      } else {
        model.code = assembleCode(model.uri, model.organization_code)
        if (model.organization_code == null) {
          model.organization_code = ServiceAdapter.defaultOrganizationCode
        }
        if (model.parent_code == null) {
          model.parent_code = ""
        }
        if (model.icon == null) {
          model.icon = ""
        }
        if (model.translate == null) {
          model.translate = ""
        }
        if (model.role_codes == null) {
          model.role_codes = List()
        }
        if (ServiceAdapter.useRelTable) {
          model.exchange_role_codes = model.role_codes
          model.role_codes = null
        }
        super.preSave(model, context)
      }
    } else {
      if (!EZ_Menu.existById(model.id).body) {
        Resp.notFound("")
      } else {
        model.code = null
        model.uri = null
        model.organization_code = null
        if (ServiceAdapter.useRelTable) {
          model.exchange_role_codes = model.role_codes
          model.role_codes = null
        }
        super.preUpdate(model, context)
      }
    }
  }

  override def postSave(saveResult: EZ_Menu, preResult: EZ_Menu, context: EZStorageContext): Resp[EZ_Menu] = {
    postSaveOrUpdate(saveResult, preResult, context)
  }

  override def postUpdate(updateResult: EZ_Menu, preResult: EZ_Menu, context: EZStorageContext): Resp[EZ_Menu] = {
    postSaveOrUpdate(updateResult, preResult, context)
  }

  override def postSaveOrUpdate(saveOrUpdateResult: EZ_Menu, preResult: EZ_Menu, context: EZStorageContext): Resp[EZ_Menu] = {
    if (preResult.id == null || preResult.id.trim == "") {
      if (ServiceAdapter.useRelTable) {
        saveOrUpdateRelRoleData(saveOrUpdateResult.code, preResult.exchange_role_codes)
        saveOrUpdateResult.role_codes = preResult.exchange_role_codes
      }
      super.postUpdate(saveOrUpdateResult, preResult, context)
    } else {
      if (ServiceAdapter.useRelTable) {
        if (preResult.exchange_role_codes != null && preResult.exchange_role_codes.nonEmpty) {
          saveOrUpdateRelRoleData(saveOrUpdateResult.code, preResult.exchange_role_codes)
          saveOrUpdateResult.role_codes = preResult.exchange_role_codes
        } else {
          saveOrUpdateResult.role_codes = getRelRoleData(saveOrUpdateResult.code).body
        }
      }
      super.postSave(saveOrUpdateResult, preResult, context)
    }
  }

  override def postGetEnabledByCond(condition: String, parameters: List[Any], getResult: EZ_Menu, context: EZStorageContext): Resp[EZ_Menu] = {
    postGetX(getResult)
    super.postGetEnabledByCond(condition, parameters, getResult, context)
  }

  override def postGetById(id: Any, getResult: EZ_Menu, context: EZStorageContext): Resp[EZ_Menu] = {
    postGetX(getResult)
    super.postGetById(id, getResult, context)
  }

  override def postGetByCond(condition: String, parameters: List[Any], getResult: EZ_Menu, context: EZStorageContext): Resp[EZ_Menu] = {
    postGetX(getResult)
    super.postGetByCond(condition, parameters, getResult, context)
  }

  override def postFindEnabled(condition: String, parameters: List[Any], findResult: List[EZ_Menu], context: EZStorageContext): Resp[List[EZ_Menu]] = {
    postSearch(findResult)
    super.postFindEnabled(condition, parameters, findResult, context)
  }

  override def postPageEnabled(condition: String, parameters: List[Any],
                               pageNumber: Long, pageSize: Int, pageResult: Page[EZ_Menu], context: EZStorageContext): Resp[Page[EZ_Menu]] = {
    postSearch(pageResult.objects)
    super.postPageEnabled(condition, parameters, pageNumber, pageSize, pageResult, context)
  }

  override def postFind(condition: String, parameters: List[Any], findResult: List[EZ_Menu], context: EZStorageContext): Resp[List[EZ_Menu]] = {
    postSearch(findResult)
    super.postFind(condition, parameters, findResult, context)
  }

  override def postPage(condition: String, parameters: List[Any],
                        pageNumber: Long, pageSize: Int, pageResult: Page[EZ_Menu], context: EZStorageContext): Resp[Page[EZ_Menu]] = {
    postSearch(pageResult.objects)
    super.postPage(condition, parameters, pageNumber, pageSize, pageResult, context)
  }

  protected def postSearch(findResult: List[EZ_Menu]): Unit = {
    findResult.foreach {
      result =>
        postGetX(result)
    }
  }

  protected def postGetX(getResult: EZ_Menu): Unit = {
    if (getResult != null) {
      if (ServiceAdapter.useRelTable) {
        getResult.role_codes = getRelRoleData(getResult.code).body
      }
    }
  }

  override def preDeleteById(id: Any, context: EZStorageContext): Resp[Any] = {
    if (ServiceAdapter.useRelTable) {
      val objR = doGetById(id, context)
      if (objR && objR.body != null) {
        deleteRelRoleData(objR.body.code)
      }
    }
    super.preDeleteById(id, context)
  }

  override def preDeleteByCond(condition: String, parameters: List[Any],
                               context: EZStorageContext): Resp[(String, List[Any])] = {
    if (ServiceAdapter.useRelTable) {
      val objR = doFind(condition, parameters, context)
      if (objR && objR.body != null && objR.body.nonEmpty) {
        objR.body.foreach {
          obj =>
            deleteRelRoleData(obj.code)
        }
      }
    }
    super.preDeleteByCond(condition, parameters, context)
  }

  override def preUpdateByCond(newValues: String, condition: String, parameters: List[Any], context: EZStorageContext): Resp[(String, String, List[Any])] =
    Resp.notImplemented("")

  def assembleCode(uri: String, organizationCode: String): String = {
    organizationCode + BaseModel.SPLIT + uri
  }

  def findWithSort(): Resp[List[EZ_Menu]]

  def findEnableWithSort(): Resp[List[EZ_Menu]]

  def findByOrganizationCodeWithSort(organizationCode: String): Resp[List[EZ_Menu]]

  def findEnableByOrganizationCodeWithSort(organizationCode: String): Resp[List[EZ_Menu]]

  def saveOrUpdateRelRoleData(menuCode: String, roleCodes: List[String]): Resp[Void]

  def deleteRelRoleData(menuCode: String): Resp[Void]

  def getRelRoleData(menuCode: String): Resp[List[String]]

}

object EZ_Menu_Mongo extends MongoSecureStorage[EZ_Menu] with MongoStatusStorage[EZ_Menu] with EZ_Menu_Base {

  override def findWithSort(): Resp[List[EZ_Menu]] = {
    val resp = findWithOpt(s"""{}""", Map("sort" -> SortEnum.DESC))
    postSearch(resp.body)
    resp
  }

  override def findEnableWithSort(): Resp[List[EZ_Menu]] = {
    val resp = findWithOpt(s"""{"enable":true}""", Map("sort" -> SortEnum.DESC))
    postSearch(resp.body)
    resp
  }

  override def findByOrganizationCodeWithSort(organizationCode: String): Resp[List[EZ_Menu]] = {
    val resp = findWithOpt(s"""{"organization_code":"$organizationCode"}""", Map("sort" -> SortEnum.DESC))
    postSearch(resp.body)
    resp
  }

  override def findEnableByOrganizationCodeWithSort(organizationCode: String): Resp[List[EZ_Menu]] = {
    val resp = findWithOpt(s"""{"enable":true,"organization_code":"$organizationCode"}""", Map("sort" -> SortEnum.DESC))
    postSearch(resp.body)
    resp
  }

  override def saveOrUpdateRelRoleData(menuCode: String, roleCodes: List[String]): Resp[Void] = {
    deleteRelRoleData(menuCode)
    roleCodes.foreach {
      roleCode =>
        MongoProcessor.save(EZ_Menu.TABLE_REL_MENU_ROLE,
          new JsonObject(s"""{"menu_code":"$menuCode","role_code":"$roleCode"}""")
        )
    }
    Resp.success(null)
  }

  override def deleteRelRoleData(menuCode: String): Resp[Void] = {
    MongoProcessor.deleteByCond(EZ_Menu.TABLE_REL_MENU_ROLE,
      new JsonObject(s"""{"menu_code":"$menuCode"}"""))
  }

  override def getRelRoleData(menuCode: String): Resp[List[String]] = {
    val resp = MongoProcessor.find(EZ_Menu.TABLE_REL_MENU_ROLE,
      new JsonObject(s"""{"menu_code":"$menuCode"}"""), null, 0, classOf[JsonObject])
    if (resp) {
      Resp.success(resp.body.map(_.getString("role_code")))
    } else {
      resp
    }
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

  override def saveOrUpdateRelRoleData(menuCode: String, roleCodes: List[String]): Resp[Void] = {
    deleteRelRoleData(menuCode)
    JDBCProcessor.batch(
      s"""INSERT INTO ${EZ_Menu.TABLE_REL_MENU_ROLE} ( menu_code , role_code ) VALUES ( ? , ? )""",
      roleCodes.map {
        roleCode => List(menuCode, roleCode)
      })
  }

  override def deleteRelRoleData(menuCode: String): Resp[Void] = {
    JDBCProcessor.update(s"""DELETE FROM ${EZ_Menu.TABLE_REL_MENU_ROLE} WHERE menu_code  = ? """, List(menuCode))
  }

  override def getRelRoleData(menuCode: String): Resp[List[String]] = {
    val resp = JDBCProcessor.find(
      s"""SELECT role_code FROM ${EZ_Menu.TABLE_REL_MENU_ROLE} WHERE menu_code  = ? """,
      List(menuCode), classOf[JsonObject])
    if (resp) {
      Resp.success(resp.body.map(_.getString("role_code")))
    } else {
      resp
    }
  }

}



