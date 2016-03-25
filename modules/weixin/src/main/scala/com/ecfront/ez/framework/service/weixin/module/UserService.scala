package com.ecfront.ez.framework.service.weixin.module

import com.ecfront.ez.framework.service.weixin.api.UserProcessor
import com.ecfront.ez.framework.service.weixin.vo.UserVO

object UserService extends BaseService {

  def findAllIds(): List[String] = {
    UserProcessor.findAllIds()
  }

  def getUserInfo(openId: String): UserVO = {
    UserProcessor.getUserInfo(openId)
  }

  def findUserInfo(openIds: List[String]): List[UserVO] = {
    UserProcessor.findUserInfo(openIds)
  }

}
