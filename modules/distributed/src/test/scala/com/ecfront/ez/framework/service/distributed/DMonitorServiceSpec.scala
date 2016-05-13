package com.ecfront.ez.framework.service.distributed

import com.ecfront.ez.framework.core.test.MockStartupSpec


class DMonitorServiceSpec extends MockStartupSpec {

  test("分布式服务监控测试") {

    DMonitorService.start()

    assert(DMonitorService.Manager.fetchAllServices.size == 1)
    assert(DMonitorService.Manager.fetchLiveReport.size == 1)
    val c1 = DMonitorService.Manager.fetchLiveReport.values.head
    Thread.sleep(30000)
    assert(c1 == DMonitorService.Manager.fetchLiveReport.values.head)
    Thread.sleep(31000)
    assert(c1 != DMonitorService.Manager.fetchLiveReport.values.head)
    DMonitorService.Manager.removeAService(DMonitorService.Manager.fetchLiveReport.keys.head)
    assert(DMonitorService.Manager.fetchAllServices.isEmpty)
  }

}





