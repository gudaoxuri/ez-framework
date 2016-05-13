package com.ecfront.ez.framework.service.distributed

import java.lang.Long

import com.ecfront.ez.framework.core.EZContext
import com.ecfront.ez.framework.service.redis.RedisProcessor
import com.typesafe.scalalogging.slf4j.LazyLogging
import io.vertx.core.Handler
import org.redisson.core.RMap

import scala.beans.BeanProperty
import scala.collection.JavaConversions._

/**
  * 分布式服务监控
  *
  */
object DMonitorService extends LazyLogging {

  private val HEARTBEAT_DELAY: Long = 60000L

  private val services: RMap[String, DService] = RedisProcessor.redis.getMap("__ez_service_list__")
  private val liveReport: RMap[String, Long] = RedisProcessor.redis.getMap("__ez_service_live_report__")

  /**
    * 启动当前服务监控
    */
  def start(): Unit = {
    register()
    heartbeat()
    sys.addShutdownHook(unRegister())
  }

  /**
    * 注册当前服务
    */
  private def register(): Unit = {
    val service = new DService
    service.app = EZContext.app
    service.module = EZContext.module
    service.instance = EZContext.instance
    service.projectIp = EZContext.projectIp
    service.projectHost = EZContext.projectHost
    service.projectPath = EZContext.projectPath
    services.fastPut(getKey, service)
  }

  /**
    * 注销当前服务
    */
  private def unRegister(): Unit = {
    services.remove(getKey)
    liveReport.remove(getKey)
  }

  /**
    * 上报当前服务状态
    */
  private def heartbeat(): Unit = {
    liveReport.putAsync(getKey, System.currentTimeMillis())
    EZContext.vertx.setPeriodic(HEARTBEAT_DELAY, new Handler[Long] {
      override def handle(event: Long): Unit = {
        liveReport.putAsync(getKey, System.currentTimeMillis())
      }
    })
  }

  private def getKey: String = EZContext.app + "_" + EZContext.module + "_" + EZContext.instance

  /**
    * 监控管理
    */
  object Manager {

    /**
      * 获取所有服务
      *
      * @return 所有服务
      */
    def fetchAllServices: Map[String, DService] = {
      services.getAll(services.keySet()).toMap
    }

    /**
      * 删除一个服务
      *
      * @param key 要删除的服务key
      */
    def removeAService(key: String): Unit = {
      services.remove(key)
    }

    /**
      * 删除所有服务
      */
    def removeAllServices(): Unit = {
      services.delete()
    }

    /**
      * 获取服务状态报告
      *
      * @return 服务状态报告
      */
    def fetchLiveReport: Map[String, Long] = {
      liveReport.getAll(liveReport.keySet()).toMap
    }

  }

}

class DService {
  // APP名称
  @BeanProperty var app: String = _
  // 模块名称
  @BeanProperty var module: String = _
  // 实例名称
  @BeanProperty var instance: String = _
  // 项目主机IP
  @BeanProperty var projectIp: String = _
  // 项目主机名
  @BeanProperty var projectHost: String = _
  // 项目路径
  @BeanProperty var projectPath: String = _
}
