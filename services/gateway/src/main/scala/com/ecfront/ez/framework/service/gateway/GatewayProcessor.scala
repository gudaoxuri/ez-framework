package com.ecfront.ez.framework.service.gateway

import java.util.Date

import com.ecfront.common.{JsonHelper, Resp}
import com.ecfront.ez.framework.core.eventbus.RabbitMQProcessor
import com.ecfront.ez.framework.core.helper.TimeHelper
import com.ecfront.ez.framework.core.interceptor.EZAsyncInterceptorProcessor
import com.ecfront.ez.framework.core.logger.Logging
import com.ecfront.ez.framework.core.rpc._
import com.ecfront.ez.framework.core.{EZ, EZContext}
import com.ecfront.ez.framework.service.gateway.interceptor.{EZAPIContext, GatewayInterceptor}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Promise

trait GatewayProcessor extends Logging {

  protected val HTTP_STATUS_200: Int = 200
  protected val HTTP_STATUS_302: Int = 302

  protected val FLAG_PROXY: String = "X-Forwarded-For"

  protected def execute(body: String, context: EZAPIContext, resultFun: Resp[(EZAPIContext, Map[String, Any])] => Unit): Unit = {
    if (EZ.isDebug) {
      logger.trace(s"Execute a request [${context.method}][${context.realUri}]，from ${context.remoteIP} | ${RPCProcessor.cutPrintShow(body)}")
    }
    EZAsyncInterceptorProcessor.process[EZAPIContext](GatewayInterceptor.category, context, {
      (context, param) =>
        val p = Promise[Resp[EZAPIContext]]()
        val msg = EZ.eb.asInstanceOf[RabbitMQProcessor].toAllowedMessage(body)
        val cxt = new EZContext
        cxt.id = EZ.createUUID
        cxt.startTime = TimeHelper.msf.format(new Date).toLong
        cxt.sourceIP = EZ.Info.projectIp
        cxt.sourceRPCPath = context.realUri
        if (context.optInfo.isDefined) {
          cxt.token = context.optInfo.get.token
          cxt.optAccCode = context.optInfo.get.accountCode
          cxt.optOrgCode = context.optInfo.get.organizationCode
        }
        EZContext.setContext(cxt)
        // 最长10分钟
        EZ.eb.ackAsync[Resp[Any]](EZ.eb.packageAddress(context.method, context.templateUri), msg, context.parameters, 600 * 1000) {
          (replyMessage, replyHeader) =>
            context.executeResult =
              if (replyHeader.contains(RPCProcessor.RESP_TYPE_FLAG)) {
                replyHeader(RPCProcessor.RESP_TYPE_FLAG) match {
                  case "DownloadFile" => JsonHelper.toObject[Resp[DownloadFile]](replyMessage)
                  case "ReqFile" => JsonHelper.toObject[Resp[ReqFile]](replyMessage)
                  case "Raw" => JsonHelper.toObject[Resp[Raw]](replyMessage)
                  case "RespRedirect" => JsonHelper.toObject[Resp[RespRedirect]](replyMessage)
                  case _ => replyMessage
                }
              } else {
                replyMessage
              }
            p.success(Resp.success(context))
        }
        p.future
    }).onSuccess {
      case resp =>
        resultFun(resp)
    }
  }

}
