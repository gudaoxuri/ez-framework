package com.ecfront.ez.framework.service.distributed.remote

import com.ecfront.ez.framework.core.test.MockStartupSpec
import com.ecfront.ez.framework.service.distributed.DRemoteService

class RemoteSpec extends MockStartupSpec {

  test("Remote test") {
    DRemoteService().register(classOf[RemoteInter1], new RemoteImpl1)
    DRemoteService().register(classOf[RemoteInter2], RemoteImpl2)
    assert(DRemoteService().get(classOf[RemoteInter1]).test("ssss") == "ssssssss")
    assert(DRemoteService().get(classOf[RemoteInter2]).test("b") == "bb")
  }

}


