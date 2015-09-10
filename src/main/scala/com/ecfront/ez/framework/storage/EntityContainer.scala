package com.ecfront.ez.framework.storage

import com.ecfront.common.{BeanHelper, ClassScanHelper, FieldAnnotationInfo, Ignore}
import com.typesafe.scalalogging.slf4j.LazyLogging

import scala.collection.JavaConversions._

object EntityContainer extends LazyLogging {

  val CONTAINER = collection.mutable.Map[String,EntityInfo]()

  case class EntityInfo(
                         clazz: Class[_],
                         tableName: String,
                         tableDesc: String,
                         seqIdAnnotation: SeqId,
                         fkFieldNames: List[String],
                         fieldDesc: Map[String, String],
                         allFields: Map[String, String],
                         persistentFields: Map[String, String],
                         indexFieldNames: List[String],
                         uniqueFieldNames: List[String],
                         textFieldNames: List[String],
                         ignoreFieldNames: List[String],
                         allAnnotations: List[FieldAnnotationInfo],
                         oneToManyFields: List[(OneToMany, String, Class[_])],
                         manyToManyFields: List[(ManyToMany, String, Class[_])]
                       )

  def autoBuilding(basePackage: String) = {
    ClassScanHelper.scan[Entity](basePackage).foreach {
      clazz =>
        buildingEntityInfo(clazz)
        initDBTable(clazz)
    }
  }

  def buildingEntityInfo(clazz: Class[_]): Unit = {
    val tableName = clazz.getSimpleName.toUpperCase
    val entityAnnotation = BeanHelper.getClassAnnotation[Entity](clazz).get
    val tableDesc = entityAnnotation.desc
    val seqIdAnnotation = BeanHelper.getClassAnnotation[SeqId](clazz).orNull
    val allAnnotations = BeanHelper.findFieldAnnotations(clazz).toList
    val fkFieldNames = allAnnotations.filter(_.annotation.isInstanceOf[FK]).map(_.fieldName)
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
    val textFieldNames = allAnnotations.filter(_.annotation.isInstanceOf[Text]).map {
      field =>
        field.fieldName
    }
    val allFields = BeanHelper.findFields(clazz, filterAnnotations = Seq()).map {
      item =>
        val ttype = item._1 match {
          case name if textFieldNames.contains(name) => "text"
          case name if Model.ID_FLAG == name && seqIdAnnotation != null => "seq" //使用序列ID
          case _ => item._2
        }
        item._1 -> ttype
    }
    val ignoreFieldNames = allFields.filter {
      field =>
        allAnnotations.filter(_.fieldName == field._1).exists {
          ann =>
            ann.annotation.getClass == classOf[Ignore] || ann.annotation.getClass == classOf[ManyToMany] || ann.annotation.getClass == classOf[OneToMany]
        }
    }.keys.toList
    val persistentFields = allFields.filter(field => !ignoreFieldNames.contains(field._1))
    val oneToManyFields = allAnnotations.filter(_.annotation.isInstanceOf[OneToMany]).map {
      field =>
        (field.annotation.asInstanceOf[OneToMany], field.fieldName, BeanHelper.getClassByStr(allFields(field.fieldName)))
    }
    val manyToManyFields = allAnnotations.filter(_.annotation.isInstanceOf[ManyToMany]).map {
      field =>
        (field.annotation.asInstanceOf[ManyToMany], field.fieldName, BeanHelper.getClassByStr(allFields(field.fieldName)))
    }

    CONTAINER += tableName -> EntityInfo(
      clazz,
      tableName,
      tableDesc,
      seqIdAnnotation,
      fkFieldNames,
      fieldDesc,
      allFields,
      persistentFields,
      indexFieldNames,
      uniqueFieldNames,
      textFieldNames,
      ignoreFieldNames,
      allAnnotations,
      oneToManyFields,
      manyToManyFields
    )

    logger.info( """Create model: %s""".format(clazz.getSimpleName))
  }

  def initDBTable(clazz: Class[_]): Unit = {
    val tableName = clazz.getSimpleName.toUpperCase
    val entityInfo = CONTAINER(tableName)
    JDBCStorable.db.createTableIfNotExist(
      clazz.getSimpleName, entityInfo.tableDesc, entityInfo.persistentFields, entityInfo.fieldDesc, entityInfo.indexFieldNames, entityInfo.uniqueFieldNames, Model.ID_FLAG
    )
    initManyToManyRel(entityInfo)
  }

  private def initManyToManyRel(entityInfo: EntityInfo): Unit = {
    entityInfo.manyToManyFields.foreach {
      ann =>
        val (masterFieldName, relFieldName) = getManyToManyRelTableFields(entityInfo, ann._1.mapping, ann._1.master)
        JDBCStorable.db.createTableIfNotExist(
          getManyToManyRelTableName(entityInfo, ann._1.mapping, ann._1.master), "",
          Map[String, String](
            masterFieldName -> "String",
            relFieldName -> "String"
          ), null, null, null, null)
    }
  }

  private def getManyToManyRelTableFields(entityInfo: EntityInfo, tableName: String, isMaster: Boolean): (String, String) = {
    if (isMaster) {
      (entityInfo.tableName + "_" + Model.ID_FLAG, tableName + "_" + Model.ID_FLAG)
    } else {
      (tableName + "_" + Model.ID_FLAG, entityInfo.tableName + "_" + Model.ID_FLAG)
    }
  }

  private def getManyToManyRelTableName(entityInfo: EntityInfo, tableName: String, isMaster: Boolean): String = {
    if (isMaster) {
      Model.REL_FLAG + "_" + entityInfo.tableName + "_" + tableName
    } else {
      Model.REL_FLAG + "_" + tableName + "_" + entityInfo.tableName
    }
  }
}
