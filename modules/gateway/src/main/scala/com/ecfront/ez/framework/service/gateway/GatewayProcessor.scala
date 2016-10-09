package com.ecfront.ez.framework.service.gateway

import java.util.Date

import com.ecfront.common.{JsonHelper, Resp}
import com.ecfront.ez.framework.core.eventbus.VertxEventBusProcessor
import com.ecfront.ez.framework.core.helper.TimeHelper
import com.ecfront.ez.framework.core.interceptor.EZAsyncInterceptorProcessor
import com.ecfront.ez.framework.core.rpc._
import com.ecfront.ez.framework.core.{EZ, EZContext}
import com.ecfront.ez.framework.service.gateway.interceptor.{EZAPIContext, GatewayInterceptor}
import com.typesafe.scalalogging.slf4j.LazyLogging
import io.vertx.core.eventbus.{DeliveryOptions, Message, ReplyException}
import io.vertx.core.{AsyncResult, Handler}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Promise

trait GatewayProcessor extends LazyLogging {

  protected val HTTP_STATUS_200: Int = 200
  protected val HTTP_STATUS_302: Int = 302

  protected val FLAG_PROXY: String = "X-Forwarded-For"

  protected def execute(body: String, context: EZAPIContext, resultFun: Resp[(EZAPIContext, Map[String, Any])] => Unit): Unit = {
    if (EZ.isDebug) {
      logger.trace(s"Execute a request [${context.method}][${context.realUri}]，from ${context.remoteIP} | $body")
    }
    EZAsyncInterceptorProcessor.process[EZAPIContext](GatewayInterceptor.category, context, {
      (context, param) =>
        val p = Promise[Resp[EZAPIContext]]()
        val msg = EZ.eb.asInstanceOf[VertxEventBusProcessor].toAllowedMessage(body)
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
        val opt = new DeliveryOptions
        opt.addHeader(EZ.eb.asInstanceOf[VertxEventBusProcessor].FLAG_CONTEXT, JsonHelper.toJsonString(cxt))
        context.parameters.foreach {
          arg =>
            opt.addHeader(arg._1, arg._2)
        }
        // 最长10分钟
        opt.setSendTimeout(600 * 1000)
        EZ.eb.asInstanceOf[VertxEventBusProcessor].eb.send[String](
          RPCProcessor.packageAddress(context.channel, context.method, context.templateUri),
          msg, opt, new Handler[AsyncResult[Message[String]]] {
            override def handle(event: AsyncResult[Message[String]]): Unit = {
              if (event.succeeded()) {
                context.executeResult =
                  if (event.result().headers().contains(RPCProcessor.FLAG_RESP_TYPE)) {
                    event.result().headers().get(RPCProcessor.FLAG_RESP_TYPE) match {
                      case "DownloadFile" => JsonHelper.toObject[Resp[DownloadFile]](event.result().body())
                      case "ReqFile" => JsonHelper.toObject[Resp[ReqFile]](event.result().body())
                      case "Raw" => JsonHelper.toObject[Resp[Raw]](event.result().body())
                      case "RespRedirect" => JsonHelper.toObject[Resp[RespRedirect]](event.result().body())
                      case _ => event.result().body()
                    }
                  } else {
                    event.result().body()
                  }
                p.success(Resp.success(context))
              } else {
                event.cause() match {
                  case e: ReplyException =>
                    logger.warn(s"[Gateway] API send error : [${context.templateUri}] : ${event.cause().getMessage} ")
                    p.success(Resp.badRequest(s"[Gateway] API send error : [${context.templateUri}] : not implementation"))
                  case e: Throwable =>
                    logger.error(s"[Gateway] API send error : [${context.templateUri}] : ${event.cause().getMessage} ", event.cause())
                    p.success(Resp.serverError(s"[Gateway] API send error : [${context.templateUri}] : ${event.cause().getMessage} "))
                }
              }
            }
          })
        p.future
    }).onSuccess {
      case resp =>
        resultFun(resp)
    }
  }

}
