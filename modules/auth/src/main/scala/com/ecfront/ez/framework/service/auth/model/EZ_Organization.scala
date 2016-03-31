package com.ecfront.ez.framework.service.auth.model

import com.ecfront.common.Resp
import com.ecfront.ez.framework.service.auth.ServiceAdapter
import com.ecfront.ez.framework.service.storage.foundation._
import com.ecfront.ez.framework.service.storage.jdbc.{JDBCSecureStorage, JDBCStatusStorage}
import com.ecfront.ez.framework.service.storage.mongo.{MongoSecureStorage, MongoStatusStorage}

import scala.beans.BeanProperty

/**
  * 组织（租户）实体
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
  @Label("Image")
  @BeanProperty var image: String = _

}

object EZ_Organization extends SecureStorageAdapter[EZ_Organization, EZ_Organization_Base]
  with StatusStorageAdapter[EZ_Organization, EZ_Organization_Base] with EZ_Organization_Base {

  // 默认组织
  val DEFAULT_ORGANIZATION_CODE = ""

  override protected val storageObj: EZ_Organization_Base =
    if (ServiceAdapter.mongoStorage) EZ_Organization_Mongo else EZ_Organization_JDBC

  def apply(code: String, name: String): EZ_Organization = {
    val org = EZ_Organization()
    org.code = code
    org.name = name
    org.enable = true
    org
  }

  override def getByCode(code: String): Resp[EZ_Organization] = storageObj.getByCode(code)

  override def deleteByCode(code: String): Resp[Void] = storageObj.deleteByCode(code)

}

trait EZ_Organization_Base extends SecureStorage[EZ_Organization] with StatusStorage[EZ_Organization] {

  override def preSave(model: EZ_Organization, context: EZStorageContext): Resp[EZ_Organization] = {
    preSaveOrUpdate(model, context)
  }

  override def preUpdate(model: EZ_Organization, context: EZStorageContext): Resp[EZ_Organization] = {
    preSaveOrUpdate(model, context)
  }

  override def preSaveOrUpdate(model: EZ_Organization, context: EZStorageContext): Resp[EZ_Organization] = {
    if (model.id == null || model.id.trim == "") {
      if (model.image == null) {
        model.image = ""
      }
      super.preSave(model, context)
    } else {
      model.code = null
      super.preUpdate(model, context)
    }
  }

  def getByCode(code: String): Resp[EZ_Organization]

  def deleteByCode(code: String): Resp[Void]

}

object EZ_Organization_Mongo extends
  MongoSecureStorage[EZ_Organization] with MongoStatusStorage[EZ_Organization] with EZ_Organization_Base {

  override def getByCode(code: String): Resp[EZ_Organization] = {
    getByCond( s"""{"code":"$code"}""")
  }

  override def deleteByCode(code: String): Resp[Void] = {
    deleteByCond(s"""{"code":"$code"}""")
  }

}

object EZ_Organization_JDBC extends
  JDBCSecureStorage[EZ_Organization] with JDBCStatusStorage[EZ_Organization] with EZ_Organization_Base {

  override def getByCode(code: String): Resp[EZ_Organization] = {
    getByCond( s"""code = ?""", List(code))
  }

  override def deleteByCode(code: String): Resp[Void] = {
    deleteByCond( s"""code = ?""", List(code))
  }

}


