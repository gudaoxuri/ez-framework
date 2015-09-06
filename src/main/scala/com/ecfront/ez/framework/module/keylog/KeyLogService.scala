package com.ecfront.ez.framework.module.keylog

import com.ecfront.common.{Req, StandardCode}
import com.ecfront.ez.framework.service.SyncService
import com.ecfront.ez.framework.service.protocols.JDBCService

object KeyLogService extends JDBCService[EZ_Key_Log, Req] with SyncService[EZ_Key_Log, Req] {

  def success(message: String, request: Option[Req]): Unit = {
    log(StandardCode.SUCCESS, message, request)
  }

  def notFound(message: String, request: Option[Req]): Unit = {
    log(StandardCode.NOT_FOUND, message, request)
  }

  def badRequest(message: String, request: Option[Req]): Unit = {
    log(StandardCode.BAD_REQUEST, message, request)
  }

  def forbidden(message: String, request: Option[Req]): Unit = {
    log(StandardCode.FORBIDDEN, message, request)
  }

  def unAuthorized(message: String, request: Option[Req]): Unit = {
    log(StandardCode.UNAUTHORIZED, message, request)
  }

  def serverError(message: String, request: Option[Req]): Unit = {
    log(StandardCode.INTERNAL_SERVER_ERROR, message, request)
  }

  def notImplemented(message: String, request: Option[Req]): Unit = {
    log(StandardCode.NOT_IMPLEMENTED, message, request)
  }

  def serverUnavailable(message: String, request: Option[Req]): Unit = {
    log(StandardCode.SERVICE_UNAVAILABLE, message, request)
  }

  def customFail(code: String, message: String, request: Option[Req]): Unit = {
    log(code, message, request)
  }

  private def log(code: String, message: String, request: Option[Req]): Unit = {
    val log = EZ_Key_Log()
    log.code = code
    log.message = message
    log.login_Id = if (request.isDefined) request.get.login_Id else ""
    log.organization_id = if (request.isDefined) request.get.organization_id else ""
    _save(log, request)
  }

}
