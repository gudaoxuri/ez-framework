package com.ecfront.ez.framework.core

import com.ecfront.ez.framework.core.logger.Logging

/**
  *  系统启动类，使用此类直接启动EZ服务
  */
object EZStartup extends App with Logging {

  EZManager.start()

}

