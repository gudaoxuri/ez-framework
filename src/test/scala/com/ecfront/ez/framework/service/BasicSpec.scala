package com.ecfront.ez.framework.service

import com.ecfront.ez.framework.module.schedule.ScheduleService
import com.ecfront.ez.framework.rpc.cluster.ClusterManager
import com.ecfront.ez.framework.service.protocols.{JDBCService, RedisService}
import org.scalatest._


abstract class BasicSpec extends FunSuite with BeforeAndAfter {

  before {
    ClusterManager.initCluster()
    RedisService.init()
    JDBCService.init()
  }

  after {
    ClusterManager.destoryCluster()
    ScheduleService.stop()
    RedisService.close()
    JDBCService.close()
  }

}





