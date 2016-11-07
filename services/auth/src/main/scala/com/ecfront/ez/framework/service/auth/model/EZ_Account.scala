package com.ecfront.ez.framework.service.auth.model

import java.util.Date

import com.ecfront.common._
import com.ecfront.ez.framework.core.EZ
import com.ecfront.ez.framework.core.i18n.I18NProcessor.Impl
import com.ecfront.ez.framework.service.auth.{CacheManager, ServiceAdapter}
import com.ecfront.ez.framework.service.jdbc._

import scala.beans.BeanProperty

/**
  * 账号实体
  */
@Entity("Account")
case class EZ_Account() extends SecureModel with StatusModel with OrganizationModel {

  @UUID
  @Desc("Code", 50, 0)
  @BeanProperty var code: String = _
  @Require
  @Index
  @Desc("Login Id", 200, 0)
  @BeanProperty var login_id: String = _
  @Require
  @Desc("Name", 200, 0)
  @BeanProperty var name: String = _
  @Desc("Image", 200, 0)
  @BeanProperty var image: String = _
  @Require
  @Desc("Password", 500, 0)
  @BeanProperty var password: String = _
  // 此字段不为空时保存或更新账户时不对密码做加密
  @Ignore var exchange_pwd: String = _
  @Require
  @Index
  @Desc("Email", 100, 0)
  @BeanProperty var email: String = _
  @Ignore var exchange_role_codes: Set[String] = _
  @BeanProperty
  @Ignore var role_codes: Set[String] = _
  @Index
  @Desc("Ext Id", 100, 0) // 用于关联其它对象以扩展属性，扩展Id多为业务系统用户信息表的主键
  @BeanProperty var ext_id: String = _
  @Desc("Ext Info", 0, 0)
  @BeanProperty var ext_info: String = _
  @Desc("OAuth Info", 0, 0) // key=oauth服务标记，value=openid
  @BeanProperty var oauth: String = _

}

object EZ_Account extends SecureStorage[EZ_Account] with StatusStorage[EZ_Account] with OrganizationStorage[EZ_Account] {

  // 角色关联表
  var TABLE_REL_ACCOUNT_ROLE = "ez_rel_account_role"

  val SYSTEM_ACCOUNT_LOGIN_ID = BaseModel.SYSTEM_ACCOUNT_LOGIN_ID

  val ORG_ADMIN_ACCOUNT_LOGIN_ID = BaseModel.ORG_ADMIN_ACCOUNT_LOGIN_ID

  val VIRTUAL_EMAIL = "@virtual.is"

  val LAST_CHANGE_PWD = "last_change_pwd"

  def apply(loginId: String, email: String, name: String, password: String,
            roleCodes: Set[String], organizationCode: String = ServiceAdapter.defaultOrganizationCode): EZ_Account = {
    val account = EZ_Account()
    account.login_id = loginId
    account.email = email
    account.name = name.x
    account.password = password
    account.organization_code = organizationCode
    account.enable = true
    account.role_codes = roleCodes
    account.ext_id = ""
    account.ext_info = "{}"
    account
  }

  override def preSave(model: EZ_Account): Resp[EZ_Account] = {
    if (model.login_id == null || model.login_id.trim.isEmpty
      || model.password == null || model.password.trim.isEmpty
      || model.email == null || model.email.trim.isEmpty) {
      logger.warn(s"Require【Login_id】【password】【email】")
      return Resp.badRequest("Require【Login_id】【password】【email】")
    }
    if (!FormatHelper.validEmail(model.email)) {
      logger.warn("【email】format error")
      return Resp.badRequest("【email】format error")
    }
    if (existByEmail(model.email, model.organization_code).body) {
      logger.warn("【email】exist")
      return Resp.badRequest("【email】exist")
    }
    if (existByLoginId(model.login_id, model.organization_code).body) {
      logger.warn("【login_id】exist")
      return Resp.badRequest("【login_id】exist")
    }
    model.code = EZ.createUUID
    model.password = packageEncryptPwd(model.code, model.password)
    if (model.image == null) {
      model.image = ""
    }
    if (model.organization_code == null) {
      model.organization_code = ServiceAdapter.defaultOrganizationCode
    }
    if (model.oauth == null) {
      model.oauth = ""
    }
    if (model.ext_id == null) {
      model.ext_id = ""
    }
    if (model.ext_info == null) {
      model.ext_info = "{}"
    }
    model.exchange_role_codes = model.role_codes
    model.role_codes = null
    super.preSave(model)
  }

  override def preUpdate(model: EZ_Account): Resp[EZ_Account] = {
    val oldModel = EZ_Account.getById(model.id).body
    if (oldModel == null) {
      return Resp.notFound("")
    }
    if (model.login_id != null && model.login_id != "") {
      val existLoginId = getByLoginId(model.login_id, oldModel.organization_code).body
      if (existLoginId != null && existLoginId.code != oldModel.code) {
        logger.warn("【login_id】exist")
        return Resp.badRequest("【login_id】exist")
      }
    }
    if (model.email != null && model.email != "") {
      if (!FormatHelper.validEmail(model.email)) {
        logger.warn("【email】format error")
        return Resp.badRequest("【email】format error")
      }
      val existEmail = getByEmail(model.email, oldModel.organization_code).body
      if (existEmail != null && existEmail.code != oldModel.code) {
        logger.warn("【email】exist")
        return Resp.badRequest("【email】exist")
      }
    }
    model.code = null
    model.organization_code = null
    model.role_codes = null
    model.exchange_role_codes = model.role_codes
    if (model.exchange_pwd != null && model.exchange_pwd.trim.nonEmpty) {
      model.password = model.exchange_pwd
    } else if (model.password != null && model.password.trim.nonEmpty) {
      // 修改了密码
      model.password = packageEncryptPwd(oldModel.code, model.password)
    }
    val extInfo =
      if (oldModel.ext_info == null || oldModel.ext_info.isEmpty) {
        Map(LAST_CHANGE_PWD -> new Date().getTime)
      } else {
        JsonHelper.toObject[Map[String, Any]](oldModel.ext_info) + (LAST_CHANGE_PWD -> new Date().getTime)
      }
    model.ext_info =
      if (model.ext_info != null && model.ext_info.nonEmpty) {
        JsonHelper.toJsonString(extInfo ++ JsonHelper.toObject[Map[String, Any]](model.ext_info))
      } else {
        JsonHelper.toJsonString(extInfo)
      }
    super.preUpdate(model)
  }

  override def preSaveOrUpdate(model: EZ_Account): Resp[EZ_Account] = {
    if (model.id == null || model.id.trim == "") {
      preSave(model)
    } else {
      preUpdate(model)
    }
  }

  override def postSave(saveResult: EZ_Account, preResult: EZ_Account): Resp[EZ_Account] = {
    postSaveOrUpdate(saveResult, preResult)
  }

  override def postUpdate(updateResult: EZ_Account, preResult: EZ_Account): Resp[EZ_Account] = {
    postSaveOrUpdate(updateResult, preResult)
  }

  override def postSaveOrUpdate(saveOrUpdateResult: EZ_Account, preResult: EZ_Account): Resp[EZ_Account] = {
    if (preResult.id == null || preResult.id.trim == "") {
      saveOrUpdateRelRoleData(saveOrUpdateResult.code, preResult.exchange_role_codes)
      saveOrUpdateResult.role_codes = preResult.exchange_role_codes
      super.postSave(saveOrUpdateResult, preResult)
    } else {
      if (preResult.login_id != null || preResult.password != null || preResult.email != null) {
        // 修改登录Id、密码或邮箱需要重新登录
        CacheManager.Token.removeTokenByAccountCode(saveOrUpdateResult.code)
      } else {
        // 需要重新获取
        CacheManager.Token.updateTokenInfo(getById(saveOrUpdateResult.id).body)
      }
      if (preResult.exchange_role_codes != null && preResult.exchange_role_codes.nonEmpty) {
        saveOrUpdateRelRoleData(saveOrUpdateResult.code, preResult.exchange_role_codes)
        saveOrUpdateResult.role_codes = preResult.exchange_role_codes
      } else {
        saveOrUpdateResult.role_codes = getRelRoleData(saveOrUpdateResult.code).body
      }
      super.postUpdate(saveOrUpdateResult, preResult)
    }
  }

  override def postGetEnabledByCond(condition: String, parameters: List[Any], getResult: EZ_Account): Resp[EZ_Account] = {
    postGetExt(getResult)
    super.postGetEnabledByCond(condition, parameters, getResult)
  }

  override def postGetById(id: Any, getResult: EZ_Account): Resp[EZ_Account] = {
    postGetExt(getResult)
    super.postGetById(id, getResult)
  }

  override def postGetByUUID(uuid: String, getResult: EZ_Account): Resp[EZ_Account] = {
    postGetExt(getResult)
    super.postGetById(uuid, getResult)
  }

  override def postGetByCond(condition: String, parameters: List[Any], getResult: EZ_Account): Resp[EZ_Account] = {
    postGetExt(getResult)
    super.postGetByCond(condition, parameters, getResult)
  }

  override def postFindEnabled(condition: String, parameters: List[Any], findResult: List[EZ_Account]): Resp[List[EZ_Account]] = {
    postSearchExt(findResult)
    super.postFindEnabled(condition, parameters, findResult)
  }

  override def postPageEnabled(condition: String, parameters: List[Any],
                               pageNumber: Long, pageSize: Int, pageResult: Page[EZ_Account]): Resp[Page[EZ_Account]] = {
    postSearchExt(pageResult.objects)
    super.postPageEnabled(condition, parameters, pageNumber, pageSize, pageResult)
  }

  override def postFind(condition: String, parameters: List[Any], findResult: List[EZ_Account]): Resp[List[EZ_Account]] = {
    postSearchExt(findResult)
    super.postFind(condition, parameters, findResult)
  }

  override def postPage(condition: String, parameters: List[Any],
                        pageNumber: Long, pageSize: Int, pageResult: Page[EZ_Account]): Resp[Page[EZ_Account]] = {
    postSearchExt(pageResult.objects)
    super.postPage(condition, parameters, pageNumber, pageSize, pageResult)
  }

  override def preDeleteById(id: Any): Resp[Any] = {
    preRemoveExt(doGetById(id).body, isDelete = true)
    super.preDeleteById(id)
  }

  override def preDeleteByUUID(uuid: String): Resp[String] = {
    preRemoveExt(doGetByUUID(uuid).body, isDelete = true)
    super.preDeleteByUUID(uuid)
  }

  override def preDeleteByCond(condition: String, parameters: List[Any]): Resp[(String, List[Any])] = {
    doFind(condition, parameters).body.foreach(preRemoveExt(_, isDelete = true))
    super.preDeleteByCond(condition, parameters)
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

  override def preUpdateByCond(newValues: String, condition: String, parameters: List[Any]): Resp[(String, String, List[Any])] =
    Resp.notImplemented("")

  def packageEncryptPwd(code: String, password: String): String = {
    EncryptHelper.encrypt(ServiceAdapter.encrypt_salt + code + password, ServiceAdapter.encrypt_algorithm)
  }

  def validateEncryptPwd(code: String, password: String, encryptPassword: String): Boolean = {
    EncryptHelper.validate(
      ServiceAdapter.encrypt_salt + code + password, encryptPassword, ServiceAdapter.encrypt_algorithm)
  }

  def getByCode(code: String): Resp[EZ_Account] = {
    getByUUID(code)
  }

  def getByLoginId(loginId: String, organizationCode: String): Resp[EZ_Account] = {
    getByCond( s"""login_id = ? AND organization_code  = ? """, List(loginId, organizationCode))
  }

  def getByEmail(email: String, organizationCode: String): Resp[EZ_Account] = {
    getByCond( s"""email = ? AND organization_code  = ?""", List(email, organizationCode))
  }

  def getByLoginIdOrEmail(loginIdOrEmail: String, organizationCode: String): Resp[EZ_Account] = {
    getByCond( s"""( login_id = ? OR email = ? ) AND organization_code  = ?""", List(loginIdOrEmail, loginIdOrEmail, organizationCode))
  }

  def getByOAuth(appName: String, authId: String, organizationCode: String): Resp[EZ_Account] = {
    getByCond( s"""oauth.$appName = ? AND organization_code  = ?""", List(authId, organizationCode))
  }

  def existByCode(code: String): Resp[Boolean] = {
    existByUUID(code)
  }

  def existByEmail(email: String, organizationCode: String): Resp[Boolean] = {
    existByCond(s"""email = ? AND organization_code  = ?""", List(email, organizationCode))
  }

  def existByLoginId(loginId: String, organizationCode: String): Resp[Boolean] = {
    existByCond(s"""login_id = ? AND organization_code  = ?""", List(loginId, organizationCode))
  }

  def disableByCode(code: String): Resp[Void] = {
    disableByUUID(code)
  }

  def disableByLoginId(loginId: String, organizationCode: String): Resp[Void] = {
    disableByCond(s"""login_id = ? AND organization_code  = ?""", List(loginId, organizationCode))
  }

  def disableByEmail(email: String, organizationCode: String): Resp[Void] = {
    disableByCond(s"""email = ? AND organization_code  = ?""", List(email, organizationCode))
  }

  def disableByLoginIdOrEmail(loginIdOrEmail: String, organizationCode: String): Resp[Void] = {
    disableByCond(s"""( login_id = ? OR email = ? ) AND organization_code  = ?""", List(loginIdOrEmail, loginIdOrEmail, organizationCode))
  }

  def enableByCode(code: String): Resp[Void] = {
    enableByUUID(code)
  }

  def enableByLoginId(loginId: String, organizationCode: String): Resp[Void] = {
    enableByCond(s"""login_id = ? AND organization_code  = ?""", List(loginId, organizationCode))
  }

  def enableByEmail(email: String, organizationCode: String): Resp[Void] = {
    enableByCond( s"""email = ? AND organization_code  = ?""", List(email, organizationCode))
  }

  def enableByLoginIdOrEmail(loginIdOrEmail: String, organizationCode: String): Resp[Void] = {
    enableByCond( s"""( login_id = ? OR email = ? ) AND organization_code  = ?""", List(loginIdOrEmail, loginIdOrEmail, organizationCode))
  }

  def deleteByCode(code: String): Resp[Void] = {
    deleteByUUID(code)
  }

  def deleteByLoginId(loginId: String, organizationCode: String): Resp[Void] = {
    deleteByCond( s"""login_id = ? AND organization_code  = ?""", List(loginId, organizationCode))
  }

  def deleteByEmail(email: String, organizationCode: String): Resp[Void] = {
    deleteByCond( s"""email = ? AND organization_code  = ?""", List(email, organizationCode))
  }

  def deleteByLoginIdOrEmail(loginIdOrEmail: String, organizationCode: String): Resp[Void] = {
    deleteByCond( s"""( login_id = ? OR email = ? ) AND organization_code  = ?""", List(loginIdOrEmail, loginIdOrEmail, organizationCode))
  }

  def saveOrUpdateRelRoleData(accountCode: String, roleCodes: Set[String]): Resp[Void] = {
    deleteRelRoleData(accountCode)
    JDBCProcessor.batch(
      s"""INSERT INTO ${EZ_Account.TABLE_REL_ACCOUNT_ROLE} ( account_code , role_code ) VALUES ( ? , ? )""",
      roleCodes.map {
        roleCode => List(accountCode, roleCode)
      }.toList)
  }

  def deleteRelRoleData(accountCode: String): Resp[Void] = {
    JDBCProcessor.update(s"""DELETE FROM ${EZ_Account.TABLE_REL_ACCOUNT_ROLE} WHERE account_code  = ? """, List(accountCode))
  }

  def getRelRoleData(accountCode: String): Resp[Set[String]] = {
    val resp = JDBCProcessor.find(
      s"""SELECT role_code FROM ${EZ_Account.TABLE_REL_ACCOUNT_ROLE} WHERE account_code  = ? """,
      List(accountCode))
    if (resp) {
      Resp.success(resp.body.map(_ ("role_code").asInstanceOf[String]).toSet)
    } else {
      resp
    }
  }

  private def postSearchExt(findResult: List[EZ_Account]): Unit = {
    findResult.foreach {
      result =>
        postGetExt(result)
    }
  }

  private def postGetExt(getResult: EZ_Account): Unit = {
    if (getResult != null) {
      getResult.role_codes = getRelRoleData(getResult.code).body
    }
  }

  private def preRemoveExt(obj: EZ_Account, isDelete: Boolean): Unit = {
    if (obj != null) {
      CacheManager.Token.removeTokenByAccountCode(obj.code)
      if (isDelete) {
        deleteRelRoleData(obj.code)
      }
    }
  }

}



