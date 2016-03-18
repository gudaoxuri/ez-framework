package com.ecfront.ez.framework.core

import com.ecfront.common.{JsonHelper, Resp}
import com.typesafe.scalalogging.slf4j.LazyLogging
import io.vertx.core._
import io.vertx.core.json.JsonObject

import scala.collection.mutable.ArrayBuffer
import scala.io.Source
import scala.reflect.runtime._

/**
  * EZ服务管理类，用于管理服务启动停止及依赖
  */
object EZManager extends LazyLogging {

  System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.SLF4JLogDelegateFactory")
  System.setProperty("vertx.disableFileCaching", "true")
  System.setProperty("vertx.disableFileCPResolving", "true")

  // EZ服务配置项容器
  private var ezServiceConfig: Map[String, Any] = null
  // EZ服务容器
  private var ezServices: List[EZServiceAdapter[_]] = null

  /**
    * 解析服务配置，要求在classpath根路径下存在`ez.json`文件
    *
    * @return 服务配置
    */
  private def startInParseConfig(): Resp[EZConfig] = {
    try {
      val jsonConfig = new JsonObject(Source.fromFile(EZContext.confPath + "ez.json", "UTF-8").mkString)
      Resp.success(JsonHelper.toObject(jsonConfig.encode(), classOf[EZConfig]))
    } catch {
      case e: Throwable =>
        Resp.serverError("Config parse error :" + e.getMessage)
    }
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
        Resp.notFound(s"Found unload service(s) : $notFindDependents")
      } else {
        Resp.success(services.toList)
      }
    } catch {
      case e: Throwable =>
        Resp.serverError(e.getMessage)
    }
  }

  /**
    * 执行服务排序
    *
    * @param orderServices 排序后的服务列表
    * @param services      未排序的服务列表
    */
  private def startInOrderServices(orderServices: ArrayBuffer[EZServiceAdapter[_]], services: List[EZServiceAdapter[_]]): Unit = {
    services.foreach {
      service =>
        val unLoadDependents = service.dependents -- orderServices.map(_.serviceName).toSet
        if (unLoadDependents.nonEmpty) {
          startInOrderServices(orderServices, services.filter(i => unLoadDependents.contains(i.serviceName)))
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
    startInOrderServices(orderServices, services)
    Resp.success(orderServices.toList)
  }

  /**
    * 启动EZ服务
    */
  def start(): Resp[String] = {
    EZContext.vertx = Vertx.vertx()
    logEnter("Starting...")
    logger.info("\r\n=== Parse Config ...")
    val ezConfigR = startInParseConfig()
    if (ezConfigR) {
      val ezConfig = ezConfigR.body
      EZContext.app = ezConfig.ez.app
      EZContext.module = ezConfig.ez.module
      EZContext.args = new JsonObject(JsonHelper.toJsonString(ezConfig.args))
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
                  } else {
                    logger.info(s"\r\n>>> ${initR.body}")
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
            logSuccess("Start Success")
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

