package com.ecfront.ez.framework.rpc.process.channel.eb

import com.ecfront.common.{JsonHelper, Resp}
import com.ecfront.ez.framework.rpc.process.ClientProcessor
import io.vertx.core.eventbus.{DeliveryOptions, Message}
import io.vertx.core.{AsyncResult, Handler}

/**
 * EventBus 连接处理器
 */
class EventBusClientProcessor extends ClientProcessor with EventBusOptions {

  override protected def init(): Unit = {
    initEventbus(host, port)
  }

  override private[rpc] def destroy(): Unit = {
    destoryEventbus()
  }

  def doProcess[E, F](method: String, path: String, requestBody: Any, responseClass: Class[E], resultClass: Class[F], finishFun: => Option[F] => Unit, inject: Any) {
    val address = getAddress(method, path)
    if (address != null) {
      if (isPointToPoint) {
        if (responseClass == null) {
          val preResult = rpcClient.preExecuteInterceptor(method, path, inject)
          if (preResult) {
            EventBusOptions.eb.send(address, getBody(requestBody), setDeliveryOptions(method, path, "0", preResult.body))
            clientExecute(method, path, finishFun, None)
          }
        } else {
          val preResult = rpcClient.preExecuteInterceptor(method, path, inject)
          if (preResult) {
            EventBusOptions.eb.send(address, getBody(requestBody), setDeliveryOptions(method, path, "1", preResult.body), new Handler[AsyncResult[Message[String]]] {
              override def handle(event: AsyncResult[Message[String]]): Unit = {
                if (!event.succeeded()) {
                  logger.error(s"Send [$address] failed!", event.cause())
                } else {
                  if (resultClass == classOf[Resp[E]]) {
                    clientExecute(method, path, finishFun, Some(parseResp(event.result().body(), responseClass).asInstanceOf[F]))
                  } else {
                    clientExecute(method, path, finishFun, Some(JsonHelper.toObject(event.result().body(), responseClass).asInstanceOf[F]))
                  }
                }
              }
            })
          }
        }
      } else {
        val preResult = rpcClient.preExecuteInterceptor(method, path, inject)
        if (preResult) {
          EventBusOptions.eb.publish(address, getBody(requestBody), setDeliveryOptions(method, path, "0", preResult.body))
          clientExecute(method, path, finishFun, None)
        }
      }
    } else {
      logger.error(s"Can't found [$method][$path] in cluster !")
    }
  }

  private def setDeliveryOptions[E](method: String, path: String, isReply: String, interceptInfo: Map[String, String]): DeliveryOptions = {
    val deliveryOptions = new DeliveryOptions()
    deliveryOptions.setSendTimeout(60000L * 30)
    getParameter(path).foreach {
      item =>
        deliveryOptions.addHeader(item._1, item._2)
    }
    if (interceptInfo != null) {
      interceptInfo.foreach {
        item =>
          deliveryOptions.addHeader(FLAG_INTERCEPTOR_INFO + item._1, item._2)
      }
    }
    deliveryOptions.addHeader(FLAG_METHOD, method)
    deliveryOptions.addHeader(FLAG_PATH, removeParameter(path))
    deliveryOptions.addHeader(FLAG_IS_REPLY, isReply)
    deliveryOptions.setSendTimeout(30 * 60 * 1000)
    deliveryOptions
  }

  private def getBody(requestBody: Any): String = {
    requestBody match {
      case b: String => b
      case _ => JsonHelper.toJsonString(requestBody)
    }
  }

}

