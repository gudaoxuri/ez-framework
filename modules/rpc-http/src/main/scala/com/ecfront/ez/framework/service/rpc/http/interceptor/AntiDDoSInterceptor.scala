package com.ecfront.ez.framework.service.rpc.http.interceptor

import java.lang.Long
import java.util.concurrent.TimeUnit

import com.ecfront.common.Resp
import com.ecfront.ez.framework.service.redis.RedisProcessor
import com.typesafe.scalalogging.slf4j.LazyLogging

/**
  * 反DDoS攻击拦截器
  */
object AntiDDoSInterceptor extends LazyLogging {

  private var enable: Boolean = false
  private var reqRatePerMinute: Long = _
  private var illegalReqRatePerMinute: Long = _
  private val FLAG_LIMIT = "__ez_req_limit:"
  private val DEFAULT_EXPIRE: Long = 60L
  private val redis = RedisProcessor.custom()

  def init(_reqRatePerMinute: Long, _illegalReqRatePerMinute: Long): Unit = {
    enable = true
    reqRatePerMinute = _reqRatePerMinute
    illegalReqRatePerMinute = _illegalReqRatePerMinute
  }

  def limitFilter(reqIp: String): Resp[Void] = {
    if (enable) {
      val currReqRate = redis.getAtomicLong(FLAG_LIMIT + reqIp)
      val currErrorReqRate = redis.getAtomicLong(FLAG_LIMIT + "illegal:" + reqIp)
      if (currReqRate.get() == 0) {
        currReqRate.expireAsync(DEFAULT_EXPIRE, TimeUnit.SECONDS)
      }
      if (currErrorReqRate.get() == 0) {
        currErrorReqRate.expireAsync(DEFAULT_EXPIRE, TimeUnit.SECONDS)
      }
      if (currReqRate.incrementAndGet() >= reqRatePerMinute || currErrorReqRate.get() >= illegalReqRatePerMinute) {
        logger.error(s"Too frequent requests, please try again later from $reqIp")
        Resp.locked(s"Too frequent requests, please try again later")
      } else {
        Resp.success(null)
      }
    } else {
      Resp.success(null)
    }
  }

  def addIllegal(reqIp: String): Unit = {
    if (enable) {
      redis.getAtomicLong(FLAG_LIMIT + "illegal:" + reqIp).incrementAndGetAsync()
    }
  }

}
