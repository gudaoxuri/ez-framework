package com.ecfront.ez.framework.service.gateway.interceptor

import java.lang.Long
import java.util.concurrent.ConcurrentSkipListSet

import com.ecfront.ez.framework.core.logger.Logging
import com.ecfront.ez.framework.service.gateway.helper.AsyncRedisProcessor
import io.vertx.core.{AsyncResult, Handler}

/**
  * 反DDoS攻击拦截器
  */
object AntiDDoSInterceptor extends Logging {

  private val limitIPs = new ConcurrentSkipListSet[String]()

  private var enable: Boolean = _
  private var reqRatePerMinute: Int = _
  private var illegalReqRatePerMinute: Int = _
  private val FLAG_LIMIT = "ez:rpc:limit:"
  private val DEFAULT_EXPIRE: Int = 60

  def init(_reqRatePerMinute: Int, _illegalReqRatePerMinute: Int): Unit = {
    enable = true
    reqRatePerMinute = _reqRatePerMinute
    illegalReqRatePerMinute = _illegalReqRatePerMinute
  }

  def isLimit(reqIp: String): Boolean = {
    limitFilter(reqIp)
    limitIPs.contains(reqIp)
  }

  private def limitFilter(reqIp: String): Unit = {
    if (enable) {
      AsyncRedisProcessor.client().incr(FLAG_LIMIT + reqIp, new Handler[AsyncResult[Long]] {
        override def handle(event: AsyncResult[Long]): Unit = {
          val currReqRate = event.result()
          if (currReqRate == 1) {
            AsyncRedisProcessor.client().expire(FLAG_LIMIT + reqIp, DEFAULT_EXPIRE, new Handler[AsyncResult[Long]] {
              override def handle(event: AsyncResult[Long]): Unit = {}
            })
          }
          AsyncRedisProcessor.client().incrby(FLAG_LIMIT + reqIp, 0, new Handler[AsyncResult[Long]] {
            override def handle(event: AsyncResult[Long]): Unit = {
              val currErrorReqRate = event.result()
              if (currErrorReqRate == 0) {
                AsyncRedisProcessor.client().expire(FLAG_LIMIT + "illegal:" + reqIp, DEFAULT_EXPIRE, new Handler[AsyncResult[Long]] {
                  override def handle(event: AsyncResult[Long]): Unit = {}
                })
              }
              if (currReqRate > reqRatePerMinute || currErrorReqRate > illegalReqRatePerMinute) {
                limitIPs.add(reqIp)
                logger.error(s"Too frequent requests, please try again later from $reqIp")
              } else {
                limitIPs.remove(reqIp)
              }
            }
          })
        }
      })
    }
  }

  def addIllegal(reqIp: String): Unit = {
    if (enable) {
      AsyncRedisProcessor.client().incr(FLAG_LIMIT + "illegal:" + reqIp, new Handler[AsyncResult[Long]] {
        override def handle(event: AsyncResult[Long]): Unit = {}
      })
    }
  }

}
