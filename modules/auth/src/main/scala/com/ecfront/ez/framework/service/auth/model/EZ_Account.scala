package com.ecfront.ez.framework.service.auth.model

import com.ecfront.common._
import com.ecfront.ez.framework.core.EZContext
import com.ecfront.ez.framework.core.i18n.I18NProcessor.Impl
import com.ecfront.ez.framework.service.auth.{CacheManager, OrganizationModel, OrganizationStorage, ServiceAdapter}
import com.ecfront.ez.framework.service.storage.foundation.{BaseStorage, _}
import com.ecfront.ez.framework.service.storage.jdbc.{JDBCProcessor, JDBCSecureStorage, JDBCStatusStorage}
import com.ecfront.ez.framework.service.storage.mongo.{MongoProcessor, MongoSecureStorage, MongoStatusStorage}
import io.vertx.core.json.JsonObject

import scala.beans.BeanProperty

/**
  * 账号实体
  */
@Entity("Account")
case class EZ_Account() extends SecureModel with StatusModel with OrganizationModel {

  @Unique
  @Require
  @Label("Code")
  @BeanProperty var code: String = _
  @Require
  @Label("Login Id")
  @BeanProperty var login_id: String = _
  @Require
  @Label("Name")
  @BeanProperty var name: String = _
  @Label("Image")
  @BeanProperty var image: String = _
  @Require
  @Label("Password")
  @BeanProperty var password: String = _
  // 此字段不为空时保存或更新账户时不对密码做加密
  @Ignore var exchange_pwd: String = _
  @Require
  @Label("Email")
  @BeanProperty var email: String = _
  @Label("Ext Id") // 用于关联其它对象以扩展属性，扩展Id多为业务系统用户信息表的主键
  @BeanProperty var ext_id: String = _
  @Label("Ext Info")
  @Ignore var exchange_ext_info: Map[String, Any] = _
  @BeanProperty var ext_info: Map[String, Any] = _
  @Label("OAuth Info") // key=oauth服务标记，value=openid
  @BeanProperty var oauth: Map[String, String] = _
  @Ignore var exchange_role_codes: List[String] = _
  @BeanProperty var role_codes: List[String] = _

}

object EZ_Account extends SecureStorageAdapter[EZ_Account, EZ_Account_Base]
  with StatusStorageAdapter[EZ_Account, EZ_Account_Base] with OrganizationStorage[EZ_Account] with EZ_Account_Base {

  // 角色关联表，在useRelTable=true中启用
  var TABLE_REL_ACCOUNT_ROLE = "ez_rel_account_role"

  val SYSTEM_ACCOUNT_LOGIN_ID = "sysadmin"

  val ORG_ADMIN_ACCOUNT_LOGIN_ID = "admin"

  val VIRTUAL_EMAIL = "@virtual.is"

  var extAccountStorage: BaseStorage[BaseModel] = _

  def init(_extAccountStorage: String): Unit = {
    if (_extAccountStorage == null || _extAccountStorage.trim.isEmpty) {
      extAccountStorage = null
    } else {
      extAccountStorage = _runtimeMirror.reflectModule(_runtimeMirror.staticModule(_extAccountStorage)).instance.asInstanceOf[BaseStorage[BaseModel]]
    }
  }

  override protected val storageObj: EZ_Account_Base =
    if (ServiceAdapter.mongoStorage) EZ_Account_Mongo else EZ_Account_JDBC

  def apply(loginId: String, email: String, name: String, password: String,
            roleCodes: List[String], organizationCode: String = ServiceAdapter.defaultOrganizationCode): EZ_Account = {
    val account = EZ_Account()
    account.login_id = loginId
    account.email = email
    account.name = name.x
    account.password = password
    account.organization_code = organizationCode
    account.enable = true
    account.role_codes = roleCodes
    account.ext_id = ""
    account.ext_info = Map()
    account
  }

  override def findByOrganizationCode(organizationCode: String): Resp[List[EZ_Account]] = storageObj.findByOrganizationCode(organizationCode)

  override def findEnableByOrganizationCode(organizationCode: String): Resp[List[EZ_Account]] =
    storageObj.findEnableByOrganizationCode(organizationCode)

  override def getByCode(code: String): Resp[EZ_Account] = storageObj.getByCode(code)

  override def getByLoginId(loginId: String, organizationCode: String): Resp[EZ_Account] = storageObj.getByLoginId(loginId, organizationCode)

  override def getByEmail(email: String, organizationCode: String): Resp[EZ_Account] = storageObj.getByEmail(email, organizationCode)

  override def getByLoginIdOrEmail(loginIdOrEmail: String, organizationCode: String): Resp[EZ_Account] =
    storageObj.getByLoginIdOrEmail(loginIdOrEmail, organizationCode)

  override def getByOAuth(appName: String, authId: String, organizationCode: String): Resp[EZ_Account] =
    storageObj.getByOAuth(appName, authId, organizationCode)

  override def existByEmail(email: String, organizationCode: String): Resp[Boolean] = storageObj.existByEmail(email, organizationCode)

  override def existByLoginId(loginId: String, organizationCode: String): Resp[Boolean] = storageObj.existByLoginId(loginId, organizationCode)

  override def deleteByCode(code: String): Resp[Void] = storageObj.deleteByCode(code)

  override def deleteByLoginId(loginId: String, organizationCode: String): Resp[Void] = storageObj.deleteByLoginId(loginId, organizationCode)

  override def deleteByEmail(email: String, organizationCode: String): Resp[Void] = storageObj.deleteByEmail(email, organizationCode)

  override def deleteByLoginIdOrEmail(loginIdOrEmail: String, organizationCode: String): Resp[Void] =
    storageObj.deleteByLoginIdOrEmail(loginIdOrEmail, organizationCode)

  override def saveOrUpdateRelRoleData(accountCode: String, roleCodes: List[String]): Resp[Void] = storageObj.saveOrUpdateRelRoleData(accountCode, roleCodes)

  override def deleteRelRoleData(accountCode: String): Resp[Void] = storageObj.deleteRelRoleData(accountCode)

  override def getRelRoleData(accountCode: String): Resp[List[String]] = storageObj.getRelRoleData(accountCode)

}

trait EZ_Account_Base extends SecureStorage[EZ_Account] with StatusStorage[EZ_Account] with OrganizationStorage[EZ_Account] {

  override def preSave(model: EZ_Account, context: EZStorageContext): Resp[EZ_Account] = {
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
    model.code = EZContext.createUUID()
    model.password = packageEncryptPwd(model.code, model.password)
    if (model.image == null) {
      model.image = ""
    }
    if (model.organization_code == null) {
      model.organization_code = ServiceAdapter.defaultOrganizationCode
    }
    if (model.oauth == null) {
      model.oauth = Map()
    }
    if (model.ext_id == null) {
      model.ext_id = ""
    }
    if (model.ext_info == null) {
      model.ext_info = Map()
    }
    if (ServiceAdapter.useRelTable) {
      model.exchange_role_codes = model.role_codes
      model.role_codes = null
    }
    if (EZ_Account.extAccountStorage != null) {
      model.exchange_ext_info = model.ext_info
      model.ext_info = Map()
    }
    super.preSave(model, context)
  }

  override def preUpdate(model: EZ_Account, context: EZStorageContext): Resp[EZ_Account] = {
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
    if (EZ_Account.extAccountStorage != null) {
      model.exchange_ext_info = model.ext_info
      model.ext_info = Map()
    }
    if (ServiceAdapter.useRelTable) {
      model.exchange_role_codes = model.role_codes
      model.role_codes = null
    }
    if (model.exchange_pwd != null && model.exchange_pwd.trim.nonEmpty) {
      model.password = model.exchange_pwd
    } else if (model.password != null && model.password.trim.nonEmpty) {
      model.password = packageEncryptPwd(oldModel.code, model.password)
    }
    super.preUpdate(model, context)
  }

  override def preSaveOrUpdate(model: EZ_Account, context: EZStorageContext): Resp[EZ_Account] = {
    if (model.id == null || model.id.trim == "") {
      preSave(model, context)
    } else {
      preUpdate(model, context)
    }
  }

  override def postSave(saveResult: EZ_Account, preResult: EZ_Account, context: EZStorageContext): Resp[EZ_Account] = {
    postSaveOrUpdate(saveResult, preResult, context)
  }

  override def postUpdate(updateResult: EZ_Account, preResult: EZ_Account, context: EZStorageContext): Resp[EZ_Account] = {
    postSaveOrUpdate(updateResult, preResult, context)
  }

  override def postSaveOrUpdate(saveOrUpdateResult: EZ_Account, preResult: EZ_Account, context: EZStorageContext): Resp[EZ_Account] = {
    if (preResult.id == null || preResult.id.trim == "") {
      if (EZ_Account.extAccountStorage != null) {
        val extObj = EZ_Account.extAccountStorage.save(EZ_Account.extAccountStorage.convertToEntity(preResult.exchange_ext_info), context).body
        saveOrUpdateResult.ext_id = extObj.id
        saveOrUpdateResult.role_codes = null
        doUpdate(saveOrUpdateResult, context)
        saveOrUpdateResult.ext_info = preResult.exchange_ext_info
      }
      if (ServiceAdapter.useRelTable) {
        saveOrUpdateRelRoleData(saveOrUpdateResult.code, preResult.exchange_role_codes)
        saveOrUpdateResult.role_codes = preResult.exchange_role_codes
      }
      super.postSave(saveOrUpdateResult, preResult, context)
    } else {
      if (preResult.login_id != null || preResult.password != null || preResult.email != null) {
        // 修改登录Id、密码或邮箱需要重新登录
        CacheManager.removeToken(saveOrUpdateResult.code)
      } else {
        // 需要重新获取
        CacheManager.updateTokenInfo(getById(saveOrUpdateResult.id).body)
      }
      if (EZ_Account.extAccountStorage != null) {
        if (preResult.exchange_ext_info != null && preResult.exchange_ext_info.nonEmpty) {
          EZ_Account.extAccountStorage.update(
            EZ_Account.extAccountStorage.convertToEntity(preResult.exchange_ext_info + (BaseModel.Id_FLAG -> saveOrUpdateResult.ext_id)), context).body
          saveOrUpdateResult.ext_info = preResult.exchange_ext_info
        } else {
          saveOrUpdateResult.ext_info = JsonHelper.toObject[Map[String, Any]](EZ_Account.extAccountStorage.getById(saveOrUpdateResult.ext_id.trim).body)
        }
      }
      if (ServiceAdapter.useRelTable) {
        if (preResult.exchange_role_codes != null && preResult.exchange_role_codes.nonEmpty) {
          saveOrUpdateRelRoleData(saveOrUpdateResult.code, preResult.exchange_role_codes)
          saveOrUpdateResult.role_codes = preResult.exchange_role_codes
        } else {
          saveOrUpdateResult.role_codes = getRelRoleData(saveOrUpdateResult.code).body
        }
      }
      super.postUpdate(saveOrUpdateResult, preResult, context)
    }
  }

  override def postGetEnabledByCond(condition: String, parameters: List[Any], getResult: EZ_Account, context: EZStorageContext): Resp[EZ_Account] = {
    postGetX(getResult)
    super.postGetEnabledByCond(condition, parameters, getResult, context)
  }

  override def postGetById(id: Any, getResult: EZ_Account, context: EZStorageContext): Resp[EZ_Account] = {
    postGetX(getResult)
    super.postGetById(id, getResult, context)
  }

  override def postGetByCond(condition: String, parameters: List[Any], getResult: EZ_Account, context: EZStorageContext): Resp[EZ_Account] = {
    postGetX(getResult)
    super.postGetByCond(condition, parameters, getResult, context)
  }

  override def postFindEnabled(condition: String, parameters: List[Any], findResult: List[EZ_Account], context: EZStorageContext): Resp[List[EZ_Account]] = {
    postSearch(findResult)
    super.postFindEnabled(condition, parameters, findResult, context)
  }

  override def postPageEnabled(condition: String, parameters: List[Any],
                               pageNumber: Long, pageSize: Int, pageResult: Page[EZ_Account], context: EZStorageContext): Resp[Page[EZ_Account]] = {
    postSearch(pageResult.objects)
    super.postPageEnabled(condition, parameters, pageNumber, pageSize, pageResult, context)
  }

  override def postFind(condition: String, parameters: List[Any], findResult: List[EZ_Account], context: EZStorageContext): Resp[List[EZ_Account]] = {
    postSearch(findResult)
    super.postFind(condition, parameters, findResult, context)
  }

  override def postPage(condition: String, parameters: List[Any],
                        pageNumber: Long, pageSize: Int, pageResult: Page[EZ_Account], context: EZStorageContext): Resp[Page[EZ_Account]] = {
    postSearch(pageResult.objects)
    super.postPage(condition, parameters, pageNumber, pageSize, pageResult, context)
  }

  protected def postSearch(findResult: List[EZ_Account]): Unit = {
    findResult.foreach {
      result =>
        postGetX(result)
    }
  }

  protected def postGetX(getResult: EZ_Account): Unit = {
    if (getResult != null) {
      if (EZ_Account.extAccountStorage != null) {
        getResult.ext_info = JsonHelper.toObject[Map[String, Any]](EZ_Account.extAccountStorage.getById(getResult.ext_id.trim).body)
      }
      if (ServiceAdapter.useRelTable) {
        getResult.role_codes = getRelRoleData(getResult.code).body
      }
    }
  }

  override def preDeleteById(id: Any, context: EZStorageContext): Resp[Any] = {
    if (EZ_Account.extAccountStorage != null || ServiceAdapter.useRelTable) {
      val objR = doGetById(id, context)
      if (objR && objR.body != null) {
        preDeleteX(objR.body)
      }
    }
    super.preDeleteById(id, context)
  }

  override def preDeleteByCond(condition: String, parameters: List[Any],
                               context: EZStorageContext): Resp[(String, List[Any])] = {
    if (EZ_Account.extAccountStorage != null || ServiceAdapter.useRelTable) {
      val objR = doFind(condition, parameters, context)
      if (objR && objR.body != null && objR.body.nonEmpty) {
        objR.body.foreach {
          obj =>
            preDeleteX(obj)
        }
      }
    }
    super.preDeleteByCond(condition, parameters, context)
  }

  protected def preDeleteX(obj: EZ_Account): Any = {
    CacheManager.removeToken(obj.code)
    if (EZ_Account.extAccountStorage != null && obj.ext_id != null && obj.ext_id.trim.nonEmpty) {
      EZ_Account.extAccountStorage.deleteById(obj.ext_id.trim)
    }
    if (ServiceAdapter.useRelTable) {
      deleteRelRoleData(obj.code)
    }
  }

  override def preUpdateByCond(newValues: String, condition: String, parameters: List[Any], context: EZStorageContext): Resp[(String, String, List[Any])] =
    Resp.notImplemented("")

  override def postDisableById(id: Any, context: EZStorageContext): Resp[Void] = {
    val accountR = EZ_Account.doGetById(id, context)
    if (accountR && accountR.body != null) {
      CacheManager.removeToken(accountR.body.code)
    }
    super.postDisableById(id, context)
  }

  def packageEncryptPwd(code: String, password: String): String = {
    EncryptHelper.encrypt(ServiceAdapter.encrypt_salt + code + password, ServiceAdapter.encrypt_algorithm)
  }

  def validateEncryptPwd(code: String, password: String, encryptPassword: String): Boolean = {
    EncryptHelper.validate(
      ServiceAdapter.encrypt_salt + code + password, encryptPassword, ServiceAdapter.encrypt_algorithm)
  }

  def findByOrganizationCode(organizationCode: String): Resp[List[EZ_Account]]

  def findEnableByOrganizationCode(organizationCode: String): Resp[List[EZ_Account]]

  def getByCode(code: String): Resp[EZ_Account]

  def getByLoginId(loginId: String, organizationCode: String): Resp[EZ_Account]

  def getByEmail(email: String, organizationCode: String): Resp[EZ_Account]

  def getByLoginIdOrEmail(loginIdOrEmail: String, organizationCode: String): Resp[EZ_Account]

  def getByOAuth(appName: String, authId: String, organizationCode: String): Resp[EZ_Account]

  def existByEmail(email: String, organizationCode: String): Resp[Boolean]

  def existByLoginId(loginId: String, organizationCode: String): Resp[Boolean]

  def deleteByCode(code: String): Resp[Void]

  def deleteByLoginId(loginId: String, organizationCode: String): Resp[Void]

  def deleteByEmail(email: String, organizationCode: String): Resp[Void]

  def deleteByLoginIdOrEmail(loginIdOrEmail: String, organizationCode: String): Resp[Void]

  def saveOrUpdateRelRoleData(accountCode: String, roleCodes: List[String]): Resp[Void]

  def deleteRelRoleData(accountCode: String): Resp[Void]

  def getRelRoleData(accountCode: String): Resp[List[String]]

}

object EZ_Account_Mongo extends MongoSecureStorage[EZ_Account]
  with MongoStatusStorage[EZ_Account] with OrganizationStorage[EZ_Account] with EZ_Account_Base {

  override def findByOrganizationCode(organizationCode: String): Resp[List[EZ_Account]] = {
    find( s"""{"organization_code":"$organizationCode"}""")
  }

  override def findEnableByOrganizationCode(organizationCode: String): Resp[List[EZ_Account]] = {
    findEnabled( s"""{"organization_code":"$organizationCode"}""")
  }

  override def getByCode(code: String): Resp[EZ_Account] = {
    getByCond( s"""{"code":"$code"}""")
  }

  override def getByLoginId(loginId: String, organizationCode: String): Resp[EZ_Account] = {
    getByCond( s"""{"login_id":"$loginId","organization_code":"$organizationCode"}""")
  }

  override def getByEmail(email: String, organizationCode: String): Resp[EZ_Account] = {
    getByCond( s"""{"email":"$email","organization_code":"$organizationCode"}""")
  }

  override def getByLoginIdOrEmail(loginIdOrEmail: String, organizationCode: String): Resp[EZ_Account] = {
    getByCond( s"""{"$$or":[{"login_id":"$loginIdOrEmail"},{"email":"$loginIdOrEmail"}],"organization_code":"$organizationCode"}""")
  }

  override def getByOAuth(appName: String, authId: String, organizationCode: String): Resp[EZ_Account] = {
    getByCond( s"""{"oauth.$appName":"$authId","organization_code":"$organizationCode"}""")
  }

  override def existByEmail(email: String, organizationCode: String): Resp[Boolean] = {
    existByCond(s"""{"email":"$email","organization_code":"$organizationCode"}""")
  }

  override def existByLoginId(loginId: String, organizationCode: String): Resp[Boolean] = {
    existByCond(s"""{"login_id":"$loginId","organization_code":"$organizationCode"}""")
  }

  override def deleteByLoginId(loginId: String, organizationCode: String): Resp[Void] = {
    deleteByCond( s"""{"login_id":"$loginId","organization_code":"$organizationCode"}""")
  }

  override def deleteByCode(code: String): Resp[Void] = {
    deleteByCond( s"""{"code":"$code"}""")
  }

  override def deleteByEmail(email: String, organizationCode: String): Resp[Void] = {
    deleteByCond( s"""{"email":"$email","organization_code":"$organizationCode"}""")
  }

  override def deleteByLoginIdOrEmail(loginIdOrEmail: String, organizationCode: String): Resp[Void] = {
    deleteByCond( s"""{"$$or":[{"login_id":"$loginIdOrEmail"},{"email":"$loginIdOrEmail"}],"organization_code":"$organizationCode"}""")
  }

  override def saveOrUpdateRelRoleData(accountCode: String, roleCodes: List[String]): Resp[Void] = {
    deleteRelRoleData(accountCode)
    roleCodes.foreach {
      roleCode =>
        MongoProcessor.save(EZ_Account.TABLE_REL_ACCOUNT_ROLE,
          new JsonObject(s"""{"account_code":"$accountCode","role_code":"$roleCode"}""")
        )
    }
    Resp.success(null)
  }

  override def deleteRelRoleData(accountCode: String): Resp[Void] = {
    MongoProcessor.deleteByCond(EZ_Account.TABLE_REL_ACCOUNT_ROLE,
      new JsonObject(s"""{"account_code":"$accountCode"}"""))
  }

  override def getRelRoleData(accountCode: String): Resp[List[String]] = {
    val resp = MongoProcessor.find(EZ_Account.TABLE_REL_ACCOUNT_ROLE,
      new JsonObject(s"""{"account_code":"$accountCode"}"""), null, 0, classOf[JsonObject])
    if (resp) {
      Resp.success(resp.body.map(_.getString("role_code")))
    } else {
      resp
    }
  }

}

object EZ_Account_JDBC extends JDBCSecureStorage[EZ_Account]
  with JDBCStatusStorage[EZ_Account] with OrganizationStorage[EZ_Account] with EZ_Account_Base {

  override def findByOrganizationCode(organizationCode: String): Resp[List[EZ_Account]] = {
    find(s"""organization_code = ?""", List(organizationCode))
  }

  override def findEnableByOrganizationCode(organizationCode: String): Resp[List[EZ_Account]] = {
    findEnabled(s"""organization_code = ?""", List(organizationCode))
  }

  override def getByCode(code: String): Resp[EZ_Account] = {
    getByCond( s"""code = ?""", List(code))
  }

  override def getByLoginId(loginId: String, organizationCode: String): Resp[EZ_Account] = {
    getByCond( s"""login_id = ? AND organization_code  = ? """, List(loginId, organizationCode))
  }

  override def getByEmail(email: String, organizationCode: String): Resp[EZ_Account] = {
    getByCond( s"""email = ? AND organization_code  = ?""", List(email, organizationCode))
  }

  override def getByLoginIdOrEmail(loginIdOrEmail: String, organizationCode: String): Resp[EZ_Account] = {
    getByCond( s"""( login_id = ? OR email = ? ) AND organization_code  = ?""", List(loginIdOrEmail, loginIdOrEmail, organizationCode))
  }

  override def getByOAuth(appName: String, authId: String, organizationCode: String): Resp[EZ_Account] = {
    getByCond( s"""oauth.$appName = ? AND organization_code  = ?""", List(authId, organizationCode))
  }

  override def existByEmail(email: String, organizationCode: String): Resp[Boolean] = {
    existByCond(s"""email = ? AND organization_code  = ?""", List(email, organizationCode))
  }

  override def existByLoginId(loginId: String, organizationCode: String): Resp[Boolean] = {
    existByCond(s"""login_id = ? AND organization_code  = ?""", List(loginId, organizationCode))
  }

  override def deleteByCode(code: String): Resp[Void] = {
    deleteByCond( s"""code = ?""", List(code))
  }

  override def deleteByLoginId(loginId: String, organizationCode: String): Resp[Void] = {
    deleteByCond( s"""login_id = ? AND organization_code  = ?""", List(loginId, organizationCode))
  }

  override def deleteByEmail(email: String, organizationCode: String): Resp[Void] = {
    deleteByCond( s"""email = ? AND organization_code  = ?""", List(email, organizationCode))
  }

  override def deleteByLoginIdOrEmail(loginIdOrEmail: String, organizationCode: String): Resp[Void] = {
    deleteByCond( s"""( login_id = ? OR email = ? ) AND organization_code  = ?""", List(loginIdOrEmail, loginIdOrEmail, organizationCode))
  }

  override def saveOrUpdateRelRoleData(accountCode: String, roleCodes: List[String]): Resp[Void] = {
    deleteRelRoleData(accountCode)
    JDBCProcessor.batch(
      s"""INSERT INTO ${EZ_Account.TABLE_REL_ACCOUNT_ROLE} ( account_code , role_code ) VALUES ( ? , ? )""",
      roleCodes.map {
        roleCode => List(accountCode, roleCode)
      })
  }

  override def deleteRelRoleData(accountCode: String): Resp[Void] = {
    JDBCProcessor.update(s"""DELETE FROM ${EZ_Account.TABLE_REL_ACCOUNT_ROLE} WHERE account_code  = ? """, List(accountCode))
  }

  override def getRelRoleData(accountCode: String): Resp[List[String]] = {
    val resp = JDBCProcessor.find(
      s"""SELECT role_code FROM ${EZ_Account.TABLE_REL_ACCOUNT_ROLE} WHERE account_code  = ? """,
      List(accountCode), classOf[JsonObject])
    if (resp) {
      Resp.success(resp.body.map(_.getString("role_code")))
    } else {
      resp
    }
  }

}



