package com.ecfront.ez.framework.core.cluster

import com.ecfront.ez.framework.core.logger.Logging

trait ClusterMQ extends Logging {

  def publish(topic: String, message: String, args: Map[String, String]): Unit

  def subscribe(topic: String)(receivedFun: (String, Map[String, String]) => Unit): Unit

  def request(address: String, message: String, args: Map[String, String] = Map()): Unit

  def response(address: String)(receivedFun: (String, Map[String, String]) => Unit): Unit

}