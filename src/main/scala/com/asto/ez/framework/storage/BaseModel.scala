package com.asto.ez.framework.storage

import java.lang.reflect.ParameterizedType

import com.asto.ez.framework.EZContext
import com.ecfront.common.{BeanHelper, JsonHelper, Resp}
import com.typesafe.scalalogging.slf4j.LazyLogging

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}

trait BaseModel extends Serializable{

  def toPersistentJsonString = {
    JsonHelper.toJsonString(BeanHelper.findValues(this))
  }

}

object BaseModel {

  val SPLIT = "@"

}

trait BaseStorage[M <: BaseModel] extends LazyLogging {

  protected val _modelClazz = this.getClass.getGenericInterfaces()(0).asInstanceOf[ParameterizedType].getActualTypeArguments()(0).asInstanceOf[Class[M]]

  val tableName = _modelClazz.getSimpleName.toLowerCase

  protected def preSave(model: M, context: EZContext): Future[Resp[M]] = Future(Resp.success(model))

  protected def postSave(doResult: String, context: EZContext): Future[Resp[String]] = Future(Resp.success(doResult))

  def save(model: M, context: EZContext = null): Future[Resp[String]] = {
    val p = Promise[Resp[String]]()
    preSave(model, context).onSuccess {
      case preResp =>
        if (preResp) {
          doSave(preResp.body, context).onSuccess {
            case doResp =>
              if (doResp) {
                postSave(doResp.body, context).onSuccess {
                  case postResp =>
                    p.success(postResp)
                }
              } else {
                p.success(doResp)
              }
          }
        } else {
          p.success(preResp)
        }
    }
    p.future
  }

  protected def doSave(model: M, context: EZContext): Future[Resp[String]]

  protected def preUpdate(model: M, context: EZContext): Future[Resp[M]] = Future(Resp.success(model))

  protected def postUpdate(doResult: String, context: EZContext): Future[Resp[String]] = Future(Resp.success(doResult))

  def update(model: M, context: EZContext = null): Future[Resp[String]] = {
    val p = Promise[Resp[String]]()
    preUpdate(model, context).onSuccess {
      case preResp =>
        if (preResp) {
          doUpdate(preResp.body, context).onSuccess {
            case doResp =>
              if (doResp) {
                postUpdate(doResp.body, context).onSuccess {
                  case postResp =>
                    p.success(postResp)
                }
              } else {
                p.success(doResp)
              }
          }
        } else {
          p.success(preResp)
        }
    }
    p.future
  }

  protected def doUpdate(model: M, context: EZContext): Future[Resp[String]]

  protected def preSaveOrUpdate(model: M, context: EZContext): Future[Resp[M]] = Future(Resp.success(model))

  protected def postSaveOrUpdate(doResult: String, context: EZContext): Future[Resp[String]] = Future(Resp.success(doResult))

  def saveOrUpdate(model: M, context: EZContext = null): Future[Resp[String]] = {
    val p = Promise[Resp[String]]()
    preSaveOrUpdate(model, context).onSuccess {
      case preResp =>
        if (preResp) {
          doSaveOrUpdate(preResp.body, context).onSuccess {
            case doResp =>
              if (doResp) {
                postSaveOrUpdate(doResp.body, context).onSuccess {
                  case postResp =>
                    p.success(postResp)
                }
              } else {
                p.success(doResp)
              }
          }
        } else {
          p.success(preResp)
        }
    }
    p.future
  }

  protected def doSaveOrUpdate(model: M, context: EZContext): Future[Resp[String]]

  protected def preUpdateByCond(newValues: String, condition: String, parameters: List[Any], context: EZContext): Future[Resp[(String, String, List[Any])]] = Future(Resp.success((newValues, condition, parameters)))

  protected def postUpdateByCond(newValues: String, condition: String, parameters: List[Any], context: EZContext): Future[Resp[Void]] = Future(Resp.success(null))

  def updateByCond(newValues: String, condition: String, parameters: List[Any] = List(), context: EZContext = null): Future[Resp[Void]] = {
    val p = Promise[Resp[Void]]()
    preUpdateByCond(newValues, condition, parameters, context).onSuccess {
      case preResp =>
        if (preResp) {
          doUpdateByCond(preResp.body._1, preResp.body._2, preResp.body._3, context).onSuccess {
            case doResp =>
              if (doResp) {
                postUpdateByCond(preResp.body._1, preResp.body._2, preResp.body._3, context).onSuccess {
                  case postResp =>
                    p.success(postResp)
                }
              } else {
                p.success(doResp)
              }
          }
        } else {
          p.success(preResp)
        }
    }
    p.future
  }

  protected def doUpdateByCond(newValues: String, condition: String, parameters: List[Any], context: EZContext): Future[Resp[Void]]

  protected def preDeleteById(id: Any, context: EZContext): Future[Resp[Any]] = Future(Resp.success(id))

  protected def postDeleteById(id: Any, context: EZContext): Future[Resp[Void]] = Future(Resp.success(null))

  def deleteById(id: Any, context: EZContext = null): Future[Resp[Void]] = {
    val p = Promise[Resp[Void]]()
    if (id == null) {
      p.success(Resp.badRequest("【id】不能为空"))
    } else {
      preDeleteById(id, context).onSuccess {
        case preResp =>
          if (preResp) {
            doDeleteById(preResp.body, context).onSuccess {
              case doResp =>
                if (doResp) {
                  postDeleteById(preResp.body, context).onSuccess {
                    case postResp =>
                      p.success(postResp)
                  }
                } else {
                  p.success(doResp)
                }
            }
          } else {
            p.success(preResp)
          }
      }
    }
    p.future
  }

  protected def doDeleteById(id: Any, context: EZContext): Future[Resp[Void]]

  protected def preDeleteByCond(condition: String, parameters: List[Any], context: EZContext): Future[Resp[(String, List[Any])]] = Future(Resp.success((condition, parameters)))

  protected def postDeleteByCond(condition: String, parameters: List[Any], context: EZContext): Future[Resp[Void]] = Future(Resp.success(null))

  def deleteByCond(condition: String, parameters: List[Any] = List(), context: EZContext = null): Future[Resp[Void]] = {
    val p = Promise[Resp[Void]]()
    if (condition == null) {
      p.success(Resp.badRequest("【condition】不能为空"))
    } else {
      preDeleteByCond(condition, parameters, context).onSuccess {
        case preResp =>
          if (preResp) {
            doDeleteByCond(preResp.body._1, preResp.body._2, context).onSuccess {
              case doResp =>
                if (doResp) {
                  postDeleteByCond(preResp.body._1, preResp.body._2, context).onSuccess {
                    case postResp =>
                      p.success(postResp)
                  }
                } else {
                  p.success(doResp)
                }
            }
          } else {
            p.success(preResp)
          }
      }
    }
    p.future
  }

  protected def doDeleteByCond(condition: String, parameters: List[Any], context: EZContext): Future[Resp[Void]]

  protected def preGetById(id: Any, context: EZContext): Future[Resp[Any]] = Future(Resp.success(id))

  protected def postGetById(id: Any, doResult: M, context: EZContext): Future[Resp[M]] = Future(Resp.success(doResult))

  def getById(id: Any, context: EZContext = null): Future[Resp[M]] = {
    val p = Promise[Resp[M]]()
    if (id == null) {
      p.success(Resp.badRequest("【id】不能为空"))
    } else {
      preGetById(id, context).onSuccess {
        case preResp =>
          if (preResp) {
            doGetById(preResp.body, context).onSuccess {
              case doResp =>
                if (doResp) {
                  postGetById(preResp.body, doResp.body, context).onSuccess {
                    case postResp =>
                      p.success(postResp)
                  }
                } else {
                  p.success(doResp)
                }
            }
          } else {
            p.success(preResp)
          }
      }
    }
    p.future
  }

  protected def doGetById(id: Any, context: EZContext): Future[Resp[M]]

  protected def preGetByCond(condition: String, parameters: List[Any], context: EZContext): Future[Resp[(String, List[Any])]] = Future(Resp.success((condition, parameters)))

  protected def postGetByCond(condition: String, parameters: List[Any], doResult: M, context: EZContext): Future[Resp[M]] = Future(Resp.success(doResult))

  def getByCond(condition: String, parameters: List[Any] = List(), context: EZContext = null): Future[Resp[M]] = {
    val p = Promise[Resp[M]]()
    if (condition == null) {
      p.success(Resp.badRequest("【condition】不能为空"))
    } else {
      preGetByCond(condition, parameters, context).onSuccess {
        case preResp =>
          if (preResp) {
            doGetByCond(preResp.body._1, preResp.body._2, context).onSuccess {
              case doResp =>
                if (doResp) {
                  postGetByCond(preResp.body._1, preResp.body._2, doResp.body, context).onSuccess {
                    case postResp =>
                      p.success(postResp)
                  }
                } else {
                  p.success(doResp)
                }
            }
          } else {
            p.success(preResp)
          }
      }
    }
    p.future
  }

  protected def doGetByCond(condition: String, parameters: List[Any], context: EZContext): Future[Resp[M]]

  protected def preExistById(id: Any, context: EZContext): Future[Resp[Any]] = Future(Resp.success(id))

  protected def postExistById(id: Any, doResult: Boolean, context: EZContext): Future[Resp[Boolean]] = Future(Resp.success(doResult))

  def existById(id: Any, context: EZContext = null): Future[Resp[Boolean]] = {
    val p = Promise[Resp[Boolean]]()
    if (id == null) {
      p.success(Resp.badRequest("【id】不能为空"))
    } else {
      preExistById(id, context).onSuccess {
        case preResp =>
          if (preResp) {
            doExistById(preResp.body, context).onSuccess {
              case doResp =>
                if (doResp) {
                  postExistById(preResp.body, doResp.body, context).onSuccess {
                    case postResp =>
                      p.success(postResp)
                  }
                } else {
                  p.success(doResp)
                }
            }
          } else {
            p.success(preResp)
          }
      }
    }
    p.future
  }

  protected def doExistById(id: Any, context: EZContext = null): Future[Resp[Boolean]]

  protected def preExistByCond(condition: String, parameters: List[Any], context: EZContext): Future[Resp[(String, List[Any])]] = Future(Resp.success((condition, parameters)))

  protected def postExistByCond(condition: String, parameters: List[Any], doResult: Boolean, context: EZContext): Future[Resp[Boolean]] = Future(Resp.success(doResult))

  def existByCond(condition: String, parameters: List[Any] = List(), context: EZContext = null): Future[Resp[Boolean]] = {
    val p = Promise[Resp[Boolean]]()
    if (condition == null) {
      p.success(Resp.badRequest("【condition】不能为空"))
    } else {
      preExistByCond(condition, parameters, context).onSuccess {
        case preResp =>
          if (preResp) {
            doExistByCond(preResp.body._1, preResp.body._2, context).onSuccess {
              case doResp =>
                if (doResp) {
                  postExistByCond(preResp.body._1, preResp.body._2, doResp.body, context).onSuccess {
                    case postResp =>
                      p.success(postResp)
                  }
                } else {
                  p.success(doResp)
                }
            }
          } else {
            p.success(preResp)
          }
      }
    }
    p.future
  }

  protected def doExistByCond(condition: String, parameters: List[Any], context: EZContext): Future[Resp[Boolean]]

  protected def preFind(condition: String, parameters: List[Any], context: EZContext): Future[Resp[(String, List[Any])]] = Future(Resp.success((condition, parameters)))

  protected def postFind(condition: String, parameters: List[Any], doResult: List[M], context: EZContext): Future[Resp[List[M]]] = Future(Resp.success(doResult))

  def find(condition: String, parameters: List[Any] = List(), context: EZContext = null): Future[Resp[List[M]]] = {
    val p = Promise[Resp[List[M]]]()
    preFind(condition, parameters, context).onSuccess {
      case preResp =>
        if (preResp) {
          doFind(preResp.body._1, preResp.body._2, context).onSuccess {
            case doResp =>
              if (doResp) {
                postFind(preResp.body._1, preResp.body._2, doResp.body, context).onSuccess {
                  case postResp =>
                    p.success(postResp)
                }
              } else {
                p.success(doResp)
              }
          }
        } else {
          p.success(preResp)
        }
    }
    p.future
  }

  protected def doFind(condition: String, parameters: List[Any], context: EZContext): Future[Resp[List[M]]]

  protected def prePage(condition: String, parameters: List[Any], pageNumber: Long, pageSize: Int, context: EZContext): Future[Resp[(String, List[Any])]] = Future(Resp.success((condition, parameters)))

  protected def postPage(condition: String, parameters: List[Any], pageNumber: Long, pageSize: Int, doResult: Page[M], context: EZContext): Future[Resp[Page[M]]] = Future(Resp.success(doResult))

  def page(condition: String, parameters: List[Any] = List(), pageNumber: Long = 1, pageSize: Int = 10, context: EZContext = null): Future[Resp[Page[M]]] = {
    val p = Promise[Resp[Page[M]]]()
    prePage(condition, parameters, pageNumber, pageSize, context).onSuccess {
      case preResp =>
        if (preResp) {
          doPage(preResp.body._1, preResp.body._2, pageNumber, pageSize, context).onSuccess {
            case doResp =>
              if (doResp) {
                postPage(preResp.body._1, preResp.body._2, pageNumber, pageSize, doResp.body, context).onSuccess {
                  case postResp =>
                    p.success(postResp)
                }
              } else {
                p.success(doResp)
              }
          }
        } else {
          p.success(preResp)
        }
    }
    p.future
  }

  protected def doPage(condition: String, parameters: List[Any], pageNumber: Long, pageSize: Int, context: EZContext): Future[Resp[Page[M]]]

  protected def preCount(condition: String, parameters: List[Any], context: EZContext): Future[Resp[(String, List[Any])]] = Future(Resp.success((condition, parameters)))

  protected def postCount(condition: String, parameters: List[Any], doResult: Long, context: EZContext): Future[Resp[Long]] = Future(Resp.success(doResult))

  def count(condition: String, parameters: List[Any] = List(), context: EZContext = null): Future[Resp[Long]] = {
    val p = Promise[Resp[Long]]()
    preCount(condition, parameters, context).onSuccess {
      case preResp =>
        if (preResp) {
          doCount(preResp.body._1, preResp.body._2, context).onSuccess {
            case doResp =>
              if (doResp) {
                postCount(preResp.body._1, preResp.body._2, doResp.body, context).onSuccess {
                  case postResp =>
                    p.success(postResp)
                }
              } else {
                p.success(doResp)
              }
          }
        } else {
          p.success(preResp)
        }
    }
    p.future
  }

  protected def doCount(condition: String, parameters: List[Any], context: EZContext): Future[Resp[Long]]

}



