package com.ecfront.ez.framework.service.storage.mongo

import com.ecfront.common.{BeanHelper, FieldAnnotationInfo, Ignore}
import com.ecfront.ez.framework.service.storage.foundation.{BaseEntityContainer, BaseEntityInfo}

import scala.beans.BeanProperty

private[mongo] object MongoEntityContainer extends BaseEntityContainer[MongoEntityInfo] {

  override def buildingEntityInfo(model: MongoEntityInfo, clazz: Class[_], allAnnotations: List[FieldAnnotationInfo]): Unit = {
    model.ignoreFieldNames = BeanHelper.findFields(clazz, excludeAnnotations = Seq()).filter {
      field =>
        allAnnotations.filter(_.fieldName == field._1).exists {
          ann =>
            ann.annotation.getClass == classOf[Ignore]
        }
    }.keys.toList
  }
}

case class MongoEntityInfo() extends BaseEntityInfo() {
  var ignoreFieldNames: List[String] = _
}