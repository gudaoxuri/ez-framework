package com.ecfront.ez.framework.gateway

import com.typesafe.scalalogging.slf4j.LazyLogging
import io.vertx.core.{Vertx, VertxOptions}

class GatewayStartup extends LazyLogging{

  System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.SLF4JLogDelegateFactory")
  System.setProperty("vertx.disableFileCaching", "true")
  System.setProperty("vertx.disableFileCPResolving", "true")

  private val FLAG_PERF_EVENT_LOOP_POOL_SIZE = "eventLoopPoolSize"
  private val FLAG_PERF_WORKER_POOL_SIZE = "workerPoolSize"
  private val FLAG_PERF_INTERNAL_BLOCKING_POOL_SIZE = "internalBlockingPoolSize"
  private val FLAG_PERF_MAX_EVENT_LOOP_EXECUTE_TIME = "maxEventLoopExecuteTime"
  private val FLAG_PERF_WORKER_EXECUTE_TIME = "maxWorkerExecuteTime"
  private val FLAG_PERF_WARNING_EXCEPTION_TIME = "warningExceptionTime"


  /**
    * 初始Vertx
    *
    * @return vertx实例
    */
  private def initVertx(): Vertx = {
    if (System.getProperty(FLAG_PERF_EVENT_LOOP_POOL_SIZE) != null) {
      ezConfig.ez.perf += FLAG_PERF_EVENT_LOOP_POOL_SIZE -> System.getProperty(FLAG_PERF_EVENT_LOOP_POOL_SIZE).toInt
    }
    if (System.getProperty(FLAG_PERF_WORKER_POOL_SIZE) != null) {
      ezConfig.ez.perf += FLAG_PERF_WORKER_POOL_SIZE -> System.getProperty(FLAG_PERF_WORKER_POOL_SIZE).toInt
    }
    if (System.getProperty(FLAG_PERF_INTERNAL_BLOCKING_POOL_SIZE) != null) {
      ezConfig.ez.perf += FLAG_PERF_INTERNAL_BLOCKING_POOL_SIZE -> System.getProperty(FLAG_PERF_INTERNAL_BLOCKING_POOL_SIZE).toInt
    }
    if (System.getProperty(FLAG_PERF_MAX_EVENT_LOOP_EXECUTE_TIME) != null) {
      ezConfig.ez.perf += FLAG_PERF_MAX_EVENT_LOOP_EXECUTE_TIME -> System.getProperty(FLAG_PERF_MAX_EVENT_LOOP_EXECUTE_TIME).toLong
    }
    if (System.getProperty(FLAG_PERF_WORKER_EXECUTE_TIME) != null) {
      ezConfig.ez.perf += FLAG_PERF_WORKER_EXECUTE_TIME -> System.getProperty(FLAG_PERF_WORKER_EXECUTE_TIME).toLong
    }
    if (System.getProperty(FLAG_PERF_WARNING_EXCEPTION_TIME) != null) {
      ezConfig.ez.perf += FLAG_PERF_WARNING_EXCEPTION_TIME -> System.getProperty(FLAG_PERF_WARNING_EXCEPTION_TIME).toLong
    }
    val opt = new VertxOptions()
    if (perf.contains(FLAG_PERF_EVENT_LOOP_POOL_SIZE)) {

    }
    if (perf.contains(FLAG_PERF_WORKER_POOL_SIZE)) {
      opt.setWorkerPoolSize(perf(FLAG_PERF_WORKER_POOL_SIZE).asInstanceOf[Int])
    }
    if (perf.contains(FLAG_PERF_INTERNAL_BLOCKING_POOL_SIZE)) {
      opt.setInternalBlockingPoolSize(perf(FLAG_PERF_INTERNAL_BLOCKING_POOL_SIZE).asInstanceOf[Int])
    }
    if (perf.contains(FLAG_PERF_MAX_EVENT_LOOP_EXECUTE_TIME)) {
      opt.setMaxEventLoopExecuteTime(perf(FLAG_PERF_MAX_EVENT_LOOP_EXECUTE_TIME).asInstanceOf[Long] * 1000000)
    }
    if (perf.contains(FLAG_PERF_WORKER_EXECUTE_TIME)) {
      opt.setMaxWorkerExecuteTime(perf(FLAG_PERF_WORKER_EXECUTE_TIME).asInstanceOf[Long] * 1000000)
    }
    if (perf.contains(FLAG_PERF_WARNING_EXCEPTION_TIME)) {
      opt.setWarningExceptionTime(perf(FLAG_PERF_WARNING_EXCEPTION_TIME).asInstanceOf[Long] * 1000000)
    }
    Vertx.vertx(opt)
  }

  def main(args: Array[String]): Unit = {

  }

}
