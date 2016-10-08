package com.ecfront.ez.framework.gateway

import java.util.Date

import com.ecfront.common.{JsonHelper, Resp}
import com.ecfront.ez.framework.core.eventbus.VertxEventBusProcessor
import com.ecfront.ez.framework.core.helper.TimeHelper
import com.ecfront.ez.framework.core.interceptor.EZAsyncInterceptorProcessor
import com.ecfront.ez.framework.core.rpc._
import com.ecfront.ez.framework.core.{EZ, EZContext}
import com.ecfront.ez.framework.gateway.interceptor.{EZAPIContext, GatewayInterceptor}
import com.typesafe.scalalogging.slf4j.LazyLogging
import io.vertx.core.eventbus.{DeliveryOptions, Message}
import io.vertx.core.{AsyncResult, Handler}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Promise

trait GatewayProcessor extends LazyLogging {

  protected val HTTP_STATUS_200: Int = 200
  protected val HTTP_STATUS_302: Int = 302

  protected val FLAG_PROXY: String = "X-Forwarded-For"

  protected def execute(body: String, context: EZAPIContext, resultFun: Resp[(EZAPIContext, Map[String, Any])] => Unit): Unit = {
    if (!EZ.isDebug) {
      logger.info(s"Execute a request from ${context.remoteIP} to [${context.method}] ${context.realUri}")
    } else {
      logger.trace(s"Execute a request from ${context.remoteIP} to [${context.method}] ${context.realUri} | $body")
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
        if(context.optInfo.isDefined){
          cxt.token = context.optInfo.get.token
          cxt.optAccCode = context.optInfo.get.accountCode
          cxt.optOrgCode = context.optInfo.get.organizationCode
        }
        EZContext.setContext(cxt)
        val opt = new DeliveryOptions
        opt.addHeader(EZ.eb.asInstanceOf[VertxEventBusProcessor].FLAG_CONTEXT, JsonHelper.toJsonString(EZ.context))
        context.parameters.foreach {
          arg =>
            opt.addHeader(arg._1, arg._2)
        }
        opt.setSendTimeout(120 * 1000)
        EZ.eb.asInstanceOf[VertxEventBusProcessor].eb.send[String](
          RPCProcessor.packageAddress(context.channel, context.method, context.templateUri),
          msg, opt, new Handler[AsyncResult[Message[String]]] {
            override def handle(event: AsyncResult[Message[String]]): Unit = {
              if (event.succeeded()) {
                context.executeResult = event.result().body()
                p.success(Resp.success(context))
              } else {
                logger.error(s"[Gateway] API send error : [${context.templateUri}] : ${event.cause().getMessage} ", event.cause())
                p.success(Resp.serverError(s"[Gateway] API send error : [${context.templateUri}] : ${event.cause().getMessage} "))
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
