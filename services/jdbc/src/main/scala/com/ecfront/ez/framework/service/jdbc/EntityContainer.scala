package com.ecfront.ez.framework.service.jdbc

import com.ecfront.common.{BeanHelper, ClassScanHelper, Ignore}
import com.ecfront.ez.framework.core.logger.Logging
import com.ecfront.ez.framework.service.jdbc.dialect.FiledInfo

import scala.collection.mutable

/**
  * 基础实体信息容器
  *
  */
object EntityContainer extends Logging {

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
    val fieldDesc = allAnnotations.filter(_.annotation.isInstanceOf[Desc]).map {
      field =>
        val desc = field.annotation.asInstanceOf[Desc]
        field.fieldName -> (desc.label, desc.len, desc.scale)
    }.toMap
    val uuidFieldInfo = allAnnotations.find(_.annotation.isInstanceOf[UUID]).orNull
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
    val idFieldInfo = allAnnotations.find(_.annotation.isInstanceOf[Id]).orNull
    val idStrategy = if (idFieldInfo != null) idFieldInfo.annotation.asInstanceOf[Id].strategy else "seq"
    val allFields = BeanHelper.findFields(clazz, excludeAnnotations = Seq()).map {
      item =>
        val ttype = item._1 match {
          case name if idFieldInfo != null && idFieldInfo.fieldName == name && idFieldInfo.annotation.asInstanceOf[Id].strategy != "" =>
            idFieldInfo.annotation.asInstanceOf[Id].strategy
          case _ => item._2
        }
        item._1 -> ttype
    }
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
    model.fieldDesc = fieldDesc
    model.indexFieldNames = indexFieldNames
    model.uniqueFieldNames =
      if (uuidFieldInfo != null) {
        uniqueFieldNames :+ uuidFieldInfo.fieldName
      } else {
        uniqueFieldNames
      }
    model.requireFieldNames = requireFieldNames
    model.nowBySaveFieldNames = nowBySaveFieldNames
    model.nowByUpdateFieldNames = nowByUpdateFieldNames
    model.uuidFieldName = if (uuidFieldInfo != null) uuidFieldInfo.fieldName else null
    model.idFieldName = if (idFieldInfo != null) idFieldInfo.fieldName else BaseModel.Id_FLAG
    model.idStrategy = idStrategy
    model.allFields = allFields
    model.ignoreFieldNames = ignoreFieldNames
    model.persistentFields = persistentFields
    CONTAINER += tableName -> model
    logger.info( """Create model: %s""".format(clazz.getSimpleName))
  }

  def createTable(tableName: String): Unit = {
    val entityInfo = CONTAINER(tableName)
    val fields = entityInfo.persistentFields.map {
      field =>
        val fieldName = field._1.toLowerCase
        val fieldType = field._2.toLowerCase
        val fieldDesc =
          if (entityInfo.fieldDesc.contains(field._1)) {
            entityInfo.fieldDesc(field._1)
          } else if (entityInfo.uuidFieldName != null && entityInfo.uuidFieldName == field._1) {
            ("Business Key", 32, 0)
          } else {
            ("", 0, 0)
          }
        FiledInfo(fieldName, fieldType, fieldDesc._1, fieldDesc._2, fieldDesc._3)
    }.toList
    JDBCProcessor.createTableIfNotExist(entityInfo.tableName, entityInfo.tableDesc, fields,
      entityInfo.indexFieldNames, entityInfo.uniqueFieldNames, entityInfo.idFieldName)
  }

  def mvTable(oriTableName: String, newTableName: String): Unit = {
    JDBCProcessor.changeTableName(oriTableName, newTableName)
  }

}

class EntityInfo() {
  var clazz: Class[_] = _
  var tableName: String = _
  var tableDesc: String = _
  var fieldDesc: Map[String, (String, Int, Int)] = _
  var uniqueFieldNames: List[String] = _
  var indexFieldNames: List[String] = _
  var requireFieldNames: List[String] = _
  var nowBySaveFieldNames: List[String] = _
  var nowByUpdateFieldNames: List[String] = _
  var idFieldName: String = _
  var uuidFieldName: String = _
  var idStrategy: String = _
  var allFields: mutable.LinkedHashMap[String, String] = _
  var persistentFields: mutable.LinkedHashMap[String, String] = _
  var ignoreFieldNames: List[String] = _
}
