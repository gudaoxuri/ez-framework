package com.ecfront.ez.framework.service.storage.mongo

import com.ecfront.common.{BeanHelper, FieldAnnotationInfo}
import com.ecfront.ez.framework.service.storage.foundation.{BaseEntityContainer, BaseEntityInfo}

import scala.beans.BeanProperty

private[mongo] object MongoEntityContainer extends BaseEntityContainer[MongoEntityInfo] {

  override def buildingEntityInfo(model: MongoEntityInfo, clazz: Class[_], allAnnotations: List[FieldAnnotationInfo]): Unit = {
    model.persistentFields = (
      BeanHelper.findFieldAnnotations(
        clazz,
        Seq(classOf[BeanProperty])).map(_.fieldName).toSet -- Set("_modelClazz", "tableName")
      ).toList
  }
}

case class MongoEntityInfo() extends BaseEntityInfo() {
  var persistentFields: List[String] = _
}