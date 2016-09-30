package com.ecfront.ez.framework.core.eventbus

import com.typesafe.scalalogging.slf4j.LazyLogging

trait EventBusProcessor extends LazyLogging {

  def publish(address: String, message: Any): Unit

  def request(address: String, message: Any, ha: Boolean = true): Unit

  def ack[E: Manifest](address: String, message: Any, timeout: Long = 30 * 1000): E

  def subscribe[E: Manifest](address: String)(receivedFun: E => Unit): Unit

  def response[E: Manifest](address: String)(receivedFun: E => Unit): Unit

  def reply[E: Manifest](address: String)(receivedFun: E => Any): Unit

}
