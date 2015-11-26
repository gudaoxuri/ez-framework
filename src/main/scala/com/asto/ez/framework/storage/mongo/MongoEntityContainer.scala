package com.asto.ez.framework.storage.mongo

import com.ecfront.common.{BeanHelper, ClassScanHelper, FieldAnnotationInfo, Ignore}
import com.typesafe.scalalogging.slf4j.LazyLogging

import scala.beans.BeanProperty

object MongoEntityContainer extends LazyLogging {

  val CONTAINER = collection.mutable.Map[String, MongoEntityInfo]()

  case class MongoEntityInfo(
                         clazz: Class[_],
                         tableName: String,
                         persistentFields: List[String]
                       )


  def initEntity(clazz: Class[_], tableName: String): Unit = {
    buildingEntityInfo(clazz, tableName)
  }

  def buildingEntityInfo(clazz: Class[_], tableName: String): Unit = {
    val persistentFields = BeanHelper.findFieldAnnotations(clazz,Seq(classOf[Ignore])).map(_.fieldName).toSet -- Set("_modelClazz","_tableName")

    CONTAINER += tableName -> MongoEntityInfo(
      clazz,
      tableName,
      persistentFields.toList
    )
    logger.info( """Create model: %s""".format(clazz.getSimpleName))
  }


}
