package com.ecfront.ez.framework.service.auth.model

import com.ecfront.common._
import com.ecfront.ez.framework.service.auth.ServiceAdapter
import com.ecfront.ez.framework.service.storage.foundation.{BaseStorage, _}
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
  // 此字段不为空时保存或更新账户时不对密码做加密
  @Ignore var exchange_pwd: String = _
  @Require
  @Label("Email")
  @BeanProperty var email: String = _
  @Label("Ext Id") // 用于关联其它对象以扩展属性，扩展Id多为业务系统用户信息表的主键
  @BeanProperty var ext_id: String = _
  @Label("Ext Info")
  @BeanProperty var ext_info: Map[String, Any] = _
  @Label("OAuth Info") // key=oauth服务标记，value=openid
  @BeanProperty var oauth: Map[String, String] = _
  @BeanProperty var organization_code: String = _
  @BeanProperty var role_codes: List[String] = List[String]()

}

object EZ_Account extends SecureStorageAdapter[EZ_Account, EZ_Account_Base]
  with StatusStorageAdapter[EZ_Account, EZ_Account_Base] with EZ_Account_Base {

  val SYSTEM_ACCOUNT_CODE = "sysadmin"

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
          if (model.image == null) {
            model.image = ""
          }
          if (model.oauth == null) {
            model.oauth = Map()
          }
          if (model.organization_code == null) {
            model.organization_code = ServiceAdapter.defaultOrganizationCode
          }
          if (model.role_codes == null) {
            model.role_codes = List()
          }
          if (model.ext_id == null) {
            model.ext_id = ""
          }
          if (model.ext_info == null) {
            model.ext_info = Map()
          }
          model.code = assembleCode(model.login_id, model.organization_code)
          if (model.exchange_pwd != null) {
            model.password = model.exchange_pwd
          } else {
            model.password = packageEncryptPwd(model.login_id, model.password)
          }
          if (model.id == null || model.id.trim == "") {
            if (existByEmail(model.email, model.organization_code).body) {
              Resp.badRequest("【email】exist")
            } else {
              if (EZ_Account.extAccountStorage != null) {
                val extObj = EZ_Account.extAccountStorage.save(EZ_Account.extAccountStorage.convertToEntity(model.ext_info), context).body
                model.ext_id = extObj.id
                model.ext_info = Map()
              }
              super.preSave(model, context)
            }
          } else {
            val existEmail = getByEmail(model.email, model.organization_code).body
            if (existEmail != null && existEmail.code != model.code) {
              Resp.badRequest("【email】exist")
            } else {
              if (model.ext_id.nonEmpty && EZ_Account.extAccountStorage != null) {
                EZ_Account.extAccountStorage.update(
                  EZ_Account.extAccountStorage.convertToEntity(model.ext_info + (BaseModel.Id_FLAG -> model.ext_id)), context).body
                model.ext_info = Map()
              }
              super.preUpdate(model, context)
            }
          }
        } else {
          Resp.badRequest("【email】format error")
        }
      }
    }
  }

  override def postGetEnabledByCond(condition: String, parameters: List[Any], getResult: EZ_Account, context: EZStorageContext): Resp[EZ_Account] = {
    if (getResult != null) {
      if (getResult.ext_id != null && getResult.ext_id.trim.nonEmpty && EZ_Account.extAccountStorage != null) {
        getResult.ext_info = JsonHelper.toObject[Map[String, Any]](EZ_Account.extAccountStorage.getById(getResult.ext_id.trim).body)
      }
    }
    super.postGetEnabledByCond(condition, parameters, getResult, context)
  }

  override def postFindEnabled(condition: String, parameters: List[Any], findResult: List[EZ_Account], context: EZStorageContext): Resp[List[EZ_Account]] = {
    if (findResult.nonEmpty && EZ_Account.extAccountStorage != null) {
      findResult.foreach {
        result =>
          if (result.ext_id != null && result.ext_id.trim.nonEmpty) {
            result.ext_info = JsonHelper.toObject[Map[String, Any]](EZ_Account.extAccountStorage.getById(result.ext_id.trim).body)
          }
      }
    }
    super.postFindEnabled(condition, parameters, findResult, context)
  }

  override def postPageEnabled(condition: String, parameters: List[Any],
                               pageNumber: Long, pageSize: Int, pageResult: Page[EZ_Account], context: EZStorageContext): Resp[Page[EZ_Account]] = {
    if (pageResult.objects.nonEmpty && EZ_Account.extAccountStorage != null) {
      pageResult.objects.foreach {
        result =>
          if (result.ext_id != null && result.ext_id.trim.nonEmpty) {
            result.ext_info = JsonHelper.toObject[Map[String, Any]](EZ_Account.extAccountStorage.getById(result.ext_id.trim).body)
          }
      }
    }
    super.postPageEnabled(condition, parameters, pageNumber, pageSize, pageResult, context)
  }

  override def postSave(saveResult: EZ_Account, context: EZStorageContext): Resp[EZ_Account] = {
    if (saveResult != null) {
      if (saveResult.ext_id != null && saveResult.ext_id.trim.nonEmpty && EZ_Account.extAccountStorage != null) {
        saveResult.ext_info = JsonHelper.toObject[Map[String, Any]](EZ_Account.extAccountStorage.getById(saveResult.ext_id.trim).body)
      }
    }
    super.postSave(saveResult, context)
  }

  override def postUpdate(updateResult: EZ_Account, context: EZStorageContext): Resp[EZ_Account] = {
    if (updateResult != null) {
      if (updateResult.ext_id != null && updateResult.ext_id.trim.nonEmpty && EZ_Account.extAccountStorage != null) {
        updateResult.ext_info = JsonHelper.toObject[Map[String, Any]](EZ_Account.extAccountStorage.getById(updateResult.ext_id.trim).body)
      }
    }
    super.postUpdate(updateResult, context)
  }

  override def postSaveOrUpdate(saveOrUpdateResult: EZ_Account, context: EZStorageContext): Resp[EZ_Account] = {
    if (saveOrUpdateResult != null) {
      if (saveOrUpdateResult.ext_id != null && saveOrUpdateResult.ext_id.trim.nonEmpty && EZ_Account.extAccountStorage != null) {
        saveOrUpdateResult.ext_info = JsonHelper.toObject[Map[String, Any]](EZ_Account.extAccountStorage.getById(saveOrUpdateResult.ext_id.trim).body)
      }
    }
    super.postSaveOrUpdate(saveOrUpdateResult, context)
  }

  override def preDeleteById(id: Any, context: EZStorageContext): Resp[Any] = {
    if (EZ_Account.extAccountStorage != null) {
      val objR = doGetById(id, context)
      if (objR && objR.body != null && objR.body.ext_id != null && objR.body.ext_id.trim.nonEmpty) {
        EZ_Account.extAccountStorage.deleteById(objR.body.ext_id.trim)
      }
    }
    super.preDeleteById(id, context)
  }

  override def preDeleteByCond(condition: String, parameters: List[Any],
                               context: EZStorageContext): Resp[(String, List[Any])] = {
    if (EZ_Account.extAccountStorage != null) {
      val objR = doFind(condition, parameters, context)
      if (objR && objR.body != null && objR.body.nonEmpty) {
        objR.body.foreach {
          obj =>
            if (obj.ext_id != null && obj.ext_id.trim.nonEmpty) {
              EZ_Account.extAccountStorage.deleteById(obj.ext_id.trim)
            }
        }
      }
    }
    super.preDeleteByCond(condition, parameters, context)
  }

  override def postGetById(id: Any, getResult: EZ_Account, context: EZStorageContext): Resp[EZ_Account] = {
    if (getResult != null) {
      if (getResult.ext_id != null && getResult.ext_id.trim.nonEmpty && EZ_Account.extAccountStorage != null) {
        getResult.ext_info = JsonHelper.toObject[Map[String, Any]](EZ_Account.extAccountStorage.getById(getResult.ext_id.trim).body)
      }
    }
    super.postGetById(id, getResult, context)
  }

  override def postGetByCond(condition: String, parameters: List[Any], getResult: EZ_Account, context: EZStorageContext): Resp[EZ_Account] = {
    if (getResult != null) {
      if (getResult.ext_id != null && getResult.ext_id.trim.nonEmpty && EZ_Account.extAccountStorage != null) {
        getResult.ext_info = JsonHelper.toObject[Map[String, Any]](EZ_Account.extAccountStorage.getById(getResult.ext_id.trim).body)
      }
    }
    super.postGetByCond(condition, parameters, getResult, context)
  }

  override def postFind(condition: String, parameters: List[Any], findResult: List[EZ_Account], context: EZStorageContext): Resp[List[EZ_Account]] = {
    if (findResult.nonEmpty && EZ_Account.extAccountStorage != null) {
      findResult.foreach {
        result =>
          if (result.ext_id != null && result.ext_id.trim.nonEmpty) {
            result.ext_info = JsonHelper.toObject[Map[String, Any]](EZ_Account.extAccountStorage.getById(result.ext_id.trim).body)
          }
      }
    }
    super.postFind(condition, parameters, findResult, context)
  }

  override def postPage(condition: String, parameters: List[Any],
                        pageNumber: Long, pageSize: Int, pageResult: Page[EZ_Account], context: EZStorageContext): Resp[Page[EZ_Account]] = {
    if (pageResult.objects.nonEmpty && EZ_Account.extAccountStorage != null) {
      pageResult.objects.foreach {
        result =>
          if (result.ext_id != null && result.ext_id.trim.nonEmpty) {
            result.ext_info = JsonHelper.toObject[Map[String, Any]](EZ_Account.extAccountStorage.getById(result.ext_id.trim).body)
          }
      }
    }
    super.postPage(condition, parameters, pageNumber, pageSize, pageResult, context)
  }

  override def preUpdateByCond(newValues: String, condition: String, parameters: List[Any], context: EZStorageContext): Resp[(String, String, List[Any])] =
    Resp.notImplemented("")

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



