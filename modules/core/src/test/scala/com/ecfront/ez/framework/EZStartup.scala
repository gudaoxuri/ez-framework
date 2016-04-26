package com.ecfront.ez.framework

import com.ecfront.ez.framework.core.EZManager
import com.typesafe.scalalogging.slf4j.LazyLogging

/**
  *  系统启动类，使用此类直接启动EZ服务
  */
object EZStartup extends App with LazyLogging {

  EZManager.start()

}

