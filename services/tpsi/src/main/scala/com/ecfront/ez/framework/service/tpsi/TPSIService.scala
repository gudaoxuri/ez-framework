package com.ecfront.ez.framework.service.tpsi

import java.net.SocketTimeoutException
import java.util.Date
import javax.xml.ws.WebServiceException

import com.ecfront.common.{JsonHelper, Resp, StandardCode}
import com.ecfront.ez.framework.core.EZ
import com.ecfront.ez.framework.core.logger.Logging
import com.ecfront.ez.framework.core.monitor.TaskMonitor
import com.fasterxml.jackson.databind.JsonNode

trait TPSIService extends Logging {

  private val FLAG_LIMIT = "ez:tpsi:limit:"
  private val FLAG_TOKEN = "ez:tpsi:token:"

  protected var config: TPSIConfig = _

  protected def register(): Unit = {
    config = ServiceAdapter.config
    init(config.args)
    sys.addShutdownHook {
      shutdown(config.args)
    }
  }

  register()

  protected def init(args: JsonNode): Unit

  protected def shutdown(args: JsonNode): Unit = {}

  protected def login(args: JsonNode): Resp[String] = Resp.success("")

  protected def execute[R](id: String, funName: String, execFun: => Any, execPostFun: Any => Resp[R], reTryTimes: Int = 0): Resp[R] = {
    var taskId: String = ""
    try {
      logger.debug(s"[TPSI] prepare [$funName]:[${config.code}][$id]")
      limitFilter(config)
      taskId = TaskMonitor.add(s"[TPSI] [$funName]:[${config.code}][$id]")
      logger.info(s"[TPSI] start [$funName]:[${config.code}][$id]")
      val log = if (config.isStorage) {
        EZ_TPSI_Log.start(funName, config.code, id)
      } else null
      val execResult = execFun
      val finishTime = new Date()
      val useTimes = finishTime.getTime - TaskMonitor.get(taskId)._2.getTime
      logger.debug(s"[TPSI] finish [$funName]:[${config.code}][$id] use ${useTimes}ms , return data:" + JsonHelper.toJsonString(execResult))
      val resp = execPostFun(execResult)
      if (log != null) {
        if (resp) {
          EZ_TPSI_Log.finish(success = true, "", log, finishTime)
        } else {
          EZ_TPSI_Log.finish(success = false, resp.message, log, finishTime)
        }
      }
      TaskMonitor.remove(taskId)
      resp.code match {
        case StandardCode.SUCCESS =>
          logger.info(s"[TPSI] success [$funName]:[${config.code}][$id]")
          resp
        case StandardCode.SERVICE_UNAVAILABLE =>
          if (reTryTimes <= 5) {
            logger.warn(s"[TPSI] service unavailable [$funName]:[${config.code}][$id],retry it [$reTryTimes]")
            Thread.sleep(2000 * reTryTimes)
            execute[R](id, funName, execFun, execPostFun, reTryTimes + 1)
          } else {
            logger.error(s"[TPSI] service unavailable [$funName]:[${config.code}][$id]")
            Resp.serverUnavailable(s"[TPSI] service unavailable and retry fail")
          }
        case StandardCode.UNAUTHORIZED =>
          if (config.needLogin) {
            logger.info(s"[TPSI] token expire [$funName]:[${config.code}][$id],re-login it")
            tryLogin(config)
            execute[R](id, funName, execFun, execPostFun)
          } else {
            logger.error(s"[TPSI] auth error [$funName]:[${config.code}][$id]")
            resp
          }
        case _ =>
          logger.warn(s"[TPSI] fail [$funName]:[${config.code}][$id]")
          resp
      }
    } catch {
      case e: Throwable if e.isInstanceOf[WebServiceException] || e.isInstanceOf[SocketTimeoutException] =>
        TaskMonitor.remove(taskId)
        if (reTryTimes <= 5) {
          logger.warn(s"[TPSI] timeout [$funName]:[${config.code}][$id],retry it [$reTryTimes]")
          Thread.sleep(2000 * reTryTimes)
          execute[R](id, funName, execFun, execPostFun, reTryTimes + 1)
        } else {
          logger.error(s"[TPSI] timeout [$funName]:[${config.code}][$id]", e)
          Resp.serverUnavailable(s"[TPSI] timeout and retry fail")
        }
      case e: Throwable =>
        TaskMonitor.remove(taskId)
        logger.warn(s"[TPSI] fail [$funName]:[${config.code}][$id]", e)
        Resp.serverError(s"[TPSI] fail ${e.getMessage}")
    }
  }

  private def limitFilter(config: TPSIConfig): Unit = {
    if (config.ratePerMinute != 0) {
      val currCount = EZ.cache.incr(FLAG_LIMIT + config.code, 0)
      if (currCount == 0) {
        EZ.cache.expire(FLAG_LIMIT + config.code, 60)
      }
      if (currCount >= config.ratePerMinute) {
        logger.info(s"[${config.code}] limit waiting")
        Thread.sleep(5000)
        limitFilter(config)
      } else {
        EZ.cache.incr(FLAG_LIMIT + config.code)
      }
    }
  }

  private def tryLogin(config: TPSIConfig, forceReFetch: Boolean = false): String = {
    if (!EZ.cache.exists(FLAG_TOKEN + config.code) || forceReFetch) {
      val resp = execute[String]("", "login", login(config.args), {
        _.asInstanceOf[Resp[String]]
      })
      if (resp) {
        if (config.expireMinutes != 0) {
          EZ.cache.set(FLAG_TOKEN + config.code, resp.body, config.expireMinutes)
        } else {
          EZ.cache.set(FLAG_TOKEN + config.code, resp.body)
        }
      }
    }
    EZ.cache.get(FLAG_TOKEN + config.code)
  }

}
