package com.ecfront.ez.framework.service.auth.model

import com.ecfront.common.Resp
import com.ecfront.ez.framework.core.i18n.I18NProcessor.Impl
import com.ecfront.ez.framework.service.auth.CacheManager
import com.ecfront.ez.framework.service.jdbc._

import scala.beans.BeanProperty

/**
  * 组织（租户）实体
  */
@Entity("Organization")
case class EZ_Organization() extends SecureModel with StatusModel {

  @Unique
  @Require
  @Desc("Code",200,0)
  @BeanProperty var code: String = _
  @Unique
  @Require
  @Desc("Name",200,0)
  @BeanProperty var name: String = _
  @Desc("Image",200,0)
  @BeanProperty var image: String = _
  @Desc("Category",200,0)
  @BeanProperty var category: String = _

}

object EZ_Organization extends SecureStorage[EZ_Organization] with StatusStorage[EZ_Organization] {

  def apply(code: String, name: String, category: String = ""): EZ_Organization = {
    val org = EZ_Organization()
    org.code = code
    org.name = name.x
    org.category = category
    org.enable = true
    org.image = ""
    org
  }


  override def preSave(model: EZ_Organization): Resp[EZ_Organization] = {
    if (model.category == null) {
      model.category = ""
    }
    if (model.image == null) {
      model.image = ""
    }
    super.preSave(model)
  }

  override def preUpdate(model: EZ_Organization): Resp[EZ_Organization] = {
    if (model.category == null) {
      model.category = ""
    }
    if (model.image == null) {
      model.image = ""
    }
    super.preUpdate(model)
  }

  override def preSaveOrUpdate(model: EZ_Organization): Resp[EZ_Organization] = {
    if (model.category == null) {
      model.category = ""
    }
    if (model.image == null) {
      model.image = ""
    }
    super.preSaveOrUpdate(model)
  }

  override def postSave(saveResult: EZ_Organization, preResult: EZ_Organization): Resp[EZ_Organization] = {
    if (saveResult.enable) {
      postAddExt(saveResult)
    }
    CacheManager.RBAC.initOrganization(saveResult)
    super.postSave(saveResult, preResult)
  }

  override def postUpdate(updateResult: EZ_Organization, preResult: EZ_Organization): Resp[EZ_Organization] = {
    if (updateResult.enable) {
      postAddExt(updateResult)
    } else {
      preRemoveExt(updateResult)
    }
    super.postUpdate(updateResult, preResult)
  }

  override def postSaveOrUpdate(saveOrUpdateResult: EZ_Organization, preResult: EZ_Organization): Resp[EZ_Organization] = {
    if (preResult.id == null || preResult.id.trim == "") {
      if (saveOrUpdateResult.enable) {
        postAddExt(saveOrUpdateResult)
      }
    } else {
      if (saveOrUpdateResult.enable) {
        postAddExt(saveOrUpdateResult)
      } else {
        preRemoveExt(saveOrUpdateResult)
      }
    }
    super.postSaveOrUpdate(saveOrUpdateResult, preResult)
  }

  override def postEnableById(id: Any): Resp[Void] = {
    postAddExt(doGetById(id).body)
    super.postEnableById(id)
  }

  override def postEnableByUUID(uuid: String): Resp[Void] = {
    postAddExt(doGetByUUID(uuid).body)
    super.postEnableByUUID(uuid)
  }

  override def postEnableByCond(condition: String, parameters: List[Any]): Resp[Void] = {
    doFind(condition, parameters).body.foreach(postAddExt)
    super.postEnableByCond(condition, parameters)
  }

  override def postUpdateByCond(newValues: String, condition: String, parameters: List[Any]): Resp[Void] = {
    doFind(condition, parameters).body.foreach(postAddExt)
    super.postUpdateByCond(newValues, condition, parameters)
  }

  override def preDisableById(id: Any): Resp[Any] = {
    preRemoveExt(doGetById(id).body)
    super.preDisableById(id)
  }

  override def preDisableByUUID(uuid: String): Resp[String] = {
    preRemoveExt(doGetByUUID(uuid).body)
    super.preDisableByUUID(uuid)
  }

  override def preDisableByCond(condition: String, parameters: List[Any]): Resp[(String, List[Any])] = {
    doFind(condition, parameters).body.foreach(preRemoveExt)
    super.preDisableByCond(condition, parameters)
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
    super.preDeleteByCond(condition, parameters)
  }

  private def postAddExt(obj: EZ_Organization): Unit = {
    if (obj != null) {
      CacheManager.RBAC.addOrganization(obj)
    }
  }

  private def preRemoveExt(obj: EZ_Organization): Unit = {
    if (obj != null) {
      CacheManager.RBAC.removeOrganization(obj.code)
    }
  }

  def getByCode(code: String): Resp[EZ_Organization] = {
    getByCond(s"""code = ?""", List(code))
  }

  def disableByCode(code: String): Resp[Void] = {
    disableByCond(s"""code = ?""", List(code))
  }

  def enableByCode(code: String): Resp[Void] = {
    enableByCond(s"""code = ?""", List(code))
  }

  def deleteByCode(code: String): Resp[Void] = {
    deleteByCond(s"""code = ?""", List(code))
  }

  def findByCategory(category: String): Resp[List[EZ_Organization]] = {
    find(s"""category = ?""", List(category))
  }

  def findByOrganizationCode(organizationCode: String): Resp[List[EZ_Account]] = {
    find(s"""organization_code = ?""", List(organizationCode))
  }

  def findEnableByOrganizationCode(organizationCode: String): Resp[List[EZ_Account]] = {
    findEnabled(s"""organization_code = ?""", List(organizationCode))
  }

}


