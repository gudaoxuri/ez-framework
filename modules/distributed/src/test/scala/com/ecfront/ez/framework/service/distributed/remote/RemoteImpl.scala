package com.ecfront.ez.framework.service.distributed.remote

import com.typesafe.scalalogging.slf4j.LazyLogging

class RemoteImpl1 extends RemoteInter1 with LazyLogging{

  override def test(s: String): String = {
    Thread.sleep(5000)
    logger.debug(s)
    s + s
  }

}

object RemoteImpl2 extends RemoteInter2 with LazyLogging {

  override def test(s: String): String = {
    Thread.sleep(5000)
    logger.debug(s)
    s + s
  }

}