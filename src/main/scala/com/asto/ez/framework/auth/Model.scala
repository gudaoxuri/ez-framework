package com.asto.ez.framework.auth

import com.asto.ez.framework.EZContext
import com.asto.ez.framework.storage._
import com.asto.ez.framework.storage.mongo._
import com.ecfront.common.{EncryptHelper, Resp}

import scala.beans.BeanProperty
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}

/**
  * 组织实体
  */
@Entity("Organization")
case class EZ_Organization() extends MongoBaseModel with MongoSecureModel with MongoStatusModel {

  @Unique
  @Label("编码")
  @BeanProperty var code: String = _
  @BeanProperty var name: String = _
  @BeanProperty var image: String = _

}

object EZ_Organization extends MongoBaseStorage[EZ_Organization] with MongoSecureStorage[EZ_Organization] with MongoStatusStorage[EZ_Organization] {

  def getByCode(code: String): Future[Resp[EZ_Organization]] = {
    getByCond(s"""{"code":"$code"}""")
  }

}

/**
  * 资源实体
  */
@Entity("Resource")
case class EZ_Resource() extends MongoBaseModel with MongoSecureModel with MongoStatusModel {

  @Unique
  @Label("编码（方法+路径）")
  @BeanProperty var code: String = _
  @BeanProperty var method: String = _
  @BeanProperty var uri: String = _
  @BeanProperty var name: String = _

}

object EZ_Resource extends MongoBaseStorage[EZ_Resource] with MongoSecureStorage[EZ_Resource] with MongoStatusStorage[EZ_Resource] {

  override protected def preSave(model: EZ_Resource, context: EZContext): Future[Resp[EZ_Resource]] = {
    if (model.method == null || model.method.trim.isEmpty || model.uri == null || model.uri.trim.isEmpty) {
      Future(Resp.badRequest("Require【method】and【uri】"))
    } else {
      model.code = assembleCode(model.method, model.uri)
      Future(Resp.success(model))
    }
  }

  override protected def preUpdate(model: EZ_Resource, context: EZContext): Future[Resp[EZ_Resource]] = {
    if (model.method == null || model.method.trim.isEmpty || model.uri == null || model.uri.trim.isEmpty) {
      Future(Resp.badRequest("Require【method】and【uri】"))
    } else {
      model.code = assembleCode(model.method, model.uri)
      Future(Resp.success(model))
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
case class EZ_Role() extends MongoBaseModel with MongoSecureModel with MongoStatusModel {

  @Unique
  @Label("编码")
  @BeanProperty var code: String = _
  @BeanProperty var flag: String = _
  @BeanProperty var name: String = _
  @BeanProperty var resource_codes: List[String] = List[String]()
  @BeanProperty var organization_code: String = _

}

object EZ_Role extends MongoBaseStorage[EZ_Role] with MongoSecureStorage[EZ_Role] with MongoStatusStorage[EZ_Role] {

  val SYSTEM_ROLE_CODE = "system"

  override protected def preSave(model: EZ_Role, context: EZContext): Future[Resp[EZ_Role]] = {
    if (model.flag == null || model.flag.trim.isEmpty) {
      Future(Resp.badRequest("Require【flag】"))
    } else {
      model.code = assembleCode(model.flag, model.organization_code)
      Future(Resp.success(model))
    }
  }

  override protected def preUpdate(model: EZ_Role, context: EZContext): Future[Resp[EZ_Role]] = {
    if (model.flag == null || model.flag.trim.isEmpty) {
      Future(Resp.badRequest("Require【flag】"))
    } else {
      model.code = assembleCode(model.flag, model.organization_code)
      Future(Resp.success(model))
    }
  }

  def assembleCode(flag: String, organization_code: String): String = {
    organization_code + BaseModel.SPLIT + flag
  }

  def findByCodes(codes: List[String]): Future[Resp[List[EZ_Role]]] = {
    if (codes != null && codes.nonEmpty) {
      val strCodes = codes.mkString("\"", ",", "\"")
      find(s"""{"code":{"$$in":[$strCodes]}}""")
    } else {
      Future(Resp.success(List()))
    }
  }

}

/**
  * 账号实体
  */
@Entity("Account")
case class EZ_Account() extends MongoBaseModel with MongoSecureModel with MongoStatusModel {

  @Unique
  @Label("登录名称")
  @BeanProperty var login_id: String = _
  @BeanProperty var name: String = _
  @BeanProperty var image: String = _
  @BeanProperty var password: String = _
  @Unique
  @Label("登录邮箱")
  @BeanProperty var email: String = _
  @BeanProperty var ext_id: String = _
  @BeanProperty var ext_info: Map[String, String] = _
  @BeanProperty var organization_code: String = _
  @BeanProperty var role_codes: List[String] = List[String]()

}

object EZ_Account extends MongoBaseStorage[EZ_Account] with MongoSecureStorage[EZ_Account] with MongoStatusStorage[EZ_Account] {

  val SYSTEM_ACCOUNT_CODE = "sysadmin"

  override protected def preSave(model: EZ_Account, context: EZContext): Future[Resp[EZ_Account]] = {
    if (model.login_id == null || model.login_id.trim.isEmpty || model.password == null || model.password.trim.isEmpty) {
      Future(Resp.badRequest("Require【Login_id】and【password】"))
    } else {
      model.password = packageEncryptPwd(model.login_id, model.password)
      Future(Resp.success(model))
    }
  }

  override protected def preUpdate(model: EZ_Account, context: EZContext): Future[Resp[EZ_Account]] = {
    if (model.login_id == null || model.login_id.trim.isEmpty || model.password == null || model.password.trim.isEmpty) {
      Future(Resp.badRequest("Require【Login_id】and【password】"))
    } else {
      model.password = packageEncryptPwd(model.login_id, model.password)
      Future(Resp.success(model))
    }
  }

  def getByLoginId(login_id: String): Future[Resp[EZ_Account]] = {
    getByCond(s"""{"login_id":"$login_id"}""")
  }

  def packageEncryptPwd(loginId: String, password: String): String = {
    EncryptHelper.encrypt(loginId + password)
  }

}

@Entity("Token Info")
case class EZ_Token_Info() extends MongoBaseModel {

  @BeanProperty var login_id: String = _
  @BeanProperty var login_name: String = _
  @BeanProperty var organization: EZ_Organization = _
  @BeanProperty var roles: List[EZ_Role] = _
  @BeanProperty var ext_id: String = _
  @BeanProperty var last_login_time: Long = _

}

object EZ_Token_Info extends MongoBaseStorage[EZ_Token_Info] {

  val TOKEN_FLAG = "__ez_token__"

  def save(model: EZ_Token_Info, token: EZ_Token_Info): Future[Resp[String]] = {
    val p = Promise[Resp[String]]()
    deleteByCond(s"""{"login_id":"${token.login_id}"}""").onSuccess {
      case delResp =>
        save(model).onSuccess {
          case saveResp => p.success(saveResp)
        }
    }
    p.future
  }

}


