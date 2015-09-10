package com.ecfront.ez.framework.storage

import java.lang.reflect.ParameterizedType
import java.util.UUID

import com.ecfront.common.BeanHelper
import com.typesafe.scalalogging.slf4j.LazyLogging

/**
 * 存储接口
 * @tparam M 要操作的实体对象
 * @tparam Q 请求附加对象，如可在此对象中加入请求的用户名、角色，重写_appendAuth方法实现权限控制
 */
trait Storable[M <: AnyRef, Q <: AnyRef] extends LazyLogging {

  protected val __modelClazz = this.getClass.getGenericInterfaces()(0).asInstanceOf[ParameterizedType].getActualTypeArguments()(0).asInstanceOf[Class[M]]
  protected val __tableName = __modelClazz.getSimpleName.toUpperCase
  protected val __entityInfo = if (EntityContainer.CONTAINER.contains(__tableName)) {
    EntityContainer.CONTAINER(__tableName)
  } else {
    EntityContainer.buildingEntityInfo(__modelClazz)
    EntityContainer.initDBTable(__modelClazz)
    EntityContainer.CONTAINER(__tableName)
  }

  protected def __init(modelClazz: Class[M]): Unit = {
    __customInit(modelClazz)
  }

  __init(__modelClazz)

  protected def __customInit(modelClazz: Class[M]): Unit = {}

  protected def __getMapValue(model: M): Map[String, Any] = {
    //获取对象要持久化字段的值，忽略为null的id字段（由seq控制）
    BeanHelper.findValues(model, __entityInfo.ignoreFieldNames)
      .filterNot(item => item._1 == Model.ID_FLAG && (item._2 == null || item._2.toString.trim == ""))
      .map {
        item =>
          if (__entityInfo.seqIdAnnotation != null && (item._1 == Model.ID_FLAG || __entityInfo.fkFieldNames.contains(item._1))) {
            //seq id时将id 转换成 long
            val value = item._2.asInstanceOf[String]
            (item._1, if (value == "") null else value.toLong)
          } else {
            (item._1, item._2)
          }
      }
  }

  def __getById(id: String, request: Option[Q] = None): Option[M] = {
    __doGetById(id, request)
  }

  protected def __doGetById(id: String, request: Option[Q]): Option[M]

  def __getByCondition(condition: String, parameters: Option[List[Any]] = None, request: Option[Q] = None): Option[M] = {
    __doGetByCondition(condition, parameters, request)
  }

  protected def __doGetByCondition(condition: String, parameters: Option[List[Any]], request: Option[Q]): Option[M]

  def __findAll(request: Option[Q] = None): Option[List[M]] = {
    __doFindAll(request)
  }

  protected def __doFindAll(request: Option[Q]): Option[List[M]]

  def __findAllEnable(request: Option[Q] = None): Option[List[M]] = {
     __doFindAllEnable(request)
  }

  protected def __doFindAllEnable(request: Option[Q]): Option[List[M]]

  def __findAllDisable(request: Option[Q] = None): Option[List[M]] = {
    __doFindAllDisable(request)
  }

  protected def __doFindAllDisable(request: Option[Q]): Option[List[M]]


  def __findByCondition(condition: String, parameters: Option[List[Any]] = None, request: Option[Q] = None): Option[List[M]] = {
    __doFindByCondition(condition, parameters, request)
  }

  protected def __doFindByCondition(condition: String, parameters: Option[List[Any]], request: Option[Q]): Option[List[M]]

  def __pageAll(pageNumber: Long = 1, pageSize: Long = 10, request: Option[Q] = None): Option[PageModel[M]] = {
    __doPageAll(pageNumber, pageSize, request)
  }

  protected def __doPageAll(pageNumber: Long, pageSize: Long, request: Option[Q]): Option[PageModel[M]]

  def __pageAllEnable(pageNumber: Long = 1, pageSize: Long = 10, request: Option[Q] = None): Option[PageModel[M]] = {
    __doPageAllEnable(pageNumber, pageSize, request)
  }

  protected def __doPageAllEnable(pageNumber: Long, pageSize: Long, request: Option[Q]): Option[PageModel[M]]

  def __pageAllDisable(pageNumber: Long = 1, pageSize: Long = 10, request: Option[Q] = None): Option[PageModel[M]] = {
    __doPageAllDisable(pageNumber, pageSize, request)
  }

  protected def __doPageAllDisable(pageNumber: Long, pageSize: Long, request: Option[Q]): Option[PageModel[M]]

  def __pageByCondition(condition: String, parameters: Option[List[Any]] = None, pageNumber: Long = 1, pageSize: Long = 10, request: Option[Q] = None): Option[PageModel[M]] = {
    __doPageByCondition(condition, parameters, pageNumber, pageSize, request)
  }

  protected def __doPageByCondition(condition: String, parameters: Option[List[Any]], pageNumber: Long, pageSize: Long, request: Option[Q]): Option[PageModel[M]]

  def __save(model: M, request: Option[Q] = None): Option[String] = {
    __doSave(model, request)
  }

  protected def __doSave(model: M, request: Option[Q]): Option[String]

  def __saveWithoutTransaction(model: M, request: Option[Q] = None): Option[String] = {
    if (__entityInfo.seqIdAnnotation == null) {
      val idValue = __getIdValue(model)
      if (idValue == null || idValue.isEmpty) {
        __setValueByField(model, Model.ID_FLAG, UUID.randomUUID().toString)
      }
    }
    __doSaveWithoutTransaction(model, request)
  }

  protected def __doSaveWithoutTransaction(model: M, request: Option[Q]): Option[String]

  def __update(id: String, model: M, request: Option[Q] = None): Option[String] = {
    __doUpdate(id, model, request)
  }

  protected def __doUpdate(id: String, model: M, request: Option[Q]): Option[String]

  def __updateWithoutTransaction(id: String, model: M, request: Option[Q] = None): Option[String] = {
    val savedModel = __doGetById(id, request).get
    BeanHelper.copyProperties(savedModel, model)
    __doUpdateWithoutTransaction(id, savedModel, request)
  }

  protected def __doUpdateWithoutTransaction(id: String, model: M, request: Option[Q]): Option[String]

  def __deleteById(id: String, request: Option[Q] = None): Option[String] = {
    __doDeleteById(id, request)
  }

  protected def __doDeleteById(id: String, request: Option[Q]): Option[String]

  def __deleteByIdWithoutTransaction(id: String, request: Option[Q] = None): Option[String] = {
    __doDeleteByIdWithoutTransaction(id, request)
  }

  protected def __doDeleteByIdWithoutTransaction(id: String, request: Option[Q]): Option[String]

  def __deleteByCondition(condition: String, parameters: Option[List[Any]] = None, request: Option[Q] = None): Option[List[String]] = {
    __doDeleteByCondition(condition, parameters, request)
  }

  protected def __doDeleteByCondition(condition: String, parameters: Option[List[Any]], request: Option[Q]): Option[List[String]]

  def __deleteAllWithoutTransaction(request: Option[Q] = None): Option[List[String]] = {
    __doDeleteAllWithoutTransaction(request)
  }

  protected def __doDeleteAllWithoutTransaction(request: Option[Q]): Option[List[String]]

  def __deleteAll(request: Option[Q] = None): Option[List[String]] = {
    __doDeleteAll(request)
  }

  protected def __doDeleteAll(request: Option[Q]): Option[List[String]]

  def __deleteByConditionWithoutTransaction(condition: String, parameters: Option[List[Any]] = None, request: Option[Q] = None): Option[List[String]] = {
    __doDeleteByConditionWithoutTransaction(condition, parameters, request)
  }

  protected def __doDeleteByConditionWithoutTransaction(condition: String, parameters: Option[List[Any]], request: Option[Q]): Option[List[String]]

  protected def __appendAuth(request: Option[Q]): (String, List[Any])

  protected def __getIdValue(model: AnyRef): String = {
    val idValue = __getValueByField(model, Model.ID_FLAG)
    if (idValue == null) null else idValue.asInstanceOf[String].trim
  }

  protected def __getValueByField(model: AnyRef, fieldName: String): Any = {
    BeanHelper.getValue(model, fieldName).orNull
  }

  protected def __setValueByField(model: AnyRef, fieldName: String, value: Any): Unit = {
    BeanHelper.setValue(model, fieldName, value)
  }

  protected def __convertIdToDB(id: String): Any = {
    if (__entityInfo.seqIdAnnotation != null && id != null) {
      id.toLong
    } else {
      id
    }
  }

}



