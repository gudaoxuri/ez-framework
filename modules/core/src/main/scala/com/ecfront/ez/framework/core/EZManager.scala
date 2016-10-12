package com.ecfront.ez.framework.core

import com.ecfront.common.Resp
import com.ecfront.ez.framework.core.cache.RedisCacheProcessor
import com.ecfront.ez.framework.core.config.{ConfigProcessor, EZConfig}
import com.ecfront.ez.framework.core.eventbus.VertxEventBusProcessor
import com.ecfront.ez.framework.core.i18n.I18NProcessor
import com.ecfront.ez.framework.core.rpc.RPCProcessor
import com.typesafe.scalalogging.slf4j.LazyLogging
import io.vertx.core.{Vertx, VertxOptions}

import scala.collection.mutable.ArrayBuffer
import scala.reflect.runtime._

/**
  * EZ服务管理类，用于管理服务启动停止及依赖
  */
object EZManager extends LazyLogging {

  // EZ服务配置项容器
  private var ezServiceConfig: Map[String, Any] = _
  // EZ服务容器
  private var ezServices: List[EZServiceAdapter[_]] = _

  System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.SLF4JLogDelegateFactory")
  System.setProperty("vertx.disableFileCaching", "true")
  System.setProperty("vertx.disableFileCPResolving", "true")

  private[core] val FLAG_PERF_EVENT_LOOP_POOL_SIZE = "eventLoopPoolSize"
  private[core] val FLAG_PERF_WORKER_POOL_SIZE = "workerPoolSize"
  private[core] val FLAG_PERF_INTERNAL_BLOCKING_POOL_SIZE = "internalBlockingPoolSize"
  private[core] val FLAG_PERF_MAX_EVENT_LOOP_EXECUTE_TIME = "maxEventLoopExecuteTime"
  private[core] val FLAG_PERF_WORKER_EXECUTE_TIME = "maxWorkerExecuteTime"
  private[core] val FLAG_PERF_WARNING_EXCEPTION_TIME = "warningExceptionTime"

  /**
    * 初始Vertx
    *
    * @return vertx实例
    */
  private def initVertx(perf: Map[String, Any], isDebug: Boolean): Vertx = {
    val opt = new VertxOptions()
    if (perf.contains(FLAG_PERF_EVENT_LOOP_POOL_SIZE)) {
      opt.setEventLoopPoolSize(perf(FLAG_PERF_EVENT_LOOP_POOL_SIZE).asInstanceOf[Int])
    } else {
      opt.setEventLoopPoolSize(20)
    }
    if (perf.contains(FLAG_PERF_WORKER_POOL_SIZE)) {
      opt.setWorkerPoolSize(perf(FLAG_PERF_WORKER_POOL_SIZE).asInstanceOf[Int])
    } else {
      opt.setWorkerPoolSize(100)
    }
    if (perf.contains(FLAG_PERF_INTERNAL_BLOCKING_POOL_SIZE)) {
      opt.setInternalBlockingPoolSize(perf(FLAG_PERF_INTERNAL_BLOCKING_POOL_SIZE).asInstanceOf[Int])
    } else {
      opt.setInternalBlockingPoolSize(100)
    }
    if (perf.contains(FLAG_PERF_MAX_EVENT_LOOP_EXECUTE_TIME)) {
      opt.setMaxEventLoopExecuteTime(perf(FLAG_PERF_MAX_EVENT_LOOP_EXECUTE_TIME).asInstanceOf[Int] * 1000000L)
    }
    if (perf.contains(FLAG_PERF_WORKER_EXECUTE_TIME)) {
      opt.setMaxWorkerExecuteTime(perf(FLAG_PERF_WORKER_EXECUTE_TIME).asInstanceOf[Int] * 1000000L)
    }
    if (perf.contains(FLAG_PERF_WARNING_EXCEPTION_TIME)) {
      opt.setWarningExceptionTime(perf(FLAG_PERF_WARNING_EXCEPTION_TIME).asInstanceOf[Int] * 1000000L)
    }
    if (isDebug) opt.setWarningExceptionTime(600L * 1000 * 1000000)
    Vertx.vertx(opt)
  }

  private def initConfig(specialConfig: String = null): Resp[EZConfig] = {
    ConfigProcessor.init(specialConfig)
  }

  private def initEB(vertx: Vertx): Resp[Void] = {
    val eb = new VertxEventBusProcessor()
    EZ.eb = eb
    eb.init(vertx)
  }

  private def initCache(args: Map[String, Any]): Resp[Void] = {
    val address = args("address").asInstanceOf[String].split(";")
    val db = args.getOrElse("db", 0).asInstanceOf[Int]
    val auth = args.getOrElse("auth", "").asInstanceOf[String]
    val cache = new RedisCacheProcessor()
    EZ.cache = cache
    cache.init(address, db, auth)
  }

  private def initRPC(args: Map[String, Any], vertx: Vertx): Resp[Void] = {
    RPCProcessor.init(vertx, args("package").asInstanceOf[String])
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
      EZ.vertx = initVertx(ezConfig.ez.perf.toMap, ezConfig.ez.isDebug)
      if (initEB(EZ.vertx) && initCache(ezConfig.ez.cache) && I18NProcessor.init()) {
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
              if (initRPC(ezConfig.ez.rpc, EZ.vertx)) {
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

