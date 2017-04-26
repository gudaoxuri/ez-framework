package com.ecfront.ez.framework.service.gateway.interceptor

import java.lang.Long

import com.ecfront.common.AsyncResp

import scala.collection.mutable

/**
  * 慢查询监控拦截器
  */
object SlowMonitorInterceptor extends GatewayInterceptor {

  var time: Long = _
  var includes: Set[String] = _
  var excludes: Set[String] = _

  def init(_time: Long, _includes: Set[String], _excludes: Set[String]): Unit = {
    time = _time
    includes = _includes.map(_.toLowerCase())
    excludes = _excludes.map(_.toLowerCase())
  }


  override def before(obj: EZAPIContext, context: mutable.Map[String, Any], p: AsyncResp[EZAPIContext]): Unit = {
    context += "ez_start_time" -> System.currentTimeMillis()
    p.success(obj)
  }

  override def after(obj: EZAPIContext, context: mutable.Map[String, Any], p: AsyncResp[EZAPIContext]): Unit = {
    val useTime = System.currentTimeMillis() - context("ez_start_time").asInstanceOf[Long]
    if (includes.nonEmpty) {
      // 存在包含网址过滤，仅记录在此网址中请求
      if (includes.contains(obj.method + ":" + obj.templateUri)) {
        logSlow(useTime, obj)
      }
    } else if (excludes.nonEmpty) {
      // 存在不包含网址过滤，此网址中请求不记录
      if (!excludes.contains(obj.method + ":" + obj.templateUri)) {
        logSlow(useTime, obj)
      }
    } else {
      // 记录所有请求
      logSlow(useTime, obj)
    }
    p.success(obj)
  }

  private def logSlow(useTime: Long, obj: EZAPIContext): Unit = {
    if (useTime > time) {
      logger.warn(s"[RPC][SlowTime] Request [${obj.method}:${obj.realUri}] use time [$useTime] ms")
    } else {
      logger.trace(s"[RPC][Time] Request [${obj.method}:${obj.realUri}] use time [$useTime] ms")
    }
  }

}
