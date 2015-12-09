package com.asto.ez.framework.scaffold

import java.lang.reflect.ParameterizedType

import com.asto.ez.framework.EZContext
import com.asto.ez.framework.rpc.{DELETE, GET, POST, PUT}
import com.asto.ez.framework.storage.jdbc.JDBCIdModel
import com.asto.ez.framework.storage.mongo.MongoBaseModel
import com.asto.ez.framework.storage.{BaseModel, Page, StatusModel}
import com.ecfront.common.{AsyncResp, Resp}
import com.typesafe.scalalogging.slf4j.LazyLogging
import scala.concurrent.ExecutionContext.Implicits.global
import scala.async.Async._
import scala.concurrent.Future

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


  protected def preSave(model: M, context: EZContext): Future[Resp[M]] = Future(Resp.success(model))

  protected def postSave(model: M, savedResult: String, context: EZContext): Future[Resp[String]] = Future(Resp.success(savedResult))

  @POST("")
  def _rpc_save(parameter: Map[String, String], body: M, p: AsyncResp[String], context: EZContext): Unit = {
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
                      if (postResp) {
                        p.success(postResp.body)
                      } else {
                        p.resp(postResp)
                      }
                  }
                } else {
                  p.resp(saveResp)
                }
            }
          } else {
            p.resp(preResp)
          }
      }
    } else {
      p.notImplemented(_errorMsg)
    }
  }

  protected def preUpdate(model: M, context: EZContext): Future[Resp[M]] = Future(Resp.success(model))

  protected def postUpdate(model: M, updatedResult: String, context: EZContext): Future[Resp[String]] = Future(Resp.success(updatedResult))

  @PUT(":id/")
  def _rpc_update(parameter: Map[String, String], body: M, p: AsyncResp[String], context: EZContext): Unit = {
    if (!parameter.contains("id")) {
      p.badRequest("【id】不能为空")
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
                        if (postResp) {
                          p.success(postResp.body)
                        } else {
                          p.resp(postResp)
                        }
                    }
                  } else {
                    p.resp(updateResp)
                  }
              }
            } else {
              p.resp(preResp)
            }
        }
      } else {
        p.notImplemented(_errorMsg)
      }
    }
  }

  protected def preFind(parameter: Map[String, String], context: EZContext): Future[Resp[Map[String, String]]] = Future(Resp.success(parameter))

  protected def postFind(parameter: Map[String, String], foundResult: List[M], context: EZContext): Future[Resp[List[M]]] = Future(Resp.success(foundResult))

  @GET("")
  def _rpc_find(parameter: Map[String, String], p: AsyncResp[List[M]], context: EZContext): Unit = async {
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
                      if (postResp) {
                        p.success(postResp.body)
                      } else {
                        p.resp(postResp)
                      }
                  }
                } else {
                  p.resp(findResp)
                }
            }
          } else {
            p.resp(preResp)
          }
      }
    } else {
      p.notImplemented(_errorMsg)
    }
  }

  protected def prePage(parameter: Map[String, String], context: EZContext): Future[Resp[Map[String, String]]] = Future(Resp.success(parameter))

  protected def postPage(parameter: Map[String, String], pagedResult: Page[M], context: EZContext): Future[Resp[Page[M]]] = Future(Resp.success(pagedResult))


  @GET("page/:pageNumber/:pageSize/")
  def _rpc_page(parameter: Map[String, String], p: AsyncResp[Page[M]], context: EZContext): Unit = async {
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
                      if (postResp) {
                        p.success(postResp.body)
                      } else {
                        p.resp(postResp)
                      }
                  }
                } else {
                  p.resp(pageResp)
                }
            }
          } else {
            p.resp(preResp)
          }
      }
    } else {
      p.notImplemented(_errorMsg)
    }
  }

  protected def preGet(parameter: Map[String, String], context: EZContext): Future[Resp[Map[String, String]]] = Future(Resp.success(parameter))

  protected def postGet(parameter: Map[String, String], getResult: M, context: EZContext): Future[Resp[M]] = Future(Resp.success(getResult))

  @GET(":id/")
  def _rpc_get(parameter: Map[String, String], p: AsyncResp[M], context: EZContext): Unit = async {
    if (!parameter.contains("id")) {
      p.badRequest("【id】不能为空")
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
                        if (postResp) {
                          p.success(postResp.body)
                        } else {
                          p.resp(postResp)
                        }
                    }
                  } else {
                    p.resp(getResp)
                  }
              }
            } else {
              p.resp(preResp)
            }
        }
      } else {
        p.notImplemented(_errorMsg)
      }
    }
  }

  protected def preDelete(parameter: Map[String, String], context: EZContext): Future[Resp[Map[String, String]]] = Future(Resp.success(parameter))

  protected def postDelete(parameter: Map[String, String], context: EZContext): Future[Resp[Void]] = Future(Resp.success(null))

  @DELETE(":id/")
  def _rpc_delete(parameter: Map[String, String], p: AsyncResp[Void], context: EZContext): Unit = async {
    if (!parameter.contains("id")) {
      p.badRequest("【id】不能为空")
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
                        if (postResp) {
                          p.success(postResp.body)
                        } else {
                          p.resp(postResp)
                        }
                    }
                  } else {
                    p.resp(deleteResp)
                  }
              }
            } else {
              p.resp(preResp)
            }
        }
      } else {
        p.notImplemented(_errorMsg)
      }
    }
  }

  protected def preEnable(parameter: Map[String, String], context: EZContext): Future[Resp[Map[String, String]]] = Future(Resp.success(parameter))

  protected def postEnable(parameter: Map[String, String], context: EZContext): Future[Resp[Void]] = Future(Resp.success(null))

  @GET(":id/enable/")
  def _rpc_enable(parameter: Map[String, String], p: AsyncResp[Void], context: EZContext): Unit = async {
    if (!parameter.contains("id")) {
      p.badRequest("【id】不能为空")
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
                        if (postResp) {
                          p.success(postResp.body)
                        } else {
                          p.resp(postResp)
                        }
                    }
                  } else {
                    p.resp(enableResp)
                  }
              }
            } else {
              p.resp(preResp)
            }
        }
      } else {
        p.notImplemented(_errorMsg)
      }
    }
  }

  protected def preDisable(parameter: Map[String, String], context: EZContext): Future[Resp[Map[String, String]]] = Future(Resp.success(parameter))

  protected def postDisable(parameter: Map[String, String], context: EZContext): Future[Resp[Void]] = Future(Resp.success(null))

  @GET(":id/disable/")
  def _rpc_disable(parameter: Map[String, String], p: AsyncResp[Void], context: EZContext): Unit = async {
    if (!parameter.contains("id")) {
      p.badRequest("【id】不能为空")
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
                        if (postResp) {
                          p.success(postResp.body)
                        } else {
                          p.resp(postResp)
                        }
                    }
                  } else {
                    p.resp(disableResp)
                  }
              }
            } else {
              p.resp(preResp)
            }
        }
      } else {
        p.notImplemented(_errorMsg)
      }
    }
  }

  @POST("upload/")
  def _rpc_upload(parameter: Map[String, String], p: AsyncResp[String], context: EZContext): Unit = async {
    //TODO
    p.success("")
  }

}
