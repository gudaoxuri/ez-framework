package com.asto.ez.framework.storage.mongo

import com.asto.ez.framework.storage.{BaseEntityContainer, BaseEntityInfo}
import com.ecfront.common.{BeanHelper, FieldAnnotationInfo}

import scala.beans.BeanProperty

object MongoEntityContainer extends BaseEntityContainer[MongoEntityInfo] {

  override def buildingEntityInfo(model: MongoEntityInfo, clazz: Class[_], allAnnotations: List[FieldAnnotationInfo]): Unit = {
    model.persistentFields = (BeanHelper.findFieldAnnotations(clazz, Seq(classOf[BeanProperty])).map(_.fieldName).toSet -- Set("_modelClazz", "_tableName")).toList
  }
}

case class MongoEntityInfo() extends BaseEntityInfo() {
  var persistentFields: List[String] = _
}