package com.ecfront.ez.framework.service.auth.model

import com.ecfront.common.Resp
import com.ecfront.ez.framework.service.auth.{CacheManager, ServiceAdapter}
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
  @Label("Code") // method@uri
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

trait EZ_Resource_Base extends SecureStorage[EZ_Resource] with StatusStorage[EZ_Resource] {

  override def preSave(model: EZ_Resource, context: EZStorageContext): Resp[EZ_Resource] = {
    preSaveOrUpdate(model, context)
  }

  override def preUpdate(model: EZ_Resource, context: EZStorageContext): Resp[EZ_Resource] = {
    preSaveOrUpdate(model, context)
  }

  override def preSaveOrUpdate(model: EZ_Resource, context: EZStorageContext): Resp[EZ_Resource] = {
    if (model.method == null || model.method.trim.isEmpty || model.uri == null || model.uri.trim.isEmpty) {
      Resp.badRequest("Require【method】and【uri】")
    } else {
      if (model.uri.contains(BaseModel.SPLIT)) {
        Resp.badRequest(s"【uri】can't contains ${BaseModel.SPLIT}")
      } else {
        model.code = assembleCode(model.method, model.uri)
        super.preSaveOrUpdate(model, context)
      }
    }
  }

  override def postEnableById(id: Any, context: EZStorageContext): Resp[Void] = {
    CacheManager.addResource(super.getById(id).body.code)
    super.postEnableById(id, context)
  }

  override def postDisableById(id: Any, context: EZStorageContext): Resp[Void] = {
    CacheManager.removeResource(super.getById(id).body.code)
    super.postDisableById(id, context)
  }

  override def postSave(saveResult: EZ_Resource, preResult: EZ_Resource, context: EZStorageContext): Resp[EZ_Resource] = {
    if (saveResult.enable) {
      CacheManager.addResource(saveResult.code)
    }
    super.postSave(saveResult, preResult, context)
  }

  override def postUpdate(updateResult: EZ_Resource, preResult: EZ_Resource, context: EZStorageContext): Resp[EZ_Resource] = {
    if (updateResult.enable) {
      CacheManager.addResource(updateResult.code)
    }
    super.postUpdate(updateResult, preResult, context)
  }

  override def postSaveOrUpdate(saveOrUpdateResult: EZ_Resource, preResult: EZ_Resource, context: EZStorageContext): Resp[EZ_Resource] = {
    if (saveOrUpdateResult.enable) {
      CacheManager.addResource(saveOrUpdateResult.code)
    }
    super.postSaveOrUpdate(saveOrUpdateResult, preResult, context)
  }

  override def postDeleteById(id: Any, context: EZStorageContext): Resp[Void] = {
    val resR = doGetById(id, context)
    if (resR && resR.body != null) {
      CacheManager.removeResource(resR.body.code)
    }
    super.postDeleteById(id, context)
  }

  override def preUpdateByCond(newValues: String, condition: String, parameters: List[Any],
                               context: EZStorageContext): Resp[(String, String, List[Any])] =
    Resp.notImplemented("")


  override def preDeleteByCond(condition: String, parameters: List[Any],
                               context: EZStorageContext): Resp[(String, List[Any])] = {
    val resR = doGetByCond(condition, parameters, context)
    if (resR && resR.body != null) {
      CacheManager.removeResource(resR.body.code)
    }
    super.preDeleteByCond(condition, parameters, context)
  }

  def assembleCode(method: String, uri: String): String = {
    method + BaseModel.SPLIT + uri
  }

  def deleteByCode(code: String): Resp[Void]

}

object EZ_Resource_Mongo extends MongoSecureStorage[EZ_Resource] with MongoStatusStorage[EZ_Resource] with EZ_Resource_Base {

  override def deleteByCode(code: String): Resp[Void] = {
    deleteByCond(s"""{"code":"$code"}""")
  }
}

object EZ_Resource_JDBC extends JDBCSecureStorage[EZ_Resource] with JDBCStatusStorage[EZ_Resource] with EZ_Resource_Base {

  override def deleteByCode(code: String): Resp[Void] = {
    deleteByCond( s"""code = ?""", List(code))
  }

}




