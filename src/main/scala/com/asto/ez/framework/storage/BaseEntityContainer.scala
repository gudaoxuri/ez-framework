package com.asto.ez.framework.storage

import java.lang.reflect.ParameterizedType

import com.ecfront.common.{FieldAnnotationInfo, BeanHelper, ClassScanHelper}
import com.typesafe.scalalogging.slf4j.LazyLogging

trait BaseEntityContainer[E <: BaseEntityInfo] extends LazyLogging {

  protected val _modelClazz = this.getClass.getGenericInterfaces()(0).asInstanceOf[ParameterizedType].getActualTypeArguments()(0).asInstanceOf[Class[E]]

  val CONTAINER = collection.mutable.Map[String, E]()

  def autoBuilding(basePackage: String) = {
    ClassScanHelper.scan[Entity](basePackage).foreach {
      clazz =>
        val tableName = clazz.getSimpleName.toLowerCase
        buildingEntityInfo(clazz, tableName)
    }
  }

  protected def buildingEntityInfo(model: E,clazz: Class[_], allAnnotations:List[FieldAnnotationInfo]): Unit

  def buildingEntityInfo(clazz: Class[_], tableName: String): Unit = {
    val entityAnnotation = BeanHelper.getClassAnnotation[Entity](clazz).get
    val tableDesc = entityAnnotation.desc
    val allAnnotations = BeanHelper.findFieldAnnotations(clazz).toList
    val fieldLabel = allAnnotations.filter(_.annotation.isInstanceOf[Label]).map {
      field =>
        field.fieldName -> field.annotation.asInstanceOf[Label].label
    }.toMap
    val fieldDesc = allAnnotations.filter(_.annotation.isInstanceOf[Desc]).map {
      field =>
        field.fieldName -> field.annotation.asInstanceOf[Desc].desc
    }.toMap
    val indexFieldNames = allAnnotations.filter(_.annotation.isInstanceOf[Index]).map {
      field =>
        field.fieldName
    }
    val uniqueFieldNames = allAnnotations.filter(_.annotation.isInstanceOf[Unique]).map {
      field =>
        field.fieldName
    }
    val requireFieldNames = allAnnotations.filter(_.annotation.isInstanceOf[Require]).map {
      field =>
        field.fieldName
    }
    val model = _modelClazz.newInstance()
    model.clazz = clazz
    model.tableName = tableName
    model.tableDesc = tableDesc
    model.fieldLabel = fieldLabel
    model.fieldDesc = fieldDesc
    model.indexFieldNames = indexFieldNames
    model.uniqueFieldNames = uniqueFieldNames
    model.requireFieldNames = requireFieldNames
    buildingEntityInfo(model,clazz,allAnnotations)
    CONTAINER += tableName -> model
    logger.info( """Create model: %s""".format(clazz.getSimpleName))
  }

}

class BaseEntityInfo() {
  var clazz: Class[_] = _
  var tableName: String = _
  var tableDesc: String = _
  var fieldLabel: Map[String, String] = _
  var fieldDesc: Map[String, String] = _
  var indexFieldNames: List[String] = _
  var uniqueFieldNames: List[String] = _
  var requireFieldNames: List[String] = _
}
