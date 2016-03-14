package com.ecfront.ez.framework.service.storage.jdbc

import com.ecfront.common.{BeanHelper, FieldAnnotationInfo, Ignore}
import com.ecfront.ez.framework.service.storage.foundation._

object JDBCEntityContainer extends BaseEntityContainer[JDBCEntityInfo] {

  override protected def buildingEntityInfo(model: JDBCEntityInfo, clazz: Class[_], allAnnotations: List[FieldAnnotationInfo]): Unit = {
    val idFieldInfo = allAnnotations.find(_.annotation.isInstanceOf[Id]).orNull
    model.idFieldName = if (idFieldInfo != null) idFieldInfo.fieldName else BaseModel.Id_FLAG
    model.idStrategy = if (idFieldInfo != null) idFieldInfo.annotation.asInstanceOf[Id].strategy else "seq"
    model.textFieldNames = allAnnotations.filter(_.annotation.isInstanceOf[Text]).map {
      field =>
        field.fieldName
    }
    model.allFields = BeanHelper.findFields(clazz, filterAnnotations = Seq()).map {
      item =>
        val ttype = item._1 match {
          case name if model.textFieldNames.contains(name) => "text"
          case name if idFieldInfo != null && idFieldInfo.fieldName == name && idFieldInfo.annotation.asInstanceOf[Id].strategy != "" =>
            idFieldInfo.annotation.asInstanceOf[Id].strategy
          case _ => item._2
        }
        item._1 -> ttype
    }
    model.ignoreFieldNames = model.allFields.filter {
      field =>
        allAnnotations.filter(_.fieldName == field._1).exists {
          ann =>
            ann.annotation.getClass == classOf[Ignore]
        }
    }.keys.toList
    model.persistentFields = model.allFields.filter(field => !model.ignoreFieldNames.contains(field._1))
  }

}

case class JDBCEntityInfo() extends BaseEntityInfo() {
  var idFieldName: String = _
  var idStrategy: String = _
  var allFields: Map[String, String] = _
  var persistentFields: Map[String, String] = _
  var textFieldNames: List[String] = _
  var ignoreFieldNames: List[String] = _
}
