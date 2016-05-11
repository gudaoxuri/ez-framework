package com.ecfront.ez.framework.service.rpc.foundation.scaffold

import java.lang.reflect.ParameterizedType
import java.util.regex.Pattern

import com.ecfront.common.{JsonHelper, Resp}
import com.ecfront.ez.framework.service.rpc.foundation._
import com.ecfront.ez.framework.service.storage.foundation._
import com.typesafe.scalalogging.slf4j.LazyLogging

/**
  * 简单RPC服务
  *
  * @tparam M 对应的实体类型
  * @tparam C 对应的RPC上下文
  */
trait SimpleRPCService[M <: BaseModel, C <: EZRPCContext] extends LazyLogging {

  // 持久化对象
  protected val storageObj: BaseStorage[M]

  protected val modelClazz = this.getClass.getGenericInterfaces()(0).asInstanceOf[ParameterizedType].getActualTypeArguments()(0).asInstanceOf[Class[M]]

  protected val modelObj = modelClazz.newInstance()

  val DEFAULT_PAGE_SIZE: Int = 10

  /**
    * 保存操作
    *
    * @param parameter 请求参数
    * @param body      原始（字符串类型）请求体
    * @param context   PRC上下文
    * @return 保存结果
    */
  @POST("")
  def rpcSave(parameter: Map[String, String], body: String, context: C): Resp[M] = {
    logger.trace(s" RPC simple save : $body")
    val model = JsonHelper.toObject(body, modelClazz)
    storageObj.save(model, context.toStorageContext)
  }

  /**
    * 更新操作
    *
    * @param parameter 请求参数，必须包含`id`
    * @param body      原始（字符串类型）请求体
    * @param context   PRC上下文
    * @return 更新结果
    */
  @PUT(":id/")
  def rpcUpdate(parameter: Map[String, String], body: String, context: C): Resp[M] = {
    if (!parameter.contains("id")) {
      Resp.badRequest("【id】not null")
    } else {
      logger.trace(s" RPC simple update : $body")
      val model = JsonHelper.toObject(body, modelClazz)
      model.id = parameter("id")
      storageObj.update(model, context.toStorageContext)
    }
  }

  /**
    * 查找启用记录操作，对应的实体必须继承[[StatusModel]]
    *
    * @param parameter 请求参数，可以包含`condition` 用于筛选条件
    * @param context   PRC上下文
    * @return 查找到的结果
    */
  @GET("enable/")
  def rpcFindEnable(parameter: Map[String, String], context: C): Resp[List[M]] = {
    logger.trace(s" RPC simple find enable : $parameter")
    if (classOf[StatusModel].isAssignableFrom(modelClazz)) {
      val conditionR = if (parameter.contains("condition")) conditionCheck(parameter("condition")) else Resp.success("")
      if (conditionR) {
        storageObj.asInstanceOf[StatusStorage[_]].findEnabled(conditionR.body, List(), context.toStorageContext)
      } else {
        conditionR
      }
    } else {
      Resp.notImplemented("")
    }
  }

  /**
    * 查找操作
    *
    * @param parameter 请求参数，可以包含`condition` 用于筛选条件
    * @param context   PRC上下文
    * @return 查找到的结果
    */
  @GET("")
  def rpcFind(parameter: Map[String, String], context: C): Resp[List[M]] = {
    logger.trace(s" RPC simple find : $parameter")
    val conditionR = if (parameter.contains("condition")) conditionCheck(parameter("condition")) else Resp.success("")
    if (conditionR) {
      storageObj.find(conditionR.body, List(), context.toStorageContext)
    } else {
      conditionR
    }
  }

  /**
    * 分页操作
    *
    * @param parameter 请求参数，可以包含
    *                  `condition` 用于筛选条件，
    *                  `pageNumber` 用于设定当前页码，页码从1开始，
    *                  `pageSize` 用于设定每页显示记录数
    * @param context   PRC上下文
    * @return 查找到的结果
    */
  @GET("page/:pageNumber/:pageSize/")
  def rpcPage(parameter: Map[String, String], context: C): Resp[Page[M]] = {
    logger.trace(s" RPC simple page : $parameter")
    val pageNumber = if (parameter.contains("pageNumber")) parameter("pageNumber").toLong else 1L
    val pageSize = if (parameter.contains("pageSize")) parameter("pageSize").toInt else DEFAULT_PAGE_SIZE
    val conditionR = if (parameter.contains("condition")) conditionCheck(parameter("condition")) else Resp.success("")
    if (conditionR) {
      storageObj.page(conditionR.body, List(), pageNumber, pageSize, context.toStorageContext)
    } else {
      conditionR
    }
  }

  /**
    * 获取单条记录操作
    *
    * @param parameter 请求参数，必须包含`id`
    * @param context   PRC上下文
    * @return 获取的结果
    */
  @GET(":id/")
  def rpcGet(parameter: Map[String, String], context: C): Resp[M] = {
    if (!parameter.contains("id")) {
      Resp.badRequest("【id】not null")
    } else {
      val id = parameter("id")
      logger.trace(s" RPC simple get : $id")
      storageObj.getById(id, context.toStorageContext)
    }
  }

  /**
    * 删除单条记录操作
    *
    * @param parameter 请求参数，必须包含`id`
    * @param context   PRC上下文
    * @return 删除的结果
    */
  @DELETE(":id/")
  def rpcDelete(parameter: Map[String, String], context: C): Resp[Void] = {
    if (!parameter.contains("id")) {
      Resp.badRequest("【id】not null")
    } else {
      val id = parameter("id")
      logger.trace(s" RPC simple delete : $id")
      storageObj.deleteById(id, context.toStorageContext)
    }
  }

  /**
    * 启用单条记录操作
    *
    * @param parameter 请求参数，必须包含`id`，对应的实体必须继承[[StatusModel]]
    * @param context   PRC上下文
    * @return 启用的结果
    */
  @GET(":id/enable/")
  def rpcEnable(parameter: Map[String, String], context: C): Resp[Void] = {
    if (!parameter.contains("id")) {
      Resp.badRequest("【id】not null")
    } else {
      if (classOf[StatusModel].isAssignableFrom(modelClazz)) {
        val id = parameter("id")
        logger.trace(s" RPC simple enable : $id")
        storageObj.asInstanceOf[StatusStorage[_]].enableById(id, context.toStorageContext)
      } else {
        Resp.notImplemented("")
      }
    }
  }

  /**
    * 禁用单条记录操作
    *
    * @param parameter 请求参数，必须包含`id`，对应的实体必须继承[[StatusModel]]
    * @param context   PRC上下文
    * @return 禁用的结果
    */
  @GET(":id/disable/")
  def rpcDisable(parameter: Map[String, String], context: C): Resp[Void] = {
    if (!parameter.contains("id")) {
      Resp.badRequest("【id】not null")
    } else {
      if (classOf[StatusModel].isAssignableFrom(modelClazz)) {
        val id = parameter("id")
        logger.trace(s" RPC simple enable : $id")
        storageObj.asInstanceOf[StatusStorage[_]].disableById(id, context.toStorageContext)
      } else {
        Resp.notImplemented("")
      }
    }
  }

  private val reg = "(?:;)|(?:--)|(/\\*(?:.|[\\n\\r])*?\\*/)|(\\b(select|update|or|delete|insert|trancate|char|into|substr|ascii|declare|exec|count|master|into|drop|execute)\\b)"
  private val sqlPattern = Pattern.compile(reg, Pattern.CASE_INSENSITIVE)

  def conditionCheck(condition: String): Resp[String] = {
    if (sqlPattern.matcher(condition).find()) {
      Resp.badRequest("condition illegal")
    } else {
      Resp.success(condition)
    }
  }

}
