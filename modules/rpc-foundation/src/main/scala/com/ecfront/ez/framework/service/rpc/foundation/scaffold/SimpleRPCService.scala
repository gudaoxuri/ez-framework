package com.ecfront.ez.framework.service.rpc.foundation.scaffold

import java.lang.reflect.ParameterizedType

import com.ecfront.common.{JsonHelper, Resp}
import com.ecfront.ez.framework.service.rpc.foundation._
import com.ecfront.ez.framework.service.storage.foundation._
import com.typesafe.scalalogging.slf4j.LazyLogging

trait SimpleRPCService[M <: BaseModel,C <: EZRPCContext] extends LazyLogging {

  protected val storageObj: BaseStorage[M]

  protected val modelClazz = this.getClass.getGenericInterfaces()(0).asInstanceOf[ParameterizedType].getActualTypeArguments()(0).asInstanceOf[Class[M]]

  protected val modelObj = modelClazz.newInstance()

  val DEFAULT_PAGE_SIZE: Int = 10

  @POST("")
  def rpcSave(parameter: Map[String, String], body: String, context: C): Resp[M] = {
    logger.trace(s" RPC simple save : $body")
    val model = JsonHelper.toObject(body, modelClazz)
    storageObj.save(model, context)
  }

  @PUT(":id/")
  def rpcUpdate(parameter: Map[String, String], body: String, context: C): Resp[M] = {
    if (!parameter.contains("id")) {
      Resp.badRequest("【id】not null")
    } else {
      logger.trace(s" RPC simple update : $body")
      val model = JsonHelper.toObject(body, modelClazz)
      storageObj.update(model, context)
    }
  }

  @GET("enable/")
  def rpcFindEnable(parameter: Map[String, String], context: C): Resp[List[M]] = {
    logger.trace(s" RPC simple find enable : $parameter")
    if (classOf[StatusModel].isAssignableFrom(modelClazz)) {
      val condition = if (parameter.contains("condition")) parameter("condition") else ""
      storageObj.asInstanceOf[StatusStorage[_]].findEnabled(condition, List(), context)
    } else {
      Resp.notImplemented("")
    }
  }

  @GET("")
  def rpcFind(parameter: Map[String, String], context: C): Resp[List[M]] = {
    logger.trace(s" RPC simple find : $parameter")
    val condition = if (parameter.contains("condition")) parameter("condition") else ""
    storageObj.find(condition, List(), context)
  }

  @GET("page/:pageNumber/:pageSize/")
  def rpcPage(parameter: Map[String, String], context: C): Resp[Page[M]] = {
    logger.trace(s" RPC simple page : $parameter")
    val condition = if (parameter.contains("condition")) parameter("condition") else ""
    val pageNumber = if (parameter.contains("pageNumber")) parameter("pageNumber").toLong else 1L
    val pageSize = if (parameter.contains("pageSize")) parameter("pageSize").toInt else DEFAULT_PAGE_SIZE
    storageObj.page(condition, List(), pageNumber, pageSize, context)
  }

  @GET(":id/")
  def rpcGet(parameter: Map[String, String], context: C): Resp[M] = {
    if (!parameter.contains("id")) {
      Resp.badRequest("【id】not null")
    } else {
      val id = parameter("id")
      logger.trace(s" RPC simple get : $id")
      storageObj.getById(id, context)
    }
  }

  @DELETE(":id/")
  def rpcDelete(parameter: Map[String, String], context: C): Resp[Void] = {
    if (!parameter.contains("id")) {
      Resp.badRequest("【id】not null")
    } else {
      val id = parameter("id")
      logger.trace(s" RPC simple delete : $id")
      storageObj.deleteById(id, context)
    }
  }

  @GET(":id/enable/")
  def rpcEnable(parameter: Map[String, String], context: C): Resp[Void] = {
    if (!parameter.contains("id")) {
      Resp.badRequest("【id】not null")
    } else {
      if (classOf[StatusModel].isAssignableFrom(modelClazz)) {
        val id = parameter("id")
        logger.trace(s" RPC simple enable : $id")
        storageObj.asInstanceOf[StatusStorage[_]].enableById(id, context)
      } else {
        Resp.notImplemented("")
      }
    }
  }

  @GET(":id/disable/")
  def rpcDisable(parameter: Map[String, String], context: C): Resp[Void] = {
    if (!parameter.contains("id")) {
      Resp.badRequest("【id】not null")
    } else {
      if (classOf[StatusModel].isAssignableFrom(modelClazz)) {
        val id = parameter("id")
        logger.trace(s" RPC simple enable : $id")
        storageObj.asInstanceOf[StatusStorage[_]].disableById(id, context)
      } else {
        Resp.notImplemented("")
      }
    }
  }

}
