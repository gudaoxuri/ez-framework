package com.ecfront.ez.framework.service.distributed

import java.util.concurrent.TimeUnit

import com.ecfront.ez.framework.service.redis.RedisProcessor
import com.typesafe.scalalogging.slf4j.LazyLogging

/**
  * 分布式PRC服务
  *
  * NOTE: 接口不要混入Scala自定义类型，如返回Resp、继承LazyLogging等，
  * 否则不导致无法正常解析
  */
case class DRemoteService[T]() extends LazyLogging {

  private val remoteService = RedisProcessor.custom().getRemoteSerivce("ez:remote")

  /**
    * 注册
    *
    * @param interfaceClass 接口类
    * @param implClass      实现类
    */
  def register(interfaceClass: Class[T], implClass: T): Unit = {
    remoteService.register(interfaceClass, implClass)
  }

  def get(interfaceClass: Class[T], executeTimeoutSec: Int = 0, ackTimeoutSec: Int = 0): T = {
    if (executeTimeoutSec == 0 && ackTimeoutSec == 0) {
      remoteService.get(interfaceClass)
    } else if (ackTimeoutSec == 0) {
      remoteService.get(interfaceClass, executeTimeoutSec, TimeUnit.SECONDS)
    } else {
      remoteService.get(interfaceClass, executeTimeoutSec, TimeUnit.SECONDS, ackTimeoutSec, TimeUnit.SECONDS)
    }
  }

}
