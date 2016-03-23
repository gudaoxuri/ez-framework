package com.ecfront.ez.framework.service.auth.model

import com.ecfront.common.Resp
import com.ecfront.ez.framework.service.auth.ServiceAdapter
import com.ecfront.ez.framework.service.storage.foundation._
import com.ecfront.ez.framework.service.storage.jdbc.JDBCBaseStorage
import com.ecfront.ez.framework.service.storage.mongo.MongoBaseStorage

import scala.beans.BeanProperty

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

object EZ_Token_Info extends BaseStorageAdapter[EZ_Token_Info, EZ_Token_Info_Base] with EZ_Token_Info_Base {

  // 前端传入的token标识
  val TOKEN_FLAG = "__ez_token__"

  override protected val storageObj: EZ_Token_Info_Base =
    if (ServiceAdapter.mongoStorage) EZ_Token_Info_Mongo else EZ_Token_Info_JDBC

  override def save(model: EZ_Token_Info, token: EZ_Token_Info): Resp[EZ_Token_Info] = storageObj.save(model, token)

  override def deleteByLoginId(loginId: String): Resp[Void] = storageObj.deleteByLoginId(loginId)

}

trait EZ_Token_Info_Base extends BaseStorage[EZ_Token_Info] {

  def save(model: EZ_Token_Info, token: EZ_Token_Info): Resp[EZ_Token_Info]

  def deleteByLoginId(loginId: String): Resp[Void]

}

object EZ_Token_Info_Mongo extends MongoBaseStorage[EZ_Token_Info] with EZ_Token_Info_Base {

  override def save(model: EZ_Token_Info, token: EZ_Token_Info): Resp[EZ_Token_Info] = {
    deleteByCond( s"""{"login_id":"${token.login_id}"}""")
    save(model)
  }

  override def deleteByLoginId(loginId: String): Resp[Void] = {
    deleteByCond(s"""{"login_id":"$loginId"}""")
  }

}

object EZ_Token_Info_JDBC extends JDBCBaseStorage[EZ_Token_Info] with EZ_Token_Info_Base {

  override def save(model: EZ_Token_Info, token: EZ_Token_Info): Resp[EZ_Token_Info] = {
    deleteByCond( s"""login_id = ?""", List(token.login_id))
    save(model)
  }

  override def deleteByLoginId(loginId: String): Resp[Void] = {
    deleteByCond(s"""login_id = ?""", List(loginId))
  }

}



