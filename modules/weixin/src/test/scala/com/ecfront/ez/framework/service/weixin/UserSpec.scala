package com.ecfront.ez.framework.service.weixin

import com.ecfront.ez.framework.core.test.MockStartupSpec
import com.ecfront.ez.framework.service.weixin.api.UserProcessor

class UserSpec extends MockStartupSpec {

  test("user test") {
    val userIds = UserProcessor.findAllIds()
    assert(userIds.nonEmpty)
    val user = UserProcessor.getUserInfo(userIds.head)
    assert(user.nickname == "孤岛旭日")
    val users = UserProcessor.findUserInfo(List(userIds.head))
    assert(users.size == 1)
    assert(users.head.nickname == "孤岛旭日")
  }

}


