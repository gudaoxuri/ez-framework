package com.ecfront.ez.framework.service.auth.model

import com.ecfront.common.Resp
import com.ecfront.ez.framework.core.i18n.I18NProcessor.Impl
import com.ecfront.ez.framework.service.auth.CacheManager
import com.ecfront.ez.framework.service.jdbc._

import scala.beans.BeanProperty

/**
  * 资源实体
  */
@Entity("Resource")
case class EZ_Resource() extends BaseModel with SecureModel {

  @Unique
  @Require
  @Desc("Code", 1000, 0) // method@uri
  @BeanProperty var code: String = _
  @Index
  @Require
  @Desc("Method", 10, 0)
  @BeanProperty var method: String = _
  @Index
  @Require
  @Desc("URI", 800, 0)
  @BeanProperty var uri: String = _
  @Require
  @Desc("Name", 200, 0)
  @BeanProperty var name: String = _

}

object EZ_Resource extends SecureStorage[EZ_Resource] {

  def apply(method: String, uri: String, name: String): EZ_Resource = {
    val res = EZ_Resource()
    res.method = method
    res.uri = uri
    res.name = name.x
    res
  }

  override def preSave(model: EZ_Resource): Resp[EZ_Resource] = {
    preSaveOrUpdate(model)
  }

  override def preUpdate(model: EZ_Resource): Resp[EZ_Resource] = {
    preSaveOrUpdate(model)
  }

  override def preSaveOrUpdate(model: EZ_Resource): Resp[EZ_Resource] = {
    if (model.method == null || model.method.trim.isEmpty || model.uri == null || model.uri.trim.isEmpty) {
      logger.warn(s"Require【method】and【uri】")
      Resp.badRequest("Require【method】and【uri】")
    } else {
      if (model.uri.contains(BaseModel.SPLIT)) {
        logger.warn(s"【uri】can't contains ${BaseModel.SPLIT}")
        Resp.badRequest(s"【uri】can't contains ${BaseModel.SPLIT}")
      } else {
        model.code = assembleCode(model.method, model.uri)
        super.preSaveOrUpdate(model)
      }
    }
  }

  override def postSave(saveResult: EZ_Resource, preResult: EZ_Resource): Resp[EZ_Resource] = {
    postAddExt(saveResult)
    super.postSave(saveResult, preResult)
  }

  override def postUpdate(updateResult: EZ_Resource, preResult: EZ_Resource): Resp[EZ_Resource] = {
    postAddExt(updateResult)
    super.postUpdate(updateResult, preResult)
  }

  override def postSaveOrUpdate(saveOrUpdateResult: EZ_Resource, preResult: EZ_Resource): Resp[EZ_Resource] = {
    postAddExt(saveOrUpdateResult)
    super.postSaveOrUpdate(saveOrUpdateResult, preResult)
  }

  override def preDeleteById(id: Any): Resp[Any] = {
    preRemoveExt(doGetById(id).body)
    super.preDeleteById(id)
  }

  override def preDeleteByUUID(uuid: String): Resp[String] = {
    preRemoveExt(doGetByUUID(uuid).body)
    super.preDeleteByUUID(uuid)
  }

  override def preDeleteByCond(condition: String, parameters: List[Any]): Resp[(String, List[Any])] = {
    doFind(condition, parameters).body.foreach(preRemoveExt)
    super.preDeleteByCond(condition, parameters)
  }

  def deleteByCode(code: String): Resp[Void] = {
    deleteByCond( s"""code = ?""", List(code))
  }

  private def postAddExt(obj: EZ_Resource): Unit = {
    if (obj != null) {
      CacheManager.RBAC.addResource(obj)
    }
  }

  private def preRemoveExt(obj: EZ_Resource): Unit = {
    if (obj != null) {
      CacheManager.RBAC.removeResource(obj.code)
    }
  }

  override def preUpdateByCond(newValues: String, condition: String, parameters: List[Any]): Resp[(String, String, List[Any])] =
    Resp.notImplemented("")

  def assembleCode(method: String, uri: String): String = {
    method + BaseModel.SPLIT + uri
  }

}




