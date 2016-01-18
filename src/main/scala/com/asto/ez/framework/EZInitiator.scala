package com.asto.ez.framework

import com.typesafe.scalalogging.slf4j.LazyLogging

trait EZInitiator extends LazyLogging {

  def needInitialization: Boolean

  def initialize(): Unit

}
