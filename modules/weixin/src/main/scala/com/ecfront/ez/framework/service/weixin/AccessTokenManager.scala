package com.ecfront.ez.framework.service.weixin

import com.ecfront.ez.framework.service.redis.RedisProcessor

object AccessTokenManager extends BaseProcessor {

  private val FLAG_ACCESS_TOKEN = "weixin_access_token"

  private def reFetchAccessToken(): (String, Int) = {
    val result = getDataByBasicUrl(s"token?grant_type=client_credential&appid=${ServiceAdapter.appId}&secret=${ServiceAdapter.secret}")
    (result("access_token").asInstanceOf[String], result("expires_in").asInstanceOf[Int])
  }

  def getAccessToken: String = {
    val token = RedisProcessor.get(FLAG_ACCESS_TOKEN).body
    if (token != null) {
      token
    } else {
      val (accessToken, expires) = reFetchAccessToken()
      RedisProcessor.set(FLAG_ACCESS_TOKEN, accessToken, expires - 10)
      accessToken
    }
  }

}
