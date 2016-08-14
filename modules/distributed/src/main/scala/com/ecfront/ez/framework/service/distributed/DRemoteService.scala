package com.ecfront.ez.framework.service.distributed

import java.util.concurrent.TimeUnit

import com.ecfront.ez.framework.service.redis.RedisProcessor
import com.typesafe.scalalogging.slf4j.LazyLogging

/**
  * 分布式PRC服务
  *
  */
case class DRemoteService[T]() extends LazyLogging {

  private val remoteService = RedisProcessor.custom().getRemoteSerivce("ez:remote")

  def register(interfaceClass: Class[T], implClass: T): Unit = {
    remoteService.register(interfaceClass, implClass)
  }

  def get(interfaceClass: Class[T], executeTimeoutSec: Int=0, ackTimeoutSec: Int=0): T = {
    if(executeTimeoutSec==0&&ackTimeoutSec==0){
      remoteService.get(interfaceClass)
    }else if(ackTimeoutSec==0){
      remoteService.get(interfaceClass, executeTimeoutSec, TimeUnit.SECONDS)
    }else {
      remoteService.get(interfaceClass, executeTimeoutSec, TimeUnit.SECONDS, ackTimeoutSec, TimeUnit.SECONDS)
    }
  }

}
