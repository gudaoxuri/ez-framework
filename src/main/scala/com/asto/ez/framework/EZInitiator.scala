package com.asto.ez.framework

import com.ecfront.common.Resp
import com.typesafe.scalalogging.slf4j.LazyLogging

import scala.concurrent.Future

trait EZInitiator extends LazyLogging {

  def needInitialization: Future[Resp[Boolean]]

  def initialize(): Future[Resp[Void]]

}
