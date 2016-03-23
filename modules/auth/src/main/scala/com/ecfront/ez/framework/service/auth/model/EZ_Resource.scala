package com.ecfront.ez.framework.service.auth.model

import com.ecfront.common.Resp
import com.ecfront.ez.framework.service.auth.ServiceAdapter
import com.ecfront.ez.framework.service.storage.foundation._
import com.ecfront.ez.framework.service.storage.jdbc.{JDBCSecureStorage, JDBCStatusStorage}
import com.ecfront.ez.framework.service.storage.mongo.{MongoSecureStorage, MongoStatusStorage}

import scala.beans.BeanProperty

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

object EZ_Resource extends SecureStorageAdapter[EZ_Resource, EZ_Resource_Base]
  with StatusStorageAdapter[EZ_Resource, EZ_Resource_Base] with EZ_Resource_Base {

  override protected val storageObj: EZ_Resource_Base =
    if (ServiceAdapter.mongoStorage) EZ_Resource_Mongo else EZ_Resource_JDBC

  def apply(method: String, uri: String, name: String): EZ_Resource = {
    val res = EZ_Resource()
    res.method = method
    res.uri = uri
    res.name = name
    res.enable = true
    res
  }

  override def deleteByCode(code: String): Resp[Void] = storageObj.deleteByCode(code)

}

trait EZ_Resource_Base extends SecureStorage[EZ_Resource] with StatusStorage[EZ_Resource]{

  override def preSave(model: EZ_Resource, context: EZStorageContext): Resp[EZ_Resource] = {
    if (model.method == null || model.method.trim.isEmpty || model.uri == null || model.uri.trim.isEmpty) {
      Resp.badRequest("Require【method】and【uri】")
    } else {
      model.code = assembleCode(model.method, model.uri)
      super.preSave(model, context)
    }
  }

  override def preUpdate(model: EZ_Resource, context: EZStorageContext): Resp[EZ_Resource] = {
    if (model.method == null || model.method.trim.isEmpty || model.uri == null || model.uri.trim.isEmpty) {
      Resp.badRequest("Require【method】and【uri】")
    } else {
      model.code = assembleCode(model.method, model.uri)
      super.preUpdate(model, context)
    }
  }

  def assembleCode(method: String, uri: String): String = {
    method + BaseModel.SPLIT + uri
  }

  def deleteByCode(code: String): Resp[Void]

}

object EZ_Resource_Mongo extends MongoSecureStorage[EZ_Resource] with MongoStatusStorage[EZ_Resource] with EZ_Resource_Base{

  override def deleteByCode(code: String): Resp[Void] = {
    deleteByCond(s"""{"code":"$code"}""")
  }

}

object EZ_Resource_JDBC extends JDBCSecureStorage[EZ_Resource] with JDBCStatusStorage[EZ_Resource] with EZ_Resource_Base{

  override def deleteByCode(code: String): Resp[Void] = {
    deleteByCond( s"""code = ?""", List(code))
  }

}




