package com.ecfront.ez.framework.service.auth.model

import com.ecfront.common.{Ignore, Resp}
import com.ecfront.ez.framework.service.auth.ServiceAdapter
import com.ecfront.ez.framework.service.jdbc._

import scala.beans.BeanProperty

/**
  * 菜单实体
  */
@Entity("Menu")
case class EZ_Menu() extends SecureModel with StatusModel with OrganizationModel {

  @Unique
  @Desc("Code", 200, 0) // organization_code@uri
  @BeanProperty var code: String = _
  @Require
  @Desc("URI", 100, 0)
  @BeanProperty var uri: String = _
  @Require
  @Desc("Name", 200, 0)
  @BeanProperty var name: String = _
  @Desc("Icon", 100, 0)
  @BeanProperty var icon: String = _
  @Desc("Translate", 200, 0)
  @BeanProperty var translate: String = _
  @Ignore var exchange_role_codes: List[String] = _
  @BeanProperty
  @Ignore var role_codes: List[String] = _
  @Desc("Parent Code", 200, 0)
  @BeanProperty var parent_code: String = _
  @Desc("Sort", 0, 0)
  @BeanProperty var sort: Int = 0

}

object EZ_Menu extends SecureStorage[EZ_Menu] with StatusStorage[EZ_Menu] with OrganizationStorage[EZ_Menu] {

  // 角色关联表
  var TABLE_REL_MENU_ROLE = "ez_rel_menu_role"

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

  override def preSave(model: EZ_Menu): Resp[EZ_Menu] = {
    preSaveOrUpdate(model)
  }

  override def preUpdate(model: EZ_Menu): Resp[EZ_Menu] = {
    preSaveOrUpdate(model)
  }

  override def preSaveOrUpdate(model: EZ_Menu): Resp[EZ_Menu] = {
    if (model.id == null || model.id.trim == "") {
      if (model.uri.contains(BaseModel.SPLIT)) {
        logger.warn(s"【uri】can't contains ${BaseModel.SPLIT}")
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
        model.exchange_role_codes = model.role_codes
        model.role_codes = null
        super.preSave(model)
      }
    } else {
      if (!EZ_Menu.existById(model.id).body) {
        Resp.notFound("")
      } else {
        model.code = null
        model.uri = null
        model.organization_code = null
        model.exchange_role_codes = model.role_codes
        model.role_codes = null
        super.preUpdate(model)
      }
    }
  }

  override def postSave(saveResult: EZ_Menu, preResult: EZ_Menu): Resp[EZ_Menu] = {
    postSaveOrUpdate(saveResult, preResult)
  }

  override def postUpdate(updateResult: EZ_Menu, preResult: EZ_Menu): Resp[EZ_Menu] = {
    postSaveOrUpdate(updateResult, preResult)
  }

  override def postSaveOrUpdate(saveOrUpdateResult: EZ_Menu, preResult: EZ_Menu): Resp[EZ_Menu] = {
    if (preResult.id == null || preResult.id.trim == "") {
      saveOrUpdateRelRoleData(saveOrUpdateResult.code, preResult.exchange_role_codes)
      saveOrUpdateResult.role_codes = preResult.exchange_role_codes
      super.postSave(saveOrUpdateResult, preResult)
    } else {
      if (preResult.exchange_role_codes != null && preResult.exchange_role_codes.nonEmpty) {
        saveOrUpdateRelRoleData(saveOrUpdateResult.code, preResult.exchange_role_codes)
        saveOrUpdateResult.role_codes = preResult.exchange_role_codes
      } else {
        saveOrUpdateResult.role_codes = getRelRoleData(saveOrUpdateResult.code).body
      }
      super.postUpdate(saveOrUpdateResult, preResult)
    }
  }

  override def postGetEnabledByCond(condition: String, parameters: List[Any], getResult: EZ_Menu): Resp[EZ_Menu] = {
    postGetX(getResult)
    super.postGetEnabledByCond(condition, parameters, getResult)
  }

  override def postGetById(id: Any, getResult: EZ_Menu): Resp[EZ_Menu] = {
    postGetX(getResult)
    super.postGetById(id, getResult)
  }

  override def postGetByUUID(uuid: String, getResult: EZ_Menu): Resp[EZ_Menu] = {
    postGetX(getResult)
    super.postGetByUUID(uuid, getResult)
  }

  override def postGetByCond(condition: String, parameters: List[Any], getResult: EZ_Menu): Resp[EZ_Menu] = {
    postGetX(getResult)
    super.postGetByCond(condition, parameters, getResult)
  }

  override def postFindEnabled(condition: String, parameters: List[Any], findResult: List[EZ_Menu]): Resp[List[EZ_Menu]] = {
    postSearch(findResult)
    super.postFindEnabled(condition, parameters, findResult)
  }

  override def postPageEnabled(condition: String, parameters: List[Any],
                               pageNumber: Long, pageSize: Int, pageResult: Page[EZ_Menu]): Resp[Page[EZ_Menu]] = {
    postSearch(pageResult.objects)
    super.postPageEnabled(condition, parameters, pageNumber, pageSize, pageResult)
  }

  override def postFind(condition: String, parameters: List[Any], findResult: List[EZ_Menu]): Resp[List[EZ_Menu]] = {
    postSearch(findResult)
    super.postFind(condition, parameters, findResult)
  }

  override def postPage(condition: String, parameters: List[Any],
                        pageNumber: Long, pageSize: Int, pageResult: Page[EZ_Menu]): Resp[Page[EZ_Menu]] = {
    postSearch(pageResult.objects)
    super.postPage(condition, parameters, pageNumber, pageSize, pageResult)
  }

  protected def postSearch(findResult: List[EZ_Menu]): Unit = {
    findResult.foreach {
      result =>
        postGetX(result)
    }
  }

  protected def postGetX(getResult: EZ_Menu): Unit = {
    if (getResult != null) {
      getResult.role_codes = getRelRoleData(getResult.code).body
    }
  }

  override def preDeleteById(id: Any): Resp[Any] = {
    val objR = doGetById(id)
    if (objR && objR.body != null) {
      deleteRelRoleData(objR.body.code)
    }
    super.preDeleteById(id)
  }

  override def preDeleteByUUID(uuid: String): Resp[String] = {
    val objR = doGetByUUID(uuid)
    if (objR && objR.body != null) {
      deleteRelRoleData(objR.body.code)
    }
    super.preDeleteByUUID(uuid)
  }

  override def preDeleteByCond(condition: String, parameters: List[Any]): Resp[(String, List[Any])] = {
    val objR = doFind(condition, parameters)
    if (objR && objR.body != null && objR.body.nonEmpty) {
      objR.body.foreach {
        obj =>
          deleteRelRoleData(obj.code)
      }
    }
    super.preDeleteByCond(condition, parameters)
  }

  override def preUpdateByCond(newValues: String, condition: String, parameters: List[Any]): Resp[(String, String, List[Any])] =
    Resp.notImplemented("")

  def assembleCode(uri: String, organizationCode: String): String = {
    organizationCode + BaseModel.SPLIT + uri
  }

  def findWithSort(): Resp[List[EZ_Menu]] = {
    find(s"1=1 ORDER BY sort DESC")
  }

  def findEnableWithSort(): Resp[List[EZ_Menu]] = {
    find(s"enable = ? ORDER BY sort DESC", List(true))
  }

  def findByOrganizationCodeWithSort(organizationCode: String): Resp[List[EZ_Menu]] = {
    find(s"organization_code = ? ORDER BY sort DESC", List(organizationCode))
  }

  def findEnableByOrganizationCodeWithSort(organizationCode: String): Resp[List[EZ_Menu]] = {
    find(s"enable = ? AND organization_code = ? ORDER BY sort DESC", List(true, organizationCode))
  }

  def saveOrUpdateRelRoleData(menuCode: String, roleCodes: List[String]): Resp[Void] = {
    deleteRelRoleData(menuCode)
    JDBCProcessor.batch(
      s"""INSERT INTO ${EZ_Menu.TABLE_REL_MENU_ROLE} ( menu_code , role_code ) VALUES ( ? , ? )""",
      roleCodes.map {
        roleCode => List(menuCode, roleCode)
      })
  }

  def deleteRelRoleData(menuCode: String): Resp[Void] = {
    JDBCProcessor.update(s"""DELETE FROM ${EZ_Menu.TABLE_REL_MENU_ROLE} WHERE menu_code  = ? """, List(menuCode))
  }

  def getRelRoleData(menuCode: String): Resp[List[String]] = {
    val resp = JDBCProcessor.find(
      s"""SELECT role_code FROM ${EZ_Menu.TABLE_REL_MENU_ROLE} WHERE menu_code  = ? """,
      List(menuCode))
    if (resp) {
      Resp.success(resp.body.map(_ ("role_code").asInstanceOf[String]))
    } else {
      resp
    }
  }

}


