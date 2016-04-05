package com.ecfront.ez.framework.service.storage.foundation

import java.lang.reflect.ParameterizedType

import com.ecfront.common.{BeanHelper, ClassScanHelper, FieldAnnotationInfo}
import com.typesafe.scalalogging.slf4j.LazyLogging

/**
  * 基础实体信息容器
  *
  * @tparam E 实体信息类型
  */
private[storage] trait BaseEntityContainer[E <: BaseEntityInfo] extends LazyLogging {

  protected val _modelClazz = this.getClass.getGenericInterfaces()(0)
    .asInstanceOf[ParameterizedType].getActualTypeArguments()(0).asInstanceOf[Class[E]]

  // Entity容器
  val CONTAINER = collection.mutable.Map[String, E]()

  /**
    * 根据package 查找子孙类中带@Entity注解的类
    *
    * @param rootPackage 查找的根package
    */
  def autoBuilding(rootPackage: String): Unit = {
    ClassScanHelper.scan[Entity](rootPackage).foreach {
      clazz =>
        val tableName = clazz.getSimpleName.toLowerCase
        buildingEntityInfo(clazz, tableName)
    }
  }

  /**
    * 构建持久化实现类的实体信息
    *
    * @param model          实体类
    * @param clazz          实体类型
    * @param allAnnotations 实体类中所有的字段注解
    */
  protected def buildingEntityInfo(model: E, clazz: Class[_], allAnnotations: List[FieldAnnotationInfo]): Unit

  /**
    * 构建持久化基础实体信息
    *
    * @param clazz     实体类型
    * @param tableName 表名
    */
  def buildingEntityInfo(clazz: Class[_], tableName: String): Unit = {
    val entityAnnotation = BeanHelper.getClassAnnotation[Entity](clazz).get
    val tableDesc = entityAnnotation.desc
    val allAnnotations = BeanHelper.findFieldAnnotations(clazz).toList
    val fieldLabel = allAnnotations.filter(_.annotation.isInstanceOf[Label]).map {
      field =>
        field.fieldName -> field.annotation.asInstanceOf[Label].label
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
    val nowBySaveFieldNames = allAnnotations.filter(_.annotation.isInstanceOf[NowBySave]).map {
      field =>
        field.fieldName
    }
    val nowByUpdateFieldNames = allAnnotations.filter(_.annotation.isInstanceOf[NowByUpdate]).map {
      field =>
        field.fieldName
    }
    val model = _modelClazz.newInstance()
    model.clazz = clazz
    model.tableName = tableName
    model.tableDesc = tableDesc
    model.fieldLabel = fieldLabel
    model.indexFieldNames = indexFieldNames
    model.uniqueFieldNames = uniqueFieldNames
    model.requireFieldNames = requireFieldNames
    model.nowBySaveFieldNames = nowBySaveFieldNames
    model.nowByUpdateFieldNames = nowByUpdateFieldNames
    buildingEntityInfo(model, clazz, allAnnotations)
    CONTAINER += tableName -> model
    logger.info( """Create model: %s""".format(clazz.getSimpleName))
  }

}

/**
  * 持久化基础实体信息
  */
class BaseEntityInfo() {
  var clazz: Class[_] = _
  var tableName: String = _
  var tableDesc: String = _
  var fieldLabel: Map[String, String] = _
  var indexFieldNames: List[String] = _
  var uniqueFieldNames: List[String] = _
  var requireFieldNames: List[String] = _
  var nowBySaveFieldNames: List[String] = _
  var nowByUpdateFieldNames: List[String] = _
}
