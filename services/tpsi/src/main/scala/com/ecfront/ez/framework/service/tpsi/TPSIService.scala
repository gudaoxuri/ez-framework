package com.ecfront.ez.framework.service.tpsi

import java.net.SocketTimeoutException
import java.text.SimpleDateFormat
import java.util.Date
import javax.xml.ws.WebServiceException

import com.ecfront.common.{JsonHelper, Resp, StandardCode}
import com.ecfront.ez.framework.core.EZ
import com.typesafe.scalalogging.slf4j.LazyLogging
import io.vertx.core.Handler

trait TPSIService extends LazyLogging {

  private val FLAG_LIMIT = "ez:tpsi:limit:"
  private val FLAG_TOKEN = "ez:tpsi:token:"

  protected var config: TPSIServiceConfig = _

  protected def register(_config: TPSIServiceConfig): Unit = {
    config = _config
    init(config)
    sys.addShutdownHook {
      shutdown(config)
    }
  }

  protected def init(config: TPSIServiceConfig): Unit

  protected def shutdown(config: TPSIServiceConfig): Unit = {}

  protected def login(config: TPSIServiceConfig): Resp[String] = {
    Resp.success("")
  }

  protected def execute[R](config: TPSIServiceConfig, id: String, funName: String, execFun: => Resp[R], reTryTimes: Int = 0): Resp[R] = {
    var traceId: String = ""
    try {
      logger.info(s"tpsi prepare [$funName]:[${config.code}][$id]")
      limitFilter(config)
      traceId = id + System.nanoTime()
      if (!currEXInfo.contains(funName + "@" + config.code)) {
        currEXInfo += funName + "@" + config.code -> collection.mutable.Map[String, (Date, String)]()
      }
      currEXInfo(funName + "@" + config.code) += traceId -> (new Date(), id)
      logger.info(s"tpsi start [$funName]:[$config.code][$id]")
      val resp = execFun
      if (currEXInfo.contains(funName + "@" + config.code) && currEXInfo(funName + "@" + config.code).contains(traceId)) {
        val trace = currEXInfo(funName + "@" + config.code)(traceId)
        logger.trace(s"tpsi execute [$funName]:[${config.code}][$id] times: ${new Date().getTime - trace._1.getTime} ms")
      }
      currEXInfo(funName + "@" + config.code) -= traceId
      logger.info(s"tpsi finish [$funName]:[${config.code}][$id] return data:" + JsonHelper.toJsonString(resp))
      resp.code match {
        case StandardCode.SUCCESS =>
          logger.info(s"tpsi success [$funName]:[${config.code}][$id]")
          resp
        case StandardCode.SERVICE_UNAVAILABLE =>
          if (reTryTimes <= 5) {
            logger.warn(s"tpsi service unavailable [$funName]:[${config.code}][$id],retry it")
            Thread.sleep(10000)
            execute[R](config, id, funName, execFun, reTryTimes + 1)
          } else {
            currEXInfo(funName + "@" + config.code) -= traceId
            logger.error(s"tpsi service unavailable [$funName]:[${config.code}][$id]")
            Resp.serverUnavailable(s"tpsi service unavailable and retry fail")
          }
        case StandardCode.UNAUTHORIZED =>
          if (config.needLogin) {
            logger.info(s"tpsi token expire [$funName]:[${config.code}][$id],re-login it")
            tryLogin(config)
            execute[R](config, id, funName, execFun)
          } else {
            logger.error(s"tpsi auth error [$funName]:[${config.code}][$id]")
            resp
          }
        case _ =>
          logger.warn(s"tpsi fail [$funName]:[${config.code}][$id]")
          resp
      }
    } catch {
      case e: Throwable if e.isInstanceOf[WebServiceException] || e.isInstanceOf[SocketTimeoutException] =>
        if (reTryTimes <= 5) {
          logger.warn(s"tpsi timeout [$funName]:[${config.code}][$id],retry it")
          Thread.sleep(10000)
          execute[R](config, id, funName, execFun, reTryTimes + 1)
        } else {
          currEXInfo(funName + "@" + config.code) -= traceId
          logger.error(s"tpsi timeout [$funName]:[${config.code}][$id]", e)
          Resp.serverUnavailable(s"tpsi timeout and retry fail")
        }
      case e: Throwable =>
        currEXInfo(funName + "@" + config.code) -= traceId
        logger.warn(s"tpsi fail [$funName]:[${config.code}][$id]", e)
        Resp.serverError(s"tpsi fail ${e.getMessage}")
    }
  }

  private def limitFilter(config: TPSIServiceConfig): Unit = {
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

  private def tryLogin(config: TPSIServiceConfig, forceReFetch: Boolean = false): String = {
    if (!EZ.cache.exists(FLAG_TOKEN + config.code) || forceReFetch) {
      val resp = execute[String](config, "", "login", login(config))
      if (resp) {
        EZ.cache.set(FLAG_TOKEN + config.code, resp.body, config.expireMinutes)
      }
    }
    EZ.cache.get(FLAG_TOKEN + config.code)
  }

  private val currEXInfo = collection.mutable.Map[String, collection.mutable.Map[String, (Date, String)]]()
  private val yyyy_MM_dd_HH_mm_ss_SSS = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS")
  EZ.vertx.setPeriodic(60000, new Handler[java.lang.Long] {
    override def handle(e: java.lang.Long): Unit = {
      val currExecCount = currEXInfo.map(_._2.size).sum
      if (currExecCount != 0) {
        val info = new StringBuffer(s"\r\n--------------Current exchange ($currExecCount) --------------\r\n")
        currEXInfo.foreach {
          map =>
            map._2.foreach {
              i =>
                info.append(s"------ ${yyyy_MM_dd_HH_mm_ss_SSS.format(i._2._1)} : ${map._1}-${i._2._2}\r\n")
            }
        }
        logger.info(info.toString)
      }
    }
  })

}
