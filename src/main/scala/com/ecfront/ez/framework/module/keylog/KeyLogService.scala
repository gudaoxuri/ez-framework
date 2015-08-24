package com.ecfront.ez.framework.module.keylog

import com.ecfront.common.StandardCode
import com.ecfront.ez.framework.module.core.EZReq
import com.ecfront.ez.framework.service.protocols.JDBCService
import com.ecfront.ez.framework.service.{BasicService, SyncService}

object KeyLogService extends JDBCService[KeyLogModel, EZReq] with SyncService[KeyLogModel, EZReq] with BasicService {

  def success(message: String, request: Option[EZReq]): Unit = {
    log(StandardCode.SUCCESS, message, request)
  }

  def notFound(message: String, request: Option[EZReq]): Unit = {
    log(StandardCode.NOT_FOUND, message, request)
  }

  def badRequest(message: String, request: Option[EZReq]): Unit = {
    log(StandardCode.BAD_REQUEST, message, request)
  }

  def forbidden(message: String, request: Option[EZReq]): Unit = {
    log(StandardCode.FORBIDDEN, message, request)
  }

  def unAuthorized(message: String, request: Option[EZReq]): Unit = {
    log(StandardCode.UNAUTHORIZED, message, request)
  }

  def serverError(message: String, request: Option[EZReq]): Unit = {
    log(StandardCode.INTERNAL_SERVER_ERROR, message, request)
  }

  def notImplemented(message: String, request: Option[EZReq]): Unit = {
    log(StandardCode.NOT_IMPLEMENTED, message, request)
  }

  def serverUnavailable(message: String, request: Option[EZReq]): Unit = {
    log(StandardCode.SERVICE_UNAVAILABLE, message, request)
  }

  def customFail(code: String, message: String, request: Option[EZReq]): Unit = {
    log(code, message, request)
  }

  private def log(code: String, message: String, request: Option[EZReq]): Unit = {
    val log = KeyLogModel()
    log.code = code
    log.message = message
    log.loginId = request.get.loginId
    _save(log, request)
  }

}
