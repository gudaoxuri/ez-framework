package com.ecfront.ez.framework.core

import com.ecfront.common.Resp
import com.ecfront.ez.framework.core.cluster.{Cluster, ClusterManage}
import com.ecfront.ez.framework.core.config.{ConfigProcessor, EZConfig}
import com.ecfront.ez.framework.core.eventbus.EventBusProcessor
import com.ecfront.ez.framework.core.i18n.I18NProcessor
import com.ecfront.ez.framework.core.logger.Logging
import com.ecfront.ez.framework.core.monitor.TaskMonitor
import com.ecfront.ez.framework.core.rpc.RPCProcessor
import com.fasterxml.jackson.databind.JsonNode

import scala.collection.JavaConversions._
import scala.collection.mutable.ArrayBuffer
import scala.reflect.runtime._

/**
  * EZ服务管理类，用于管理服务启动停止及依赖
  */
object EZManager extends Logging {

  // EZ服务配置项容器
  private var ezServiceConfig: Map[String, Any] = _
  // EZ服务容器
  private var ezServices: List[EZServiceAdapter[_]] = _

  private val clusterManageContainer = ArrayBuffer[ClusterManage]()

  var isClose = false

  private def initConfig(specialConfig: String = null): Resp[EZConfig] = {
    ConfigProcessor.init(specialConfig)
  }

  private def initCluster(config: JsonNode): Resp[Void] = {
    val spiNames = config.fieldNames().filterNot(_ == "use").toList
    val Array(rpc, mq, dist, cache) =
      if (config.has("use")) {
        val use = config.get("use")
        Array(use.get("rpc").asText(), use.get("mq").asText(), use.get("dist").asText(), use.get("cache").asText())
      } else {
        val headSpi = spiNames.head
        Array(headSpi, headSpi, headSpi, headSpi)
      }
    spiNames.foreach {
      spiName =>
        val spi = runtimeMirror.reflectModule(runtimeMirror.staticModule(s"com.ecfront.ez.framework.cluster.$spiName.${spiName.capitalize}Cluster")).instance.asInstanceOf[Cluster]
        spi.manage.init(config.get(spiName))
        clusterManageContainer += spi.manage
        EventBusProcessor.init(if (rpc == spiName) spi.rpc else null, if (mq == spiName) spi.mq else null)
        if (dist == spiName) {
          EZ.dist = spi.dist
        }
        if (cache == spiName) {
          EZ.cache = spi.cache
        }
    }
    Resp.success(null)
  }

  private def initRPC(config: Map[String, Any]): Resp[Void] = {
    RPCProcessor.init(config)
  }

  /**
    * 根据配置项初始化服务
    *
    * @return 可用的服务列表
    */
  private def startInDiscoverServices(): Resp[List[EZServiceAdapter[_]]] = {
    try {
      val services = ezServiceConfig.map {
        service =>
          // 使用ServiceAdapter对象初始化
          val serviceAdapter = runtimeMirror.reflectModule(
            runtimeMirror.staticModule(s"com.ecfront.ez.framework.service.${service._1}.ServiceAdapter$$")
          ).instance.asInstanceOf[EZServiceAdapter[_]]
          // 使用对象名做为服务名称
          serviceAdapter.serviceName = service._1
          serviceAdapter
      }.map {
        service =>
          // 加载动态依赖
          service.innerSetDynamicDependents(ezServiceConfig(service.serviceName))
          service
      }.toSet
      val notFindDependents = services.flatMap(_.dependents) -- ezServiceConfig.keys.toSet
      if (notFindDependents.nonEmpty) {
        logger.error(s"Found unload service(s) : $notFindDependents")
        Resp.notFound(s"Found unload service(s) : $notFindDependents")
      } else {
        Resp.success(services.toList)
      }
    } catch {
      case e: Throwable =>
        logger.error("start services error.", e)
        Resp.serverError(e.getMessage)
    }
  }

  /**
    * 执行服务排序
    *
    * @param orderServices   排序后的服务列表
    * @param currentServices 当前过滤后的服务列表
    * @param services        未排序的服务列表
    */
  private def startInOrderServices(
                                    orderServices: ArrayBuffer[EZServiceAdapter[_]],
                                    currentServices: List[EZServiceAdapter[_]],
                                    services: List[EZServiceAdapter[_]]): Unit = {
    currentServices.foreach {
      service =>
        val unLoadDependents = service.dependents -- orderServices.map(_.serviceName).toSet
        if (unLoadDependents.nonEmpty) {
          startInOrderServices(orderServices, services.filter(i => unLoadDependents.contains(i.serviceName)), services)
        }
        if (!orderServices.contains(service)) {
          orderServices += service
        }
    }
  }

  /**
    * 根据服务依赖对服务启动排序
    *
    * @param services 未排序的服务列表
    * @return 排序后的服务列表
    */
  private def startInOrderServices(services: List[EZServiceAdapter[_]]): Resp[List[EZServiceAdapter[_]]] = {
    val orderServices = ArrayBuffer[EZServiceAdapter[_]]()
    startInOrderServices(orderServices, services, services)
    Resp.success(orderServices.toList)
  }

  /**
    * 启动EZ服务
    *
    * @param configContent 使用自定义配置内容（json格式）
    * @return 启动是否成功
    */
  def start(configContent: String = null): Resp[String] = {
    logEnter("Starting...")
    logger.info("\r\n=== Parse Config ...")
    val ezConfigR = initConfig(configContent)
    if (ezConfigR) {
      val ezConfig = ezConfigR.body
      EZ.Info.config = ezConfig
      EZ.isDebug = ezConfig.ez.isDebug
      if (I18NProcessor.init()
        && initRPC(ezConfig.ez.rpc)) {
        EZ.Info.app = ezConfig.ez.app
        EZ.Info.module = ezConfig.ez.module
        EZ.Info.timezone = ezConfig.ez.timezone
        EZ.Info.instance = ezConfig.ez.instance
        EZ.Info.language = ezConfig.ez.language
        EZ.isDebug = ezConfig.ez.isDebug
        ezServiceConfig = ezConfig.ez.services
        logger.info("\r\n=== Discover Services ...")
        val ezServicesR = startInDiscoverServices()
        if (ezServicesR) {
          logger.info("\r\n=== Order Services ...")
          val orderServicesR = startInOrderServices(ezServicesR.body)
          if (orderServicesR) {
            ezServices = orderServicesR.body
            var isSuccess = true
            var message = ""
            ezServices.foreach {
              service =>
                if (isSuccess) {
                  logger.info(s"\r\n>>> Init ${service.serviceName}")
                  try {
                    val initR = service.innerInit(ezServiceConfig(service.serviceName))
                    if (!initR) {
                      message = s"Init [${service.serviceName}] Service error : [${initR.code}] [${initR.message}]"
                      isSuccess = false
                    }
                  } catch {
                    case e: Throwable =>
                      logger.error(s"Init [${service.serviceName}] Service error : ${e.getMessage}", e)
                      message = s"Init [${service.serviceName}] Service error : ${e.getMessage}"
                      isSuccess = false
                  }
                }
            }
            if (isSuccess) {
              ezServices.foreach(_.initPost())
              if (initCluster(ezConfig.ez.cluster) && RPCProcessor.autoBuilding(ezConfig.ez.rpc)) {
                logSuccess("Start Success")
              } else {
                logError(s"Start Fail : Core services start error")
              }
            } else {
              logError(s"Start Fail : $message")
            }
          } else {
            logError(s"Start Fail : ${orderServicesR.message}")
          }
        } else {
          logError(s"Start Fail : ${ezServicesR.message}")
        }
      } else {
        logError(s"Start Fail : Core services start error")
      }
    } else {
      logError(s"Start Fail : ${ezConfigR.message}")
    }
  }

  /**
    * 停止EZ服务
    */
  private def shutdown(): Resp[String] = {
    logEnter("Stopping...")
    var isSuccess = true
    var message = ""
    clusterManageContainer.foreach(_.close())
    if (ezServices != null) {
      ezServices.foreach {
        service =>
          if (isSuccess) {
            logger.info(s"\r\n>>> Destroy ${service.serviceName}")
            try {
              val destroyR = service.innerDestroy(ezServiceConfig(service.serviceName))
              if (!destroyR) {
                message = s"Destroy [${service.serviceName}] Service error : [${destroyR.code}] [${destroyR.message}]"
                isSuccess = false
              } else {
                logger.info(s"\r\n>>> ${destroyR.body}")
              }
            } catch {
              case e: Throwable =>
                message = s"Destroy [${service.serviceName}] Service error : ${e.getMessage}"
                isSuccess = false
            }
          }
      }
    }
    if (isSuccess) {
      logSuccess("Stopped , Bye")
    } else {
      logError(s"Stop Fail : $message")
    }
  }

  sys.addShutdownHook {
    isClose = true
    logger.info("!!! ==== Trigger shutdown event.")
    TaskMonitor.waitFinish()
    shutdown()
  }

  private def logEnter(message: String): Unit = {
    logger.info(
      s"""
         |==========================================
         |=== EZ Framework $message
         |-----------------------------------------""".stripMargin)
  }

  private def logError(message: String): Resp[String] = {
    logger.error(
      s"""
         |-----------------------------------------
         |=== EZ Framework $message
         |==========================================""".stripMargin)
    Resp("", message)
  }

  private def logSuccess(message: String): Resp[String] = {
    logger.info(
      s"""
         |-----------------------------------------
         |=== EZ Framework $message
         |==========================================""".stripMargin)
    Resp.success(message)
  }

  private val runtimeMirror = universe.runtimeMirror(getClass.getClassLoader)

}

