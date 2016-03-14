package com.ecfront.ez.framework.service.auth

import com.ecfront.common.{EncryptHelper, FormatHelper, Resp}
import com.ecfront.ez.framework.service.storage.foundation._
import com.ecfront.ez.framework.service.storage.mongo.{MongoBaseStorage, MongoSecureStorage, MongoStatusStorage}

import scala.beans.BeanProperty

/**
  * 组织实体
  */
@Entity("Organization")
case class EZ_Organization() extends BaseModel with SecureModel with StatusModel {

  @Unique
  @Require
  @Label("Code")
  @BeanProperty var code: String = _
  @Require
  @Label("Name")
  @BeanProperty var name: String = _
  @BeanProperty var image: String = _

}

object EZ_Organization extends MongoBaseStorage[EZ_Organization] with MongoSecureStorage[EZ_Organization] with MongoStatusStorage[EZ_Organization] {

  def apply(code: String, name: String): EZ_Organization = {
    val org = EZ_Organization()
    org.code = code
    org.name = name
    org.enable = true
    org
  }

  def getByCode(code: String): Resp[EZ_Organization] = {
    getByCond( s"""{"code":"$code"}""")
  }

}

/**
  * 资源实体
  */
@Entity("Resource")
case class EZ_Resource() extends BaseModel with SecureModel with StatusModel {

  @Unique
  @Require
  @Label("Code（Method+URI）")
  @BeanProperty var code: String = _
  @Require
  @Label("Method")
  @BeanProperty var method: String = _
  @Require
  @Label("URI")
  @BeanProperty var uri: String = _
  @Require
  @Label("Name")
  @BeanProperty var name: String = _

}

object EZ_Resource extends MongoBaseStorage[EZ_Resource] with MongoSecureStorage[EZ_Resource] with MongoStatusStorage[EZ_Resource] {

  def apply(method: String, uri: String, name: String): EZ_Resource = {
    val res = EZ_Resource()
    res.method = method
    res.uri = uri
    res.name = name
    res.enable = true
    res
  }

  override protected def preSave(model: EZ_Resource, context: EZStorageContext): Resp[EZ_Resource] = {
    if (model.method == null || model.method.trim.isEmpty || model.uri == null || model.uri.trim.isEmpty) {
      Resp.badRequest("Require【method】and【uri】")
    } else {
      model.code = assembleCode(model.method, model.uri)
      Resp.success(model)
    }
  }

  override protected def preUpdate(model: EZ_Resource, context: EZStorageContext): Resp[EZ_Resource] = {
    if (model.method == null || model.method.trim.isEmpty || model.uri == null || model.uri.trim.isEmpty) {
      Resp.badRequest("Require【method】and【uri】")
    } else {
      model.code = assembleCode(model.method, model.uri)
      Resp.success(model)
    }
  }

  def assembleCode(method: String, uri: String): String = {
    method + BaseModel.SPLIT + uri
  }

}

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

object EZ_Role extends MongoBaseStorage[EZ_Role] with MongoSecureStorage[EZ_Role] with MongoStatusStorage[EZ_Role] {

  val SYSTEM_ROLE_CODE = "system"
  val USER_ROLE_CODE = "user"

  def apply(flag: String, name: String, resourceCodes: List[String]): EZ_Role = {
    val role = EZ_Role()
    role.flag = flag
    role.name = name
    role.organization_code = ""
    role.enable = true
    role.resource_codes = resourceCodes
    role
  }

  override protected def preSave(model: EZ_Role, context: EZStorageContext): Resp[EZ_Role] = {
    if (model.flag == null || model.flag.trim.isEmpty) {
      Resp.badRequest("Require【flag】")
    } else {
      model.code = assembleCode(model.flag, model.organization_code)
      Resp.success(model)
    }
  }

  override protected def preUpdate(model: EZ_Role, context: EZStorageContext): Resp[EZ_Role] = {
    if (model.flag == null || model.flag.trim.isEmpty) {
      Resp.badRequest("Require【flag】")
    } else {
      model.code = assembleCode(model.flag, model.organization_code)
      Resp.success(model)
    }
  }

  def assembleCode(flag: String, organization_code: String): String = {
    organization_code + BaseModel.SPLIT + flag
  }

  def findByCodes(codes: List[String]): Resp[List[EZ_Role]] = {
    if (codes != null && codes.nonEmpty) {
      val strCodes = codes.mkString("\"", ",", "\"")
      find( s"""{"code":{"$$in":[$strCodes]}}""")
    } else {
      Resp.success(List())
    }
  }

}

/**
  * 账号实体
  */
@Entity("Account")
case class EZ_Account() extends BaseModel with SecureModel with StatusModel {

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
  @BeanProperty var ext_id: String = _
  @BeanProperty var ext_info: Map[String, String] = _
  @BeanProperty var oauth: Map[String, String] = _
  @BeanProperty var organization_code: String = _
  @BeanProperty var role_codes: List[String] = List[String]()

}

object EZ_Account extends MongoBaseStorage[EZ_Account] with MongoSecureStorage[EZ_Account] with MongoStatusStorage[EZ_Account] {

  val SYSTEM_ACCOUNT_CODE = "sysadmin"

  val VIRTUAL_EMAIL = "@virtual.is"

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

  override protected def preSave(model: EZ_Account, context: EZStorageContext): Resp[EZ_Account] = {
    if (model.login_id == null || model.login_id.trim.isEmpty
      || model.password == null || model.password.trim.isEmpty
      || model.email == null || model.email.trim.isEmpty) {
      Resp.badRequest("Require【Login_id】【password】【email】")
    } else {
      if ((model.oauth==null || model.oauth.isEmpty) && model.login_id.contains("@")) {
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

  override protected def preUpdate(model: EZ_Account, context: EZStorageContext): Resp[EZ_Account] = {
    if (model.login_id == null || model.login_id.trim.isEmpty
      || model.password == null || model.password.trim.isEmpty
      || model.email == null || model.email.trim.isEmpty) {
      Resp.badRequest("Require【Login_id】【password】【email】")
    } else {
      if ((model.oauth==null || model.oauth.isEmpty) && model.login_id.contains("@")) {
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

  override protected def preSaveOrUpdate(model: EZ_Account, context: EZStorageContext): Resp[EZ_Account] = {
    if (model.login_id == null || model.login_id.trim.isEmpty
      || model.password == null || model.password.trim.isEmpty
      || model.email == null || model.email.trim.isEmpty) {
      Resp.badRequest("Require【Login_id】【password】【email】")
    } else {
      if ((model.oauth==null || model.oauth.isEmpty) && model.login_id.contains("@")) {
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

  def getByLoginId(loginId: String): Resp[EZ_Account] = {
    getByCond( s"""{"login_id":"$loginId"}""")
  }

  def getByLoginIdOrEmail(loginIdOrEmail: String): Resp[EZ_Account] = {
    getByCond( s"""{"$$or":[{"login_id":"$loginIdOrEmail"},{"email":"$loginIdOrEmail"}]}""")
  }

  def getByOAuth(authId: String, appName: String): Resp[EZ_Account] = {
    getByCond( s"""{"oauth.$appName":"$authId"}""")
  }


  def packageEncryptPwd(loginId: String, password: String): String = {
    EncryptHelper.encrypt(loginId + password)
  }

}

@Entity("Token Info")
case class EZ_Token_Info() extends BaseModel {

  @BeanProperty var login_id: String = _
  @BeanProperty var login_name: String = _
  @BeanProperty var image: String = _
  @BeanProperty var organization: EZ_Organization = _
  @BeanProperty var roles: List[EZ_Role] = _
  @BeanProperty var ext_id: String = _
  @BeanProperty var ext_info: Map[String, String] = _
  @BeanProperty var last_login_time: Long = _

}

object EZ_Token_Info extends MongoBaseStorage[EZ_Token_Info] {

  val TOKEN_FLAG = "__ez_token__"

  def save(model: EZ_Token_Info, token: EZ_Token_Info): Resp[EZ_Token_Info] = {
    deleteByCond( s"""{"login_id":"${token.login_id}"}""")
    save(model)
  }

}


/**
  * 菜单实体
  */
@Entity("Menu")
case class EZ_Menu() extends BaseModel with SecureModel with StatusModel {

  @Unique
  @Require
  @Label("URI")
  @BeanProperty var uri: String = _
  @Require
  @Label("Name")
  @BeanProperty var name: String = _
  @BeanProperty var icon: String = ""
  @BeanProperty var translate: String = ""
  @BeanProperty var role_codes: List[String] = List[String]()
  @BeanProperty var parent_uri: String = null
  @BeanProperty var sort: Int = 0

}

object EZ_Menu extends MongoBaseStorage[EZ_Menu] with MongoSecureStorage[EZ_Menu] with MongoStatusStorage[EZ_Menu] {

  def apply(uri: String, name: String, parent_uri: String, roleCodes: List[String], icon: String = "", translate: String = "", sort: Int = 0): EZ_Menu = {
    val menu = EZ_Menu()
    menu.uri = uri
    menu.name = name
    menu.parent_uri = parent_uri
    menu.icon = icon
    menu.translate = translate
    menu.role_codes = roleCodes
    menu.sort = sort
    menu.enable = true
    menu
  }

}



