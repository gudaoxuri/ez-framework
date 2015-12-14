package com.asto.ez.framework.scaffold

import java.lang.reflect.ParameterizedType

import com.asto.ez.framework.EZContext
import com.asto.ez.framework.rpc.{DELETE, GET, POST, PUT}
import com.asto.ez.framework.storage.jdbc.JDBCIdModel
import com.asto.ez.framework.storage.mongo.MongoBaseModel
import com.asto.ez.framework.storage.{BaseModel, Page, StatusModel}
import com.ecfront.common.AsyncResp
import com.typesafe.scalalogging.slf4j.LazyLogging

import scala.concurrent.ExecutionContext.Implicits.global

trait SimpleRPCService[M <: BaseModel] extends LazyLogging {

  protected val _modelClazz = this.getClass.getGenericInterfaces()(0).asInstanceOf[ParameterizedType].getActualTypeArguments()(0).asInstanceOf[Class[M]]

  protected val _errorMsg = "RPC simple service initialized error: Model type must is JDBCIdModel or MongoBaseModel."
  protected val _successful =
    if (classOf[JDBCIdModel].isAssignableFrom(_modelClazz) && classOf[MongoBaseModel].isAssignableFrom(_modelClazz)) {
      logger.info("RPC simple service initialized.")
      true
    } else {
      logger.warn(_errorMsg)
      false
    }

  protected val _isJDBCModel = classOf[JDBCIdModel].isAssignableFrom(_modelClazz)
  protected val modelObj = _modelClazz.newInstance()
  protected val _emptyCondition = if (_isJDBCModel) "1=1" else "{}"

  @POST("")
  protected def _rpc_save(parameter: Map[String, String], body: M, p: AsyncResp[String], context: EZContext): Unit = {
    if (_successful) {
      logger.trace(s" RPC simple save : $body")
      body.save(context).onSuccess {
        case resp => p.resp(resp)
      }
    } else {
      p.notImplemented(_errorMsg)
    }
  }

  @PUT(":id/")
  protected def _rpc_update(parameter: Map[String, String], body: M, p: AsyncResp[String], context: EZContext): Unit = {
    if (_successful) {
      if (!parameter.contains("id")) {
        p.badRequest("【id】不能为空")
      } else {
        logger.trace(s" RPC simple update : $body")
        body.update(context).onSuccess {
          case resp => p.resp(resp)
        }
      }
    } else {
      p.notImplemented(_errorMsg)
    }
  }


  @GET("")
  protected def _rpc_find(parameter: Map[String, String], p: AsyncResp[List[M]], context: EZContext): Unit = {
    if (_successful) {
      logger.trace(s" RPC simple find : $parameter")
      val condition = if (parameter.contains("condition")) parameter("condition") else _emptyCondition
      modelObj.find(condition, List(), context).onSuccess {
        case resp => p.resp(resp)
      }
    } else {
      p.notImplemented(_errorMsg)
    }
  }


  @GET("page/:pageNumber/:pageSize/")
  protected def _rpc_page(parameter: Map[String, String], p: AsyncResp[Page[M]], context: EZContext): Unit = {
    if (_successful) {
      logger.trace(s" RPC simple page : $parameter")
      val condition = if (parameter.contains("condition")) parameter("condition") else _emptyCondition
      val pageNumber = if (parameter.contains("page_number")) parameter("page_number").toLong else 1L
      val pageSize = if (parameter.contains("page_size")) parameter("page_size").toInt else 10
      modelObj.page(condition, List(), pageNumber, pageSize, context).onSuccess {
        case resp => p.resp(resp)
      }
    } else {
      p.notImplemented(_errorMsg)
    }
  }

  @GET(":id/")
  protected def _rpc_get(parameter: Map[String, String], p: AsyncResp[M], context: EZContext): Unit = {
    if (_successful) {
      if (!parameter.contains("id")) {
        p.badRequest("【id】不能为空")
      } else {
        val id = parameter("id")
        logger.trace(s" RPC simple get : $id")
        modelObj.getById(id, context).onSuccess {
          case resp => p.resp(resp)
        }
      }
    } else {
      p.notImplemented(_errorMsg)
    }
  }

  @DELETE(":id/")
  protected def _rpc_delete(parameter: Map[String, String], p: AsyncResp[Void], context: EZContext): Unit = {
    if (_successful) {
      if (!parameter.contains("id")) {
        p.badRequest("【id】不能为空")
      } else {
        val id = parameter("id")
        logger.trace(s" RPC simple delete : $id")
        modelObj.deleteById(id, context).onSuccess {
          case resp => p.resp(resp)
        }
      }
    } else {
      p.notImplemented(_errorMsg)
    }
  }

  @GET(":id/enable/")
  protected def _rpc_enable(parameter: Map[String, String], p: AsyncResp[Void], context: EZContext): Unit = {
    if (_successful) {
      if (!parameter.contains("id")) {
        p.badRequest("【id】不能为空")
      } else {
        if (classOf[StatusModel].isAssignableFrom(_modelClazz)) {
          val id = parameter("id")
          logger.trace(s" RPC simple enable : $id")
          modelObj.asInstanceOf[StatusModel].enableById(id, context).onSuccess {
            case resp => p.resp(resp)
          }
        } else {
          p.notImplemented("启用方法未实现")
        }
      }
    } else {
      p.notImplemented(_errorMsg)
    }
  }


  @GET(":id/disable/")
  protected def _rpc_disable(parameter: Map[String, String], p: AsyncResp[Void], context: EZContext): Unit = {
    if (_successful) {
      if (!parameter.contains("id")) {
        p.badRequest("【id】不能为空")
      } else {
        if (classOf[StatusModel].isAssignableFrom(_modelClazz)) {
          val id = parameter("id")
          logger.trace(s" RPC simple enable : $id")
          modelObj.asInstanceOf[StatusModel].disableById(id, context).onSuccess {
            case resp => p.resp(resp)
          }
        } else {
          p.notImplemented("启用方法未实现")
        }
      }
    } else {
      p.notImplemented(_errorMsg)
    }
  }


  @POST("upload/")
  def _rpc_upload(parameter: Map[String, String], p: AsyncResp[String], context: EZContext): Unit = {
    //TODO
    p.success("")
  }

}
