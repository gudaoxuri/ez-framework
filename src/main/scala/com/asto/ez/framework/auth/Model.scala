package com.asto.ez.framework.auth

import com.asto.ez.framework.storage._
import com.asto.ez.framework.storage.mongo.{MongoBaseModel, MongoSecureModel, MongoStatusModel}
import com.ecfront.common.Resp

import scala.beans.BeanProperty
import scala.concurrent.{Future, Promise}

/**
  * 资源实体
  */
@Entity("Resources")
case class EZ_Resource() extends MongoBaseModel with MongoSecureModel with MongoStatusModel {
  @Unique
  @Label("编码")
  @BeanProperty var code: String = _
  @BeanProperty var method: String = _
  @BeanProperty var uri: String = _
  @BeanProperty var name: String = _
}

object EZ_Resource {
  val SPLIT = "@"
}

/**
  * 角色实体
  */
@Entity("Roles")
case class EZ_Role() extends MongoBaseModel with MongoSecureModel with MongoStatusModel {
  @Unique
  @Label("编码")
  @BeanProperty var code: String = _
  @BeanProperty var name: String = _
  @BeanProperty var resource_codes: List[String] = List[String]()
}

object EZ_Role {

  val model = EZ_Role()

  def findByCodes(codes: List[String]): Future[Resp[List[model.type]]] = {
    if (codes != null && codes.nonEmpty) {
      val strCodes = codes.mkString("\"", ",", "\"")
      model.find(s"""{"code":{"$$in",[$strCodes]}}""")
    } else {
      Future(Resp.success(List()))
    }
  }

}

/**
  * 组织实体
  */
@Entity("Organizations")
case class EZ_Organization() extends MongoBaseModel with MongoSecureModel with MongoStatusModel {
  @Unique
  @Label("编码")
  @BeanProperty var code: String = _
  @BeanProperty var name: String = _
  @BeanProperty var image: String = _
}

object EZ_Organization {

  val model = EZ_Organization()

  def getByCode(code: String): Future[Resp[model.type]] = {
    model.getByCond(s"""{"code":"$code"}""")
  }

}

/**
  * 账号实体
  */
@Entity("Accounts")
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

object EZ_Account {

  val model = EZ_Account()

  def getByLoginId(login_id: String): Future[Resp[model.type]] = {
    model.getByCond(s"""{"login_id":"$login_id"}""")
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

object EZ_Token_Info {

  val model = EZ_Token_Info()
  val TOKEN_FLAG = "__ez_token__"

  def save(token: EZ_Token_Info): Future[Resp[String]] = {
    val p = Promise[Resp[String]]()
    model.deleteByCond(s"""{"login_id":"${token.login_id}"}""").onSuccess {
      case delResp =>
        token.save().onSuccess {
          case saveResp => p.success(saveResp)
        }
    }
    p.future
  }

}


