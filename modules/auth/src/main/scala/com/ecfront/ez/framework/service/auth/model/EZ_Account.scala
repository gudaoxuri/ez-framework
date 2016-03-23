package com.ecfront.ez.framework.service.auth.model

import com.ecfront.common.{EncryptHelper, FormatHelper, Resp}
import com.ecfront.ez.framework.service.auth.ServiceAdapter
import com.ecfront.ez.framework.service.storage.foundation._
import com.ecfront.ez.framework.service.storage.jdbc.{JDBCSecureStorage, JDBCStatusStorage}
import com.ecfront.ez.framework.service.storage.mongo.{MongoSecureStorage, MongoStatusStorage}

import scala.beans.BeanProperty

/**
  * 账号实体
  */
@Entity("Account")
case class EZ_Account() extends SecureModel with StatusModel {

  @Unique
  @Require
  @Label("Login Id")
  @BeanProperty var login_id: String = _
  @Require
  @Label("Name")
  @BeanProperty var name: String = _
  @BeanProperty var image: String = _
  @Require
  @Label("Password")
  @BeanProperty var password: String = _
  @Unique
  @Require
  @Label("Email")
  @BeanProperty var email: String = _
  @Label("Ext Id")
  @BeanProperty var ext_id: String = _
  @Label("Ext Info")
  @BeanProperty var ext_info: Map[String, String] = _
  @Label("OAuth Info")
  @BeanProperty var oauth: Map[String, String] = _
  @BeanProperty var organization_code: String = _
  @BeanProperty var role_codes: List[String] = List[String]()

}

object EZ_Account extends SecureStorageAdapter[EZ_Account, EZ_Account_Base]
  with StatusStorageAdapter[EZ_Account, EZ_Account_Base] with EZ_Account_Base {

  val SYSTEM_ACCOUNT_CODE = "sysadmin"

  val VIRTUAL_EMAIL = "@virtual.is"

  override protected val storageObj: EZ_Account_Base =
    if (ServiceAdapter.mongoStorage) EZ_Account_Mongo else EZ_Account_JDBC

  def apply(loginId: String, email: String, name: String, password: String, roleCodes: List[String]): EZ_Account = {
    val account = EZ_Account()
    account.login_id = loginId
    account.email = email
    account.name = name
    account.password = password
    account.organization_code = ""
    account.enable = true
    account.role_codes = roleCodes
    account.ext_id = ""
    account.ext_info = Map()
    account
  }

  override def getByLoginId(loginId: String): Resp[EZ_Account] = storageObj.getByLoginId(loginId)

  override def getByEmail(email: String): Resp[EZ_Account] = storageObj.getByEmail(email)

  override def getByLoginIdOrEmail(loginIdOrEmail: String): Resp[EZ_Account] = storageObj.getByLoginIdOrEmail(loginIdOrEmail)

  override def getByOAuth(appName: String, authId: String): Resp[EZ_Account] = storageObj.getByOAuth(appName, authId)

  override def existByEmail(email: String): Resp[Boolean] = storageObj.existByEmail(email)

  override def deleteByLoginId(loginId: String): Resp[Void] = storageObj.deleteByLoginId(loginId)

  override def deleteByEmail(email: String): Resp[Void] = storageObj.deleteByEmail(email)

  override def deleteByLoginIdOrEmail(loginIdOrEmail: String): Resp[Void] = storageObj.deleteByLoginIdOrEmail(loginIdOrEmail)

}

trait EZ_Account_Base extends SecureStorage[EZ_Account] with StatusStorage[EZ_Account] {

  override def preSave(model: EZ_Account, context: EZStorageContext): Resp[EZ_Account] = {
    if (model.login_id == null || model.login_id.trim.isEmpty
      || model.password == null || model.password.trim.isEmpty
      || model.email == null || model.email.trim.isEmpty) {
      Resp.badRequest("Require【Login_id】【password】【email】")
    } else {
      // 当账号不是oauth类型且登录ld包含@时，拒绝保存
      if ((model.oauth == null || model.oauth.isEmpty) && model.login_id.contains("@")) {
        Resp.badRequest("【login id】can't use email address")
      } else if (FormatHelper.validEmail(model.email)) {
        model.password = packageEncryptPwd(model.login_id, model.password)
        if (model.ext_id == null) {
          model.ext_id = ""
        }
        if (model.ext_info == null) {
          model.ext_info = Map()
        }
        super.preSave(model, context)
      } else {
        Resp.badRequest("【email】format error")
      }
    }
  }

  override def preUpdate(model: EZ_Account, context: EZStorageContext): Resp[EZ_Account] = {
    if (model.login_id == null || model.login_id.trim.isEmpty
      || model.password == null || model.password.trim.isEmpty
      || model.email == null || model.email.trim.isEmpty) {
      Resp.badRequest("Require【Login_id】【password】【email】")
    } else {
      // 当账号不是oauth类型且登录ld包含@时，拒绝保存
      if ((model.oauth == null || model.oauth.isEmpty) && model.login_id.contains("@")) {
        Resp.badRequest("【login id】can't use email address")
      } else {
        if (FormatHelper.validEmail(model.email)) {
          if (model.ext_id == null) {
            model.ext_id = ""
          }
          if (model.ext_info == null) {
            model.ext_info = Map()
          }
          super.preUpdate(model, context)
        } else {
          Resp.badRequest("【email】format error")
        }
      }
    }
  }

  override def preSaveOrUpdate(model: EZ_Account, context: EZStorageContext): Resp[EZ_Account] = {
    if (model.login_id == null || model.login_id.trim.isEmpty
      || model.password == null || model.password.trim.isEmpty
      || model.email == null || model.email.trim.isEmpty) {
      Resp.badRequest("Require【Login_id】【password】【email】")
    } else {
      // 当账号不是oauth类型且登录ld包含@时，拒绝保存
      if ((model.oauth == null || model.oauth.isEmpty) && model.login_id.contains("@")) {
        Resp.badRequest("【login id】can't use email address")
      } else {
        if (FormatHelper.validEmail(model.email)) {
          super.preSaveOrUpdate(model, context)
        } else {
          Resp.badRequest("【email】format error")
        }
      }
    }
  }

  def packageEncryptPwd(loginId: String, password: String): String = {
    EncryptHelper.encrypt(loginId + password)
  }

  def getByLoginId(loginId: String): Resp[EZ_Account]

  def getByEmail(email: String): Resp[EZ_Account]

  def getByLoginIdOrEmail(loginIdOrEmail: String): Resp[EZ_Account]

  def getByOAuth(appName: String, authId: String): Resp[EZ_Account]

  def existByEmail(email: String): Resp[Boolean]

  def deleteByLoginId(loginId: String): Resp[Void]

  def deleteByEmail(email: String): Resp[Void]

  def deleteByLoginIdOrEmail(loginIdOrEmail: String): Resp[Void]

}

object EZ_Account_Mongo extends MongoSecureStorage[EZ_Account] with MongoStatusStorage[EZ_Account] with EZ_Account_Base {

  override def getByLoginId(loginId: String): Resp[EZ_Account] = {
    getByCond( s"""{"login_id":"$loginId"}""")
  }

  override def getByEmail(email: String): Resp[EZ_Account] = {
    getByCond( s"""{"email":"$email"}""")
  }

  override def getByLoginIdOrEmail(loginIdOrEmail: String): Resp[EZ_Account] = {
    getByCond( s"""{"$$or":[{"login_id":"$loginIdOrEmail"},{"email":"$loginIdOrEmail"}]}""")
  }

  override def getByOAuth(appName: String, authId: String): Resp[EZ_Account] = {
    getByCond( s"""{"oauth.$appName":"$authId"}""")
  }

  override def existByEmail(email: String): Resp[Boolean] = {
    existByCond(s"""{"email":"$email"}""")
  }

  override def deleteByLoginId(loginId: String): Resp[Void] = {
    deleteByCond( s"""{"login_id":"$loginId"}""")
  }

  override def deleteByEmail(email: String): Resp[Void] = {
    deleteByCond( s"""{"email":"$email"}""")
  }

  override def deleteByLoginIdOrEmail(loginIdOrEmail: String): Resp[Void] = {
    deleteByCond( s"""{"$$or":[{"login_id":"$loginIdOrEmail"},{"email":"$loginIdOrEmail"}]}""")
  }

}

object EZ_Account_JDBC extends JDBCSecureStorage[EZ_Account] with JDBCStatusStorage[EZ_Account] with EZ_Account_Base {

  override def getByLoginId(loginId: String): Resp[EZ_Account] = {
    getByCond( s"""login_id = ?""", List(loginId))
  }

  override def getByEmail(email: String): Resp[EZ_Account] = {
    getByCond( s"""email = ?""", List(email))
  }

  override def getByLoginIdOrEmail(loginIdOrEmail: String): Resp[EZ_Account] = {
    getByCond( s"""login_id = ? OR email = ?""", List(loginIdOrEmail, loginIdOrEmail))
  }

  override def getByOAuth(appName: String, authId: String): Resp[EZ_Account] = {
    getByCond( s"""oauth.$appName = ?""", List(authId))
  }

  override def existByEmail(email: String): Resp[Boolean] = {
    existByCond(s"""email = ?""", List(email))
  }

  override def deleteByLoginId(loginId: String): Resp[Void] = {
    deleteByCond( s"""login_id = ?""", List(loginId))
  }

  override def deleteByEmail(email: String): Resp[Void] = {
    deleteByCond( s"""email = ?""", List(email))
  }

  override def deleteByLoginIdOrEmail(loginIdOrEmail: String): Resp[Void] = {
    deleteByCond( s"""login_id = ? OR email = ?""", List(loginIdOrEmail, loginIdOrEmail))
  }

}



