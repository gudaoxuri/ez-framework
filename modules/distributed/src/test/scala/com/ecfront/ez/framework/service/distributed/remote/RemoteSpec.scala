package com.ecfront.ez.framework.service.distributed.remote

import com.ecfront.ez.framework.core.test.MockStartupSpec
import com.ecfront.ez.framework.service.distributed.DRemoteService

class RemoteSpec extends MockStartupSpec {

  test("Remote test1") {
    val impl1 = new RemoteImpl1
    DRemoteService().register(classOf[RemoteInter], impl1)

    assert(DRemoteService().get(classOf[RemoteInter]).test("ssss") == "ssssssss")

  }

  test("Remote test2") {
    DRemoteService().register(classOf[RemoteInter], RemoteImpl2)

    assert(DRemoteService().get(classOf[RemoteInter]).test("b") == "bb")

  }
}


