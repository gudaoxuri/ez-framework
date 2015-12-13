package com.asto.ez.framework.scaffold

import java.lang.reflect.ParameterizedType

import com.asto.ez.framework.EZContext
import com.asto.ez.framework.rpc.{DELETE, GET, POST, PUT}
import com.asto.ez.framework.storage.jdbc.JDBCIdModel
import com.asto.ez.framework.storage.mongo.MongoBaseModel
import com.asto.ez.framework.storage.{BaseModel, Page, StatusModel}
import com.ecfront.common.{AsyncResp, Resp}
import com.typesafe.scalalogging.slf4j.LazyLogging

import scala.async.Async._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}

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
    save(parameter, body, context).onSuccess {
      case resp => p.resp(resp)
    }
  }

  protected def preSave(model: M, context: EZContext): Future[Resp[M]] = Future(Resp.success(model))

  protected def postSave(model: M, savedResult: String, context: EZContext): Future[Resp[String]] = Future(Resp.success(savedResult))

  def save(parameter: Map[String, String], body: M, context: EZContext): Future[Resp[String]] = {
    val p = Promise[Resp[String]]()
    if (_successful) {
      logger.trace(s" RPC simple save : $body")
      preSave(body, context).onSuccess {
        case preResp =>
          if (preResp) {
            preResp.body.save().onSuccess {
              case saveResp =>
                if (saveResp) {
                  postSave(preResp.body, saveResp.body, context).onSuccess {
                    case postResp =>
                      p.success(postResp)
                  }
                } else {
                  p.success(saveResp)
                }
            }
          } else {
            p.success(preResp)
          }
      }
    } else {
      p.success(Resp.notImplemented(_errorMsg))
    }
    p.future
  }

  @PUT(":id/")
  protected def _rpc_update(parameter: Map[String, String], body: M, p: AsyncResp[String], context: EZContext): Unit = {
    update(parameter, body, context).onSuccess {
      case resp => p.resp(resp)
    }
  }

  protected def preUpdate(model: M, context: EZContext): Future[Resp[M]] = Future(Resp.success(model))

  protected def postUpdate(model: M, updatedResult: String, context: EZContext): Future[Resp[String]] = Future(Resp.success(updatedResult))

  def update(parameter: Map[String, String], body: M, context: EZContext): Future[Resp[String]] = {
    val p = Promise[Resp[String]]()
    if (!parameter.contains("id")) {
      p.success(Resp.badRequest("【id】不能为空"))
    } else {
      if (_successful) {
        val id = parameter("id")
        body match {
          case b: JDBCIdModel =>
            b.id = id
          case b: MongoBaseModel =>
            b.id = id
        }
        logger.trace(s" RPC simple update : $parameter -> $body")
        preUpdate(body, context).onSuccess {
          case preResp =>
            if (preResp) {
              preResp.body.update(context).onSuccess {
                case updateResp =>
                  if (updateResp) {
                    postUpdate(preResp.body, updateResp.body, context).onSuccess {
                      case postResp =>
                        p.success(postResp)
                    }
                  } else {
                    p.success(updateResp)
                  }
              }
            } else {
              p.success(preResp)
            }
        }
      } else {
        p.success(Resp.notImplemented(_errorMsg))
      }
    }
    p.future
  }

  @GET("")
  protected def _rpc_find(parameter: Map[String, String], p: AsyncResp[List[M]], context: EZContext): Unit = {
    find(parameter, context).onSuccess {
      case resp => p.resp(resp)
    }
  }

  protected def preFind(parameter: Map[String, String], context: EZContext): Future[Resp[Map[String, String]]] = Future(Resp.success(parameter))

  protected def postFind(parameter: Map[String, String], foundResult: List[M], context: EZContext): Future[Resp[List[M]]] = Future(Resp.success(foundResult))

  def find(parameter: Map[String, String], context: EZContext): Future[Resp[List[M]]] = {
    val p = Promise[Resp[List[M]]]()
    if (_successful) {
      logger.trace(s" RPC simple find : $parameter")
      preFind(parameter, context).onSuccess {
        case preResp =>
          if (preResp) {
            val condition = if (preResp.body.contains("condition")) preResp.body("condition") else _emptyCondition
            modelObj.find(condition, List(), context).onSuccess {
              case findResp =>
                if (findResp) {
                  postFind(preResp.body, findResp.body, context).onSuccess {
                    case postResp =>
                      p.success(postResp)
                  }
                } else {
                  p.success(findResp)
                }
            }
          } else {
            p.success(preResp)
          }
      }
    } else {
      p.success(Resp.notImplemented(_errorMsg))
    }
    p.future
  }

  @GET("page/:pageNumber/:pageSize/")
  protected def _rpc_page(parameter: Map[String, String], p: AsyncResp[Page[M]], context: EZContext): Unit = {
    page(parameter, context).onSuccess {
      case resp => p.resp(resp)
    }
  }

  protected def prePage(parameter: Map[String, String], context: EZContext): Future[Resp[Map[String, String]]] = Future(Resp.success(parameter))

  protected def postPage(parameter: Map[String, String], pagedResult: Page[M], context: EZContext): Future[Resp[Page[M]]] = Future(Resp.success(pagedResult))

  def page(parameter: Map[String, String], context: EZContext): Future[Resp[Page[M]]] = {
    val p = Promise[Resp[Page[M]]]()
    if (_successful) {
      logger.trace(s" RPC simple page : $parameter")
      prePage(parameter, context).onSuccess {
        case preResp =>
          if (preResp) {
            val condition = if (preResp.body.contains("condition")) preResp.body("condition") else _emptyCondition
            val pageNumber = if (preResp.body.contains("pageNumber")) preResp.body("pageNumber").toLong else 1L
            val pageSize = if (preResp.body.contains("pageSize")) preResp.body("pageSize").toInt else 10
            modelObj.page(condition, List(), pageNumber, pageSize, context).onSuccess {
              case pageResp =>
                if (pageResp) {
                  postPage(preResp.body, pageResp.body.asInstanceOf[Page[M]], context).onSuccess {
                    case postResp =>
                      p.success(postResp)
                  }
                } else {
                  p.success(pageResp)
                }
            }
          } else {
            p.success(preResp)
          }
      }
    } else {
      p.success(Resp.notImplemented(_errorMsg))
    }
    p.future
  }

  @GET(":id/")
  protected def _rpc_get(parameter: Map[String, String], p: AsyncResp[M], context: EZContext): Unit = {
    get(parameter, context).onSuccess {
      case resp => p.resp(resp)
    }
  }

  protected def preGet(parameter: Map[String, String], context: EZContext): Future[Resp[Map[String, String]]] = Future(Resp.success(parameter))

  protected def postGet(parameter: Map[String, String], getResult: M, context: EZContext): Future[Resp[M]] = Future(Resp.success(getResult))

  def get(parameter: Map[String, String], context: EZContext): Future[Resp[M]] = {
    val p = Promise[Resp[M]]()
    if (!parameter.contains("id")) {
      p.success(Resp.badRequest("【id】不能为空"))
    } else {
      if (_successful) {
        val id = parameter("id")
        logger.trace(s" RPC simple get : $parameter")
        preGet(parameter, context).onSuccess {
          case preResp =>
            if (preResp) {
              modelObj.getById(id, context).onSuccess {
                case getResp =>
                  if (getResp) {
                    postGet(preResp.body, getResp.body, context).onSuccess {
                      case postResp =>
                        p.success(postResp)
                    }
                  } else {
                    p.success(getResp)
                  }
              }
            } else {
              p.success(preResp)
            }
        }
      } else {
        p.success(Resp.notImplemented(_errorMsg))
      }
    }
    p.future
  }

  @DELETE(":id/")
  protected def _rpc_delete(parameter: Map[String, String], p: AsyncResp[Void], context: EZContext): Unit = {
    delete(parameter, context).onSuccess {
      case resp => p.resp(resp)
    }
  }

  protected def preDelete(parameter: Map[String, String], context: EZContext): Future[Resp[Map[String, String]]] = Future(Resp.success(parameter))

  protected def postDelete(parameter: Map[String, String], context: EZContext): Future[Resp[Void]] = Future(Resp.success(null))

  def delete(parameter: Map[String, String], context: EZContext): Future[Resp[Void]] = {
    val p = Promise[Resp[Void]]()
    if (!parameter.contains("id")) {
      p.success(Resp.badRequest("【id】不能为空"))
    } else {
      if (_successful) {
        val id = parameter("id")
        logger.trace(s" RPC simple delete : $parameter")
        preDelete(parameter, context).onSuccess {
          case preResp =>
            if (preResp) {
              modelObj.deleteById(id, context).onSuccess {
                case deleteResp =>
                  if (deleteResp) {
                    postDelete(preResp.body, context).onSuccess {
                      case postResp =>
                        p.success(postResp)
                    }
                  } else {
                    p.success(deleteResp)
                  }
              }
            } else {
              p.success(preResp)
            }
        }
      } else {
        p.success(Resp.notImplemented(_errorMsg))
      }
    }
    p.future
  }

  @GET(":id/enable/")
  protected def _rpc_enable(parameter: Map[String, String], p: AsyncResp[Void], context: EZContext): Unit = {
    enable(parameter, context).onSuccess {
      case resp => p.resp(resp)
    }
  }

  protected def preEnable(parameter: Map[String, String], context: EZContext): Future[Resp[Map[String, String]]] = Future(Resp.success(parameter))

  protected def postEnable(parameter: Map[String, String], context: EZContext): Future[Resp[Void]] = Future(Resp.success(null))

  def enable(parameter: Map[String, String], context: EZContext): Future[Resp[Void]] = {
    val p = Promise[Resp[Void]]()
    if (!parameter.contains("id")) {
      p.success(Resp.badRequest("【id】不能为空"))
    } else {
      if (_successful) {
        val id = parameter("id")
        logger.trace(s" RPC simple enable : $parameter")
        preEnable(parameter, context).onSuccess {
          case preResp =>
            if (preResp) {
              modelObj.asInstanceOf[StatusModel].enableById(id, context).onSuccess {
                case enableResp =>
                  if (enableResp) {
                    postEnable(preResp.body, context).onSuccess {
                      case postResp =>
                        p.success(postResp)
                    }
                  } else {
                    p.success(enableResp)
                  }
              }
            } else {
              p.success(preResp)
            }
        }
      } else {
        p.success(Resp.notImplemented(_errorMsg))
      }
    }
    p.future
  }

  @GET(":id/disable/")
  protected def _rpc_disable(parameter: Map[String, String], p: AsyncResp[Void], context: EZContext): Unit = {
    disable(parameter, context).onSuccess {
      case resp => p.resp(resp)
    }
  }

  protected def preDisable(parameter: Map[String, String], context: EZContext): Future[Resp[Map[String, String]]] = Future(Resp.success(parameter))

  protected def postDisable(parameter: Map[String, String], context: EZContext): Future[Resp[Void]] = Future(Resp.success(null))

  def disable(parameter: Map[String, String], context: EZContext): Future[Resp[Void]] = {
    val p = Promise[Resp[Void]]()
    if (!parameter.contains("id")) {
      p.success(Resp.badRequest("【id】不能为空"))
    } else {
      if (_successful) {
        val id = parameter("id")
        logger.trace(s" RPC simple disable : $parameter")
        preDisable(parameter, context).onSuccess {
          case preResp =>
            if (preResp) {
              modelObj.asInstanceOf[StatusModel].enableById(id, context).onSuccess {
                case disableResp =>
                  if (disableResp) {
                    postEnable(preResp.body, context).onSuccess {
                      case postResp =>
                        p.success(postResp)
                    }
                  } else {
                    p.success(disableResp)
                  }
              }
            } else {
              p.success(preResp)
            }
        }
      } else {
        p.success(Resp.notImplemented(_errorMsg))
      }
    }
    p.future
  }

  @POST("upload/")
  def _rpc_upload(parameter: Map[String, String], p: AsyncResp[String], context: EZContext): Unit = async {
    //TODO
    p.success("")
  }

}
