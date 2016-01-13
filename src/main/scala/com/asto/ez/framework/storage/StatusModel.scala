package com.asto.ez.framework.storage

import com.asto.ez.framework.EZContext
import com.ecfront.common.Resp

import scala.beans.BeanProperty
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}

trait StatusModel extends BaseModel {

  @Index
  @BeanProperty var enable: Boolean = _

}

object StatusModel {

  val ENABLE_FLAG = "enable"

}

trait StatusStorage[M <: StatusModel] extends BaseStorage[M] {

  protected def preGetEnabledByCond(condition: String, parameters: List[Any], context: EZContext): Future[Resp[(String, List[Any])]] = Future(Resp.success((condition, parameters)))

  protected def postGetEnabledByCond(condition: String, parameters: List[Any], doResult: M, context: EZContext): Future[Resp[M]] = Future(Resp.success(doResult))

  def getEnabledByCond(condition: String, parameters: List[Any] = List(), context: EZContext = null): Future[Resp[M]] = {
    val p = Promise[Resp[M]]()
    if (condition == null) {
      p.success(Resp.badRequest("【condition】不能为空"))
    } else {
      preGetEnabledByCond(condition, parameters, context).onSuccess {
        case preResp =>
          if (preResp) {
            doGetEnabledByCond(preResp.body._1, preResp.body._2, context).onSuccess {
              case doResp =>
                if (doResp) {
                  postGetEnabledByCond(preResp.body._1, preResp.body._2, doResp.body, context).onSuccess {
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

  protected def doGetEnabledByCond(condition: String, parameters: List[Any], context: EZContext): Future[Resp[M]]

  protected def preFindEnabled(condition: String, parameters: List[Any], context: EZContext): Future[Resp[(String, List[Any])]] = Future(Resp.success((condition, parameters)))

  protected def postFindEnabled(condition: String, parameters: List[Any], doResult: List[M], context: EZContext): Future[Resp[List[M]]] = Future(Resp.success(doResult))

  def findEnabled(condition: String, parameters: List[Any] = List(), context: EZContext = null): Future[Resp[List[M]]] = {
    val p = Promise[Resp[List[M]]]()
    preFindEnabled(condition, parameters, context).onSuccess {
      case preResp =>
        if (preResp) {
          doFindEnabled(preResp.body._1, preResp.body._2, context).onSuccess {
            case doResp =>
              if (doResp) {
                postFindEnabled(preResp.body._1, preResp.body._2, doResp.body, context).onSuccess {
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

  protected def doFindEnabled(condition: String, parameters: List[Any], context: EZContext): Future[Resp[List[M]]]

  protected def prePageEnabled(condition: String, parameters: List[Any], pageNumber: Long, pageSize: Int, context: EZContext): Future[Resp[(String, List[Any])]] = Future(Resp.success((condition, parameters)))

  protected def postPageEnabled(condition: String, parameters: List[Any], pageNumber: Long, pageSize: Int, doResult: Page[M], context: EZContext): Future[Resp[Page[M]]] = Future(Resp.success(doResult))

  def pageEnabled(condition: String, parameters: List[Any] = List(), pageNumber: Long = 1, pageSize: Int = 10, context: EZContext = null): Future[Resp[Page[M]]] = {
    val p = Promise[Resp[Page[M]]]()
    prePageEnabled(condition, parameters, pageNumber, pageSize, context).onSuccess {
      case preResp =>
        if (preResp) {
          doPageEnabled(preResp.body._1, preResp.body._2, pageNumber, pageSize, context).onSuccess {
            case doResp =>
              if (doResp) {
                postPageEnabled(preResp.body._1, preResp.body._2, pageNumber, pageSize, doResp.body, context).onSuccess {
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

  protected def doPageEnabled(condition: String, parameters: List[Any], pageNumber: Long, pageSize: Int, context: EZContext): Future[Resp[Page[M]]]

  protected def preExistEnabledByCond(condition: String, parameters: List[Any], context: EZContext): Future[Resp[(String, List[Any])]] = Future(Resp.success((condition, parameters)))

  protected def postExistEnabledByCond(condition: String, parameters: List[Any], doResult: Boolean, context: EZContext): Future[Resp[Boolean]] = Future(Resp.success(doResult))

  def existEnabledByCond(condition: String, parameters: List[Any] = List(), context: EZContext = null): Future[Resp[Boolean]] = {
    val p = Promise[Resp[Boolean]]()
    if (condition == null) {
      p.success(Resp.badRequest("【condition】不能为空"))
    } else {
      preExistEnabledByCond(condition, parameters, context).onSuccess {
        case preResp =>
          if (preResp) {
            doExistEnabledByCond(preResp.body._1, preResp.body._2, context).onSuccess {
              case doResp =>
                if (doResp) {
                  postExistEnabledByCond(preResp.body._1, preResp.body._2, doResp.body, context).onSuccess {
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

  protected def doExistEnabledByCond(condition: String, parameters: List[Any], context: EZContext): Future[Resp[Boolean]]

  protected def preCountEnabled(condition: String, parameters: List[Any], context: EZContext): Future[Resp[(String, List[Any])]] = Future(Resp.success((condition, parameters)))

  protected def postCountEnabled(condition: String, parameters: List[Any], doResult: Long, context: EZContext): Future[Resp[Long]] = Future(Resp.success(doResult))

  def countEnabled(condition: String, parameters: List[Any] = List(), context: EZContext = null): Future[Resp[Long]] = {
    val p = Promise[Resp[Long]]()
    preCountEnabled(condition, parameters, context).onSuccess {
      case preResp =>
        if (preResp) {
          doCountEnabled(preResp.body._1, preResp.body._2, context).onSuccess {
            case doResp =>
              if (doResp) {
                postCountEnabled(preResp.body._1, preResp.body._2, doResp.body, context).onSuccess {
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

  protected def doCountEnabled(condition: String, parameters: List[Any], context: EZContext): Future[Resp[Long]]

  protected def preEnableById(id: Any, context: EZContext): Future[Resp[Any]] = Future(Resp.success(id))

  protected def postEnableById(id: Any, context: EZContext): Future[Resp[Void]] = Future(Resp.success(null))

  def enableById(id: Any, context: EZContext = null): Future[Resp[Void]] = {
    val p = Promise[Resp[Void]]()
    if (id == null) {
      p.success(Resp.badRequest("【id】不能为空"))
    } else {
      preEnableById(id, context).onSuccess {
        case preResp =>
          if (preResp) {
            doEnableById(preResp.body, context).onSuccess {
              case doResp =>
                if (doResp) {
                  postEnableById(preResp.body, context).onSuccess {
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

  protected def doEnableById(id: Any, context: EZContext): Future[Resp[Void]]

  protected def preDisableById(id: Any, context: EZContext): Future[Resp[Any]] = Future(Resp.success(id))

  protected def postDisableById(id: Any, context: EZContext): Future[Resp[Void]] = Future(Resp.success(null))

  def disableById(id: Any, context: EZContext = null): Future[Resp[Void]] = {
    val p = Promise[Resp[Void]]()
    if (id == null) {
      p.success(Resp.badRequest("【id】不能为空"))
    } else {
      preDisableById(id, context).onSuccess {
        case preResp =>
          if (preResp) {
            doDisableById(preResp.body, context).onSuccess {
              case doResp =>
                if (doResp) {
                  postDisableById(preResp.body, context).onSuccess {
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

  protected def doDisableById(id: Any, context: EZContext): Future[Resp[Void]]

}









