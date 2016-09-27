package com.ecfront.ez.framework.service.jdbc

import com.ecfront.common.{BeanHelper, ClassScanHelper, Ignore}
import com.typesafe.scalalogging.slf4j.LazyLogging

/**
  * 基础实体信息容器
  *
  */
object EntityContainer extends LazyLogging {

  // Entity容器
  val CONTAINER = collection.mutable.Map[String, EntityInfo]()

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
    val idFieldInfo = allAnnotations.find(_.annotation.isInstanceOf[Id]).orNull
    val allFields = BeanHelper.findFields(clazz, excludeAnnotations = Seq())
    val ignoreFieldNames = allFields.filter {
      field =>
        allAnnotations.filter(_.fieldName == field._1).exists {
          ann =>
            ann.annotation.getClass == classOf[Ignore]
        }
    }.keys.toList
    val persistentFields = allFields.filter(field => !ignoreFieldNames.contains(field._1))
    val model = new EntityInfo
    model.clazz = clazz
    model.tableName = tableName
    model.tableDesc = tableDesc
    model.fieldLabel = fieldLabel
    model.uniqueFieldNames = uniqueFieldNames
    model.requireFieldNames = requireFieldNames
    model.nowBySaveFieldNames = nowBySaveFieldNames
    model.nowByUpdateFieldNames = nowByUpdateFieldNames
    model.idFieldName = if (idFieldInfo != null) idFieldInfo.fieldName else BaseModel.Id_FLAG
    model.allFields = allFields
    model.ignoreFieldNames = ignoreFieldNames
    model.persistentFields = persistentFields
    CONTAINER += tableName -> model
    logger.info( """Create model: %s""".format(clazz.getSimpleName))
  }

}

class EntityInfo() {
  var clazz: Class[_] = _
  var tableName: String = _
  var tableDesc: String = _
  var fieldLabel: Map[String, String] = _
  var uniqueFieldNames: List[String] = _
  var requireFieldNames: List[String] = _
  var nowBySaveFieldNames: List[String] = _
  var nowByUpdateFieldNames: List[String] = _
  var idFieldName: String = _
  var allFields: Map[String, String] = _
  var persistentFields: Map[String, String] = _
  var ignoreFieldNames: List[String] = _
}
