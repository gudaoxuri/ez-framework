package com.ecfront.ez.framework.core.cluster

import com.ecfront.ez.framework.core.logger.Logging
import scala.concurrent.Future

trait ClusterRPC extends Logging {

  def ack(address: String, message: String, args: Map[String, String] = Map(), timeout: Long = 30 * 1000): (String, Map[String, String])

  def ackAsync(address: String, message: String, args: Map[String, String] = Map(), timeout: Long = 30 * 1000)
              (replyFun: => (String, Map[String, String]) => Unit, replyError: => Throwable => Unit): Unit

  def reply(address: String)(receivedFun: (String, Map[String, String]) => (String, Map[String, String])): Unit

  def replyAsync(address: String)(receivedFun: (String, Map[String, String]) => Future[(String, Map[String, String])]): Unit

}