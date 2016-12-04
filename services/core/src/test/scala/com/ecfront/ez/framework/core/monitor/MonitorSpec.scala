package com.ecfront.ez.framework.core.monitor

import com.ecfront.ez.framework.core.{EZ, MockStartupSpec}

class MonitorSpec extends MockStartupSpec {

  test("Monitor Test") {
    EZ.newThread{
      logger.info("start1 ....")
      Thread.sleep(5000)
      logger.info("end1....")
    }

    EZ.newThread{
      EZ.newThread{
        logger.info("start2 ....")
        Thread.sleep(10000)
        logger.info("end2....")
      }
    }

  }

}


