package com.ecfront.ez.framework.core.eventbus

import com.ecfront.common.{JsonHelper, Resp}
import com.ecfront.ez.framework.core.cluster.{ClusterMQ, ClusterRPC}
import com.ecfront.ez.framework.core.logger.Logging
import com.ecfront.ez.framework.core.monitor.TaskMonitor
import com.ecfront.ez.framework.core.rpc.{Method, RPCProcessor}
import com.ecfront.ez.framework.core.{EZ, EZContext}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}

object EventBusProcessor extends Logging {

  private[ecfront] val FLAG_CONTEXT = "__ez_context__"
  val ADDRESS_SPLIT_FLAG = "@"

  private val DEFAULT_TIMEOUT =
    if (EZ.isDebug) {
      Long.MaxValue
    } else {
      60L * 1000
    }

  private var rpcCluster: ClusterRPC = _
  private var mqCluster: ClusterMQ = _

  def init(_rpcCluster: ClusterRPC, _mqCluster: ClusterMQ): Resp[Void] = {
    if (_rpcCluster != null) {
      rpcCluster = _rpcCluster
    }
    if (_mqCluster != null) {
      mqCluster = _mqCluster
    }
    Resp.success(null)
  }

  def publish(address: String, message: Any, args: Map[String, String] = Map()): Unit = {
    val addr = packageAddress(Method.PUB_SUB.toString, address)
    val msg = toAllowedMessage(message)
    logger.trace(s"[EB] Publish a message [$addr] : $args > ${msg.toString} ")
    doPublish(addr, msg, args)
  }

  private def doPublish(address: String, message: Any, args: Map[String, String]): Unit = {
    mqCluster.publish(address, toJsonString(message),
      args + (FLAG_CONTEXT -> JsonHelper.toJsonString(EZ.context)))
  }

  def request(address: String, message: Any, args: Map[String, String] = Map(), ha: Boolean = true): Unit = {
    val addr = packageAddress(Method.REQ_RESP.toString, address)
    val msg = toAllowedMessage(message)
    logger.trace(s"[EB] Request a message [$addr] : $args > ${msg.toString} ")
    doRequest(addr, msg, args, ha)
  }

  private def doRequest(address: String, message: Any, args: Map[String, String], ha: Boolean): Unit = {
    mqCluster.request(address, toJsonString(message),
      args + (FLAG_CONTEXT -> JsonHelper.toJsonString(EZ.context)))
  }

  def pubReq(address: String, message: Any, args: Map[String, String] = Map(), ha: Boolean = true): Unit = {
    publish(address, message, args)
    request(address, message, args, ha)
  }

  def ack[E: Manifest](address: String, message: Any, args: Map[String, String] = Map(), timeout: Long = DEFAULT_TIMEOUT): (E, Map[String, String]) = {
    val addr = packageAddress(Method.ACK.toString, address)
    val msg = toAllowedMessage(message)
    val taskId = TaskMonitor.add(s"ACK [$address] Task")
    logger.trace(s"[EB] ACK a message [$addr] : $args > ${msg.toString} ")
    try {
      doAck[E](addr, msg, args, timeout)
    } finally {
      TaskMonitor.remove(taskId)
    }
  }

  private def doAck[E](address: String, message: Any, args: Map[String, String], timeout: Long)
                      (implicit e: Manifest[E]): (E, Map[String, String]) = {
    val result = rpcCluster.ack(address, toJsonString(message),
      args + (FLAG_CONTEXT -> JsonHelper.toJsonString(EZ.context)), timeout)
    if (result._2.contains(FLAG_CONTEXT)) {
      EZContext.setContext(JsonHelper.toObject[EZContext](result._2(FLAG_CONTEXT)))
    }
    val headers = result._2 - FLAG_CONTEXT
    try {
      val msg =
        if (e == manifest[String]) {
          result._1.asInstanceOf[E]
        } else if (e != manifest[Nothing]) {
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

  def ackAsync[E: Manifest](address: String, message: Any, args: Map[String, String] = Map(), timeout: Long = DEFAULT_TIMEOUT)(replyFun: => (E, Map[String, String]) => Unit): Unit = {
    val addr = packageAddress(Method.ACK.toString, address)
    val msg = toAllowedMessage(message)
    val taskId = TaskMonitor.add(s"ACK [$address] Task")
    logger.trace(s"[EB] ACK async a message [$addr] : $args > ${msg.toString} ")
    doAckAsync[E]({
      try {
        replyFun
      } finally {
        TaskMonitor.remove(taskId)
      }
    }, {
      _ =>
        TaskMonitor.remove(taskId)
    }, addr, msg, args, timeout)
  }

  private def doAckAsync[E](replyFun: => (E, Map[String, String]) => Unit, replyError: => Throwable => Unit,
                            address: String, message: Any, args: Map[String, String], timeout: Long)(implicit e: Manifest[E]): Unit = {
    rpcCluster.ackAsync(address, toJsonString(message),
      args + (FLAG_CONTEXT -> JsonHelper.toJsonString(EZ.context)), timeout)({
      (replyMessage, replyArgs) =>
        if (replyArgs.contains(FLAG_CONTEXT)) {
          EZContext.setContext(JsonHelper.toObject[EZContext](replyArgs(FLAG_CONTEXT)))
        }
        val headers = replyArgs - FLAG_CONTEXT
        try {
          val msg =
            if (e == manifest[String]) {
              replyMessage.asInstanceOf[E]
            } else if (e != manifest[Nothing]) {
              JsonHelper.toObject[E](replyMessage)
            } else {
              null.asInstanceOf[E]
            }
          replyFun(msg, headers)
        } catch {
          case e: Throwable =>
            logger.error(s"[EB] Ack reply a message error : [$address] : $message ", e.getMessage)
            throw e
        }
    }, replyError)
  }

  def subscribe[E: Manifest](address: String, reqClazz: Class[E] = null)(receivedFun: (E, Map[String, String]) => Unit): Unit = {
    doSubscribe[E](packageAddress(Method.PUB_SUB.toString, address), reqClazz)(receivedFun)
  }

  private def doSubscribe[E: Manifest](address: String, reqClazz: Class[E])(receivedFun: (E, Map[String, String]) => Unit): Unit = {
    mqCluster.subscribe(address) {
      (message, args) =>
        if (args.contains(FLAG_CONTEXT)) {
          EZContext.setContext(JsonHelper.toObject[EZContext](args(FLAG_CONTEXT)))
        }
        val headers = args - FLAG_CONTEXT
        logger.trace(s"[EB] Received a subscribe message [$address] : $headers > $message ")
        try {
          val msg = toObject[E](message, reqClazz)
          receivedFun(msg, headers)
        } catch {
          case ex: Throwable =>
            logger.error(s"[EB] subscribe [$address] Execute error", ex)
        }
    }
  }

  def response[E: Manifest](address: String, reqClazz: Class[E] = null)(receivedFun: (E, Map[String, String]) => Unit): Unit = {
    doResponse[E](packageAddress(Method.REQ_RESP.toString, address), reqClazz)(receivedFun)
  }

  private def doResponse[E: Manifest](address: String, reqClazz: Class[E])(receivedFun: (E, Map[String, String]) => Unit): Unit = {
    mqCluster.response(address) {
      (message, args) =>
        if (args.contains(FLAG_CONTEXT)) {
          EZContext.setContext(JsonHelper.toObject[EZContext](args(FLAG_CONTEXT)))
        }
        val headers = args - FLAG_CONTEXT
        logger.trace(s"[EB] Received a response message [$address] : $headers > $message ")
        try {
          val msg = toObject[E](message, reqClazz)
          receivedFun(msg, headers)
        } catch {
          case ex: Throwable =>
            logger.error(s"[EB] response [$address] Execute error", ex)
        }
    }
  }

  def reply[E: Manifest](address: String, reqClazz: Class[E] = null)(receivedFun: (E, Map[String, String]) => (Any, Map[String, String])): Unit = {
    doReply[E](packageAddress(Method.ACK.toString, address), reqClazz)(receivedFun)
  }

  private def doReply[E: Manifest](address: String, reqClazz: Class[E])
                                  (receivedFun: (E, Map[String, String]) => (Any, Map[String, String])): Unit = {
    rpcCluster.reply(address) {
      (message, args) =>
        if (args.contains(FLAG_CONTEXT)) {
          EZContext.setContext(JsonHelper.toObject[EZContext](args(FLAG_CONTEXT)))
        }
        val headers = args - FLAG_CONTEXT
        logger.trace(s"[EB] Received a reply message [$address] : $headers > $message ")
        try {
          val msg = toObject[E](message, reqClazz)
          val result = receivedFun(msg, headers)
          (toJsonString(result._1), result._2 + (FLAG_CONTEXT -> JsonHelper.toJsonString(EZ.context)))
        } catch {
          case ex: Throwable =>
            logger.error(s"[EB] reply [$address] Execute error", ex)
            (toJsonString(Resp.serverError(s"[EB] reply [$address] Execute error : ${ex.getMessage}")), Map(FLAG_CONTEXT -> JsonHelper.toJsonString(EZ.context)))
        }
    }
  }

  def replyAsync[E: Manifest](address: String, reqClazz: Class[E] = null)(receivedFun: (E, Map[String, String]) => Future[(Any, Map[String, String])]): Unit = {
    doReplyAsync[E](packageAddress(Method.ACK.toString, address), reqClazz)(receivedFun)
  }

  private def doReplyAsync[E: Manifest](address: String, reqClazz: Class[E])
                                       (receivedFun: (E, Map[String, String]) => Future[(Any, Map[String, String])]): Unit = {
    rpcCluster.replyAsync(address) {
      (message, args) =>
        val p = Promise[(String, Map[String, String])]()
        val tmpContent =
          if (args.contains(FLAG_CONTEXT)) {
            EZContext.setContext(JsonHelper.toObject[EZContext](args(FLAG_CONTEXT)))
            args(FLAG_CONTEXT)
          } else {
            JsonHelper.toJsonString(EZContext.getContext)
          }
        val headers = args - FLAG_CONTEXT
        logger.trace(s"[EB] Received a reply message [$address] : $headers > $message ")
        try {
          val msg = toObject[E](message, reqClazz)
          receivedFun(msg, headers).onSuccess {
            case result =>
              p.success((toJsonString(result._1), result._2 + (FLAG_CONTEXT -> tmpContent)))
          }
        } catch {
          case ex: Throwable =>
            logger.error(s"[EB] reply [$address] Execute error", ex)
            p.success((toJsonString(Resp.serverError(s"[EB] reply [$address] Execute error : ${ex.getMessage}")), Map(FLAG_CONTEXT -> tmpContent)))
        }
        p.future
    }
  }

  private def toJsonString(obj: Any): String = {
    obj match {
      case o: String => o
      case _ => JsonHelper.toJsonString(obj)
    }
  }

  def packageAddress(defaultMethod: String, path: String): String = {
    val formatPath = if (path.endsWith("/")) path else path + "/"
    if (formatPath.contains("@")) {
      formatPath
    } else {
      defaultMethod + ADDRESS_SPLIT_FLAG + formatPath
    }
  }

  private[ecfront] def toAllowedMessage(message: Any): Any = {
    message match {
      case m if m.isInstanceOf[String] || m.isInstanceOf[Int] ||
        m.isInstanceOf[Long] || m.isInstanceOf[Double] || m.isInstanceOf[Float] ||
        m.isInstanceOf[Boolean] || m.isInstanceOf[BigDecimal] || m.isInstanceOf[Short] => message
      case _ => JsonHelper.toJsonString(message)
    }
  }

  private def toObject[E](message: Any, reqClazz: Class[E])(implicit e: Manifest[E]): E = {
    if (reqClazz != null) {
      reqClazz match {
        case m if classOf[String].isAssignableFrom(m) ||
          classOf[Int].isAssignableFrom(m) ||
          classOf[Long].isAssignableFrom(m) ||
          classOf[Double].isAssignableFrom(m) ||
          classOf[Float].isAssignableFrom(m) ||
          classOf[Boolean].isAssignableFrom(m) ||
          classOf[BigDecimal].isAssignableFrom(m) ||
          classOf[Short].isAssignableFrom(m) => message.asInstanceOf[E]
        case _ => JsonHelper.toObject(message, reqClazz)
      }
    } else {
      if (e == manifest[String]) {
        if (message.isInstanceOf[String]) {
          message.asInstanceOf[E]
        } else {
          JsonHelper.toJsonString(message).asInstanceOf[E]
        }
      } else {
        JsonHelper.toObject[E](message)
      }
    }
  }

}
