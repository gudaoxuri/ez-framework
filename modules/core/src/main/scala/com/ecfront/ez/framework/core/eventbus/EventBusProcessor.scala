package com.ecfront.ez.framework.core.eventbus

import com.typesafe.scalalogging.slf4j.LazyLogging

trait EventBusProcessor extends LazyLogging {

  def publish(address: String, message: Any, args: Map[String, String] = Map()): Unit

  def request(address: String, message: Any, args: Map[String, String] = Map(), ha: Boolean = true): Unit

  def ack[E: Manifest](address: String, message: Any, args: Map[String, String] = Map(), timeout: Long = 30 * 1000): (E,Map[String,String])

  def subscribe[E: Manifest](address: String, reqClazz: Class[E] = null)(receivedFun: (E, Map[String, String]) => Unit): Unit

  def response[E: Manifest](address: String, reqClazz: Class[E] = null)(receivedFun: (E, Map[String, String]) => Unit): Unit

  def reply[E: Manifest](address: String, reqClazz: Class[E] = null)(receivedFun: (E, Map[String, String]) => (Any,Map[String,String])): Unit

}
