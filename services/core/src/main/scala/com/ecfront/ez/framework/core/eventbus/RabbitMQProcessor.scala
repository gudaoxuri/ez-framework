package com.ecfront.ez.framework.core.eventbus

import com.ecfront.common.{JsonHelper, Resp}
import com.ecfront.ez.framework.core.cluster.RabbitMQClusterManager
import com.ecfront.ez.framework.core.rpc.RPCProcessor
import com.ecfront.ez.framework.core.{EZ, EZContext}

/**
  * 事件总线处理器
  */
class RabbitMQProcessor extends EventBusProcessor {

  private[ecfront] val FLAG_CONTEXT = "__ez_context__"

  private[core] def init(): Resp[Void] = {
    logger.info("[EB] Init successful")
    Resp.success(null)
  }

  override protected def doPublish(address: String, message: Any, args: Map[String, String]): Unit = {
    RabbitMQClusterManager.publish(address, JsonHelper.toJsonString(message),
      args + (FLAG_CONTEXT -> JsonHelper.toJsonString(EZ.context)))
  }

  override protected def doRequest(address: String, message: Any, args: Map[String, String], ha: Boolean): Unit = {
    RabbitMQClusterManager.request(address, JsonHelper.toJsonString(message),
      args + (FLAG_CONTEXT -> JsonHelper.toJsonString(EZ.context)))
  }

  override protected def doAck[E](address: String, message: Any, args: Map[String, String], timeout: Long)
                                 (implicit e: Manifest[E]): (E, Map[String, String]) = {
    val result = RabbitMQClusterManager.ack(address, JsonHelper.toJsonString(message),
      args + (FLAG_CONTEXT -> JsonHelper.toJsonString(EZ.context)), timeout)
    EZContext.setContext(JsonHelper.toObject[EZContext](result._2(FLAG_CONTEXT)))
    val headers = result._2 - FLAG_CONTEXT
    try {
      val msg =
        if (e != manifest[Nothing]) {
          JsonHelper.toObject[E](result._1)
        } else {
          null.asInstanceOf[E]
        }
      (msg, headers)
    } catch {
      case e: Throwable =>
        logger.error(s"[EB] Ack reply a message error : [$address] : ${result._1} ", e.getMessage)
        throw e
    }
  }

  override protected def doSubscribe[E: Manifest](address: String, reqClazz: Class[E])(receivedFun: (E, Map[String, String]) => Unit): Unit = {
    RabbitMQClusterManager.subscribe(address) {
      (message, args) =>
        EZContext.setContext(JsonHelper.toObject[EZContext](args(FLAG_CONTEXT)))
        val headers = args - FLAG_CONTEXT
        logger.trace(s"[EB] Received a subscribe message [$address] : $headers > ${RPCProcessor.cutPrintShow(message)} ")
        try {
          val message = toObject[E](message, reqClazz)
          receivedFun(message, headers)
        } catch {
          case ex: Throwable =>
            logger.error(s"[EB] subscribe [$address] Execute error", ex)
        }
    }
  }

  override protected def doResponse[E: Manifest](address: String, reqClazz: Class[E])(receivedFun: (E, Map[String, String]) => Unit): Unit = {
    RabbitMQClusterManager.response(address) {
      (message, args) =>
        EZContext.setContext(JsonHelper.toObject[EZContext](args(FLAG_CONTEXT)))
        val headers = args - FLAG_CONTEXT
        logger.trace(s"[EB] Received a response message [$address] : $headers > ${RPCProcessor.cutPrintShow(message)} ")
        try {
          val message = toObject[E](message, reqClazz)
          receivedFun(message, headers)
        } catch {
          case ex: Throwable =>
            logger.error(s"[EB] response [$address] Execute error", ex)
        }
    }
  }

  override protected def doReply[E: Manifest](address: String, reqClazz: Class[E])
                                             (receivedFun: (E, Map[String, String]) => (Any, Map[String, String])): Unit = {
    RabbitMQClusterManager.reply(address) {
      (message, args) =>
        EZContext.setContext(JsonHelper.toObject[EZContext](args(FLAG_CONTEXT)))
        val headers = args - FLAG_CONTEXT
        logger.trace(s"[EB] Received a reply message [$address] : $headers > ${RPCProcessor.cutPrintShow(message)} ")
        try {
          val message = toObject[E](message, reqClazz)
          val result = receivedFun(message, headers)
          (JsonHelper.toJsonString(result._1), result._2 + (FLAG_CONTEXT -> JsonHelper.toJsonString(EZ.context)))
        } catch {
          case ex: Throwable =>
            logger.error(s"[EB] reply [$address] Execute error", ex)
            (JsonHelper.toJsonString(Resp.serverError(s"[EB] reply [$address] Execute error : ${ex.getMessage}")), Map(FLAG_CONTEXT -> JsonHelper.toJsonString(EZ.context)))
        }
    }
  }

}
