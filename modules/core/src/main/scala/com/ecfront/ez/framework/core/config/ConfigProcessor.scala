package com.ecfront.ez.framework.core.config

import com.ecfront.common.{JsonHelper, Resp}
import com.ecfront.ez.framework.core.{EZ, EZManager}
import com.typesafe.scalalogging.slf4j.LazyLogging

import scala.collection.JavaConversions._
import scala.io.Source

object ConfigProcessor extends LazyLogging {

  /**
    * 解析服务配置 , 默认情况下加载classpath根路径下的`ez.json`文件
    *
    * @param specialConfig 使用自定义配置内容（json格式）
    * @return 服务配置
    */
  private[core] def init(specialConfig: String = null): Resp[EZConfig] = {
    try {
      val configContent =
        if (specialConfig == null) {
          Source.fromFile(EZ.Info.confPath + "ez.json", "UTF-8").mkString
        } else {
          specialConfig
        }
      val ezConfig =
        if (configContent.startsWith("@")) {
          // 统一配置
          val Array(app, module, path) = configContent.substring(1).split("#")
          val unifyConfigPath = if (path.endsWith("/")) path else path + "/"
          val basicConfig = parseConfig(Source.fromFile(unifyConfigPath + "ez.json", "UTF-8").mkString)
          val moduleConfig = parseConfig(Source.fromFile(unifyConfigPath + s"ez.$app.$module.json", "UTF-8").mkString)
          if (moduleConfig.ez.app == null) {
            moduleConfig.ez.app = basicConfig.ez.app
          }
          if (moduleConfig.ez.module == null) {
            moduleConfig.ez.module = basicConfig.ez.module
          }
          moduleConfig.ez.instance = moduleConfig.ez.instance + System.nanoTime()
          if (moduleConfig.ez.cache == null) {
            moduleConfig.ez.cache = basicConfig.ez.cache
          }
          if (moduleConfig.ez.rpc == null) {
            moduleConfig.ez.rpc = basicConfig.ez.rpc
          }
          if (moduleConfig.ez.timezone == null) {
            moduleConfig.ez.timezone = basicConfig.ez.timezone
          }
          if (moduleConfig.ez.language == null) {
            moduleConfig.ez.language = basicConfig.ez.language
          }
          moduleConfig.ez.isDebug = basicConfig.ez.isDebug
          if (moduleConfig.ez.perf == null || moduleConfig.ez.perf.isEmpty) {
            moduleConfig.ez.perf = basicConfig.ez.perf
          }
          // 服务处理
          moduleConfig.ez.services =
            moduleConfig.ez.services.map {
              service =>
                if (JsonHelper.toJson(service._2).size() == 0) {
                  // 使用基础配置
                  service._1 -> basicConfig.ez.services(service._1)
                } else {
                  service
                }
            }
          // 全局参数
          if (basicConfig.args.size() != 0) {
            val args = JsonHelper.createObjectNode()
            basicConfig.args.fields().foreach {
              arg =>
                args.set(arg.getKey, arg.getValue)
            }
            moduleConfig.args.fields().foreach {
              arg =>
                args.set(arg.getKey, arg.getValue)
            }
            moduleConfig.args = JsonHelper.toJson(args)
          }
          moduleConfig
        } else {
          // 普通配置
          parseConfig(configContent)
        }
      Resp.success(ezConfig)
    } catch {
      case e: Throwable =>
        logger.error("Config parse error :" + e.getMessage, e)
        throw e
    }
  }


  private def parseConfig(configContent: String): EZConfig = {
    val ezConfig = JsonHelper.toObject(configContent, classOf[EZConfig])
    if (ezConfig.ez.instance == null) {
      ezConfig.ez.instance = (EZ.Info.projectIp + EZ.Info.projectPath).hashCode + ""
    }
    if (ezConfig.ez.language == null) {
      ezConfig.ez.language = "en"
    }
    if (ezConfig.ez.perf == null) {
      ezConfig.ez.perf = collection.mutable.Map[String, Any]()
    }
    if (System.getProperty(EZManager.FLAG_PERF_EVENT_LOOP_POOL_SIZE) != null) {
      ezConfig.ez.perf += EZManager.FLAG_PERF_EVENT_LOOP_POOL_SIZE -> System.getProperty(EZManager.FLAG_PERF_EVENT_LOOP_POOL_SIZE).toInt
    }
    if (System.getProperty(EZManager.FLAG_PERF_WORKER_POOL_SIZE) != null) {
      ezConfig.ez.perf += EZManager.FLAG_PERF_WORKER_POOL_SIZE -> System.getProperty(EZManager.FLAG_PERF_WORKER_POOL_SIZE).toInt
    }
    if (System.getProperty(EZManager.FLAG_PERF_INTERNAL_BLOCKING_POOL_SIZE) != null) {
      ezConfig.ez.perf += EZManager.FLAG_PERF_INTERNAL_BLOCKING_POOL_SIZE -> System.getProperty(EZManager.FLAG_PERF_INTERNAL_BLOCKING_POOL_SIZE).toInt
    }
    if (System.getProperty(EZManager.FLAG_PERF_MAX_EVENT_LOOP_EXECUTE_TIME) != null) {
      ezConfig.ez.perf += EZManager.FLAG_PERF_MAX_EVENT_LOOP_EXECUTE_TIME -> System.getProperty(EZManager.FLAG_PERF_MAX_EVENT_LOOP_EXECUTE_TIME).toLong
    }
    if (System.getProperty(EZManager.FLAG_PERF_WORKER_EXECUTE_TIME) != null) {
      ezConfig.ez.perf += EZManager.FLAG_PERF_WORKER_EXECUTE_TIME -> System.getProperty(EZManager.FLAG_PERF_WORKER_EXECUTE_TIME).toLong
    }
    if (System.getProperty(EZManager.FLAG_PERF_WARNING_EXCEPTION_TIME) != null) {
      ezConfig.ez.perf += EZManager.FLAG_PERF_WARNING_EXCEPTION_TIME -> System.getProperty(EZManager.FLAG_PERF_WARNING_EXCEPTION_TIME).toLong
    }
    ezConfig
  }
}
