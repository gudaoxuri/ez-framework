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
  @Label("Code") // organization_code@login_id
  @BeanProperty var code: String = _
  @Require
  @Label("Login Id") // 不能包含@
  @BeanProperty var login_id: String = _
  @Require
  @Label("Name")
  @BeanProperty var name: String = _
  @Label("Image")
  @BeanProperty var image: String = _
  @Require
  @Label("Password")
  @BeanProperty var password: String = _
  @Require
  @Label("Email")
  @BeanProperty var email: String = _
  @Label("Ext Id") // 用于关联其它对象以扩展属性，扩展Id多为业务系统用户信息表的主键
  @BeanProperty var ext_id: String = _
  @Label("Ext Info")
  @BeanProperty var ext_info: Map[String, String] = _
  @Label("OAuth Info") // key=oauth服务标记，value=openid
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

  def apply(loginId: String, email: String, name: String, password: String, roleCodes: List[String], organizationCode: String = ""): EZ_Account = {
    val account = EZ_Account()
    account.login_id = loginId
    account.email = email
    account.name = name
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

  override def deleteByCode(code: String): Resp[Void] = storageObj.deleteByCode(code)

  override def deleteByLoginId(loginId: String, organizationCode: String): Resp[Void] = storageObj.deleteByLoginId(loginId, organizationCode)

  override def deleteByEmail(email: String, organizationCode: String): Resp[Void] = storageObj.deleteByEmail(email, organizationCode)

  override def deleteByLoginIdOrEmail(loginIdOrEmail: String, organizationCode: String): Resp[Void] =
    storageObj.deleteByLoginIdOrEmail(loginIdOrEmail, organizationCode)

}

trait EZ_Account_Base extends SecureStorage[EZ_Account] with StatusStorage[EZ_Account] {

  override def preSave(model: EZ_Account, context: EZStorageContext): Resp[EZ_Account] = {
    preSaveOrUpdate(model, context)
  }

  override def preUpdate(model: EZ_Account, context: EZStorageContext): Resp[EZ_Account] = {
    preSaveOrUpdate(model, context)
  }

  override def preSaveOrUpdate(model: EZ_Account, context: EZStorageContext): Resp[EZ_Account] = {
    if (model.login_id == null || model.login_id.trim.isEmpty
      || model.password == null || model.password.trim.isEmpty
      || model.email == null || model.email.trim.isEmpty) {
      Resp.badRequest("Require【Login_id】【password】【email】")
    } else {
      // 当账号不是oauth类型且登录ld包含@时，拒绝保存
      if ((model.oauth == null || model.oauth.isEmpty) && model.login_id.contains(BaseModel.SPLIT)) {
        Resp.badRequest(s"【login id】can't contains ${BaseModel.SPLIT}")
      } else {
        if (FormatHelper.validEmail(model.email)) {
          if (model.ext_id == null) {
            model.ext_id = ""
          }
          if (model.ext_info == null) {
            model.ext_info = Map()
          }
          if (model.id == null || model.id.trim == "") {
            if (existByEmail(model.email, model.organization_code).body) {
              Resp.badRequest("【email】exist")
            } else {
              model.password = packageEncryptPwd(model.login_id, model.password)
              model.code = assembleCode(model.login_id, model.organization_code)
              super.preSaveOrUpdate(model, context)
            }
          } else {
            model.code = assembleCode(model.login_id, model.organization_code)
            val existEmail = getByEmail(model.email, model.organization_code).body
            if (existEmail != null && existEmail.code != model.code) {
              Resp.badRequest("【email】exist")
            } else {
              super.preSaveOrUpdate(model, context)
            }
          }
        } else {
          Resp.badRequest("【email】format error")
        }
      }
    }
  }

  def assembleCode(loginId: String, organization_code: String): String = {
    organization_code + BaseModel.SPLIT + loginId
  }

  def packageEncryptPwd(loginId: String, password: String): String = {
    EncryptHelper.encrypt(loginId + password)
  }

  def findByOrganizationCode(organizationCode: String): Resp[List[EZ_Account]]

  def findEnableByOrganizationCode(organizationCode: String): Resp[List[EZ_Account]]

  def getByCode(code: String): Resp[EZ_Account]

  def getByLoginId(loginId: String, organizationCode: String): Resp[EZ_Account]

  def getByEmail(email: String, organizationCode: String): Resp[EZ_Account]

  def getByLoginIdOrEmail(loginIdOrEmail: String, organizationCode: String): Resp[EZ_Account]

  def getByOAuth(appName: String, authId: String, organizationCode: String): Resp[EZ_Account]

  def existByEmail(email: String, organizationCode: String): Resp[Boolean]

  def deleteByCode(code: String): Resp[Void]

  def deleteByLoginId(loginId: String, organizationCode: String): Resp[Void]

  def deleteByEmail(email: String, organizationCode: String): Resp[Void]

  def deleteByLoginIdOrEmail(loginIdOrEmail: String, organizationCode: String): Resp[Void]


}

object EZ_Account_Mongo extends MongoSecureStorage[EZ_Account] with MongoStatusStorage[EZ_Account] with EZ_Account_Base {

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

}

object EZ_Account_JDBC extends JDBCSecureStorage[EZ_Account] with JDBCStatusStorage[EZ_Account] with EZ_Account_Base {

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

}



