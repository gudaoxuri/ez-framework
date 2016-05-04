package com.ecfront.ez.framework.core

import com.ecfront.common.{JsonHelper, Resp}
import com.ecfront.ez.framework.core.i18n.I18NProcessor
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
  private[ecfront] def initVertx(perf: Map[String, Any]): Vertx = {
    val opt = new VertxOptions()
    if (perf.contains(FLAG_PERF_EVENT_LOOP_POOL_SIZE)) {
      opt.setEventLoopPoolSize(perf(FLAG_PERF_EVENT_LOOP_POOL_SIZE).asInstanceOf[Int])
    }
    if (perf.contains(FLAG_PERF_WORKER_POOL_SIZE)) {
      opt.setWorkerPoolSize(perf(FLAG_PERF_WORKER_POOL_SIZE).asInstanceOf[Int])
    }
    if (perf.contains(FLAG_PERF_INTERNAL_BLOCKING_POOL_SIZE)) {
      opt.setInternalBlockingPoolSize(perf(FLAG_PERF_INTERNAL_BLOCKING_POOL_SIZE).asInstanceOf[Int])
    }
    if (perf.contains(FLAG_PERF_MAX_EVENT_LOOP_EXECUTE_TIME)) {
      opt.setMaxEventLoopExecuteTime(perf(FLAG_PERF_MAX_EVENT_LOOP_EXECUTE_TIME).asInstanceOf[Int])
    }
    if (perf.contains(FLAG_PERF_WORKER_EXECUTE_TIME)) {
      opt.setMaxWorkerExecuteTime(perf(FLAG_PERF_WORKER_EXECUTE_TIME).asInstanceOf[Int])
    }
    if (perf.contains(FLAG_PERF_WARNING_EXCEPTION_TIME)) {
      opt.setWarningExceptionTime(perf(FLAG_PERF_WARNING_EXCEPTION_TIME).asInstanceOf[Int])
    }
    Vertx.vertx(opt)
  }

  /**
    * 解析服务配置 , 默认情况下加载classpath根路径下的`ez.json`文件
    *
    * @param configContent 使用自定义配置内容（json格式）
    * @return 服务配置
    */
  private def startInParseConfig(configContent: String = null): Resp[EZConfig] = {
    try {
      val finalConfigContent =
        if (configContent == null) {
          Source.fromFile(EZContext.confPath + "ez.json", "UTF-8").mkString
        } else {
          configContent
        }
      val jsonConfig = new JsonObject(finalConfigContent)
      val ezConfig = JsonHelper.toObject(jsonConfig.encode(), classOf[EZConfig])
      if (ezConfig.ez.language == null) {
        ezConfig.ez.language = "en"
      }
      if (ezConfig.ez.perf == null) {
        ezConfig.ez.perf = collection.mutable.Map[String, Any]()
      }
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
        ezConfig.ez.perf += FLAG_PERF_MAX_EVENT_LOOP_EXECUTE_TIME -> System.getProperty(FLAG_PERF_MAX_EVENT_LOOP_EXECUTE_TIME).toInt
      }
      if (System.getProperty(FLAG_PERF_WORKER_EXECUTE_TIME) != null) {
        ezConfig.ez.perf += FLAG_PERF_WORKER_EXECUTE_TIME -> System.getProperty(FLAG_PERF_WORKER_EXECUTE_TIME).toInt
      }
      if (System.getProperty(FLAG_PERF_WARNING_EXCEPTION_TIME) != null) {
        ezConfig.ez.perf += FLAG_PERF_WARNING_EXCEPTION_TIME -> System.getProperty(FLAG_PERF_WARNING_EXCEPTION_TIME).toInt
      }
      Resp.success(ezConfig)
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
    val ezConfigR = startInParseConfig(configContent)
    if (ezConfigR) {
      val ezConfig = ezConfigR.body
      EZContext.vertx = initVertx(ezConfig.ez.perf.toMap)
      EZContext.app = ezConfig.ez.app
      EZContext.perf = ezConfig.ez.perf.toMap
      EZContext.module = ezConfig.ez.module
      EZContext.language = ezConfig.ez.language
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
            ezServices.foreach(_.initPost())
            I18NProcessor.init()
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

