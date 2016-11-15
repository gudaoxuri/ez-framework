package com.ecfront.ez.framework.service.gateway.metrics

import java.lang.Long

import com.ecfront.common.Resp
import com.ecfront.ez.framework.core.logger.Logging
import io.vertx.core.json.JsonObject
import io.vertx.core.metrics.Measured
import io.vertx.core.{Handler, Vertx}
import io.vertx.ext.dropwizard.MetricsService

trait MetricsProcessor extends Logging {

  private var metricsService: MetricsService = _
  private var vertx: Vertx = _

  private[gateway] def register(_vertx: Vertx): Resp[Void] = {
    vertx = _vertx
    metricsService = MetricsService.create(vertx)
    Resp.success(null)
  }

  def getMetrics(service: Measured): JsonObject = {
    metricsService.getMetricsSnapshot(service)
  }

  def getMetrics(name: String): JsonObject = {
    metricsService.getMetricsSnapshot(name)
  }

  def statistics(intervalSec: Int, service: Measured): Unit = {
    vertx.setPeriodic(intervalSec * 1000, new Handler[Long] {
      override def handle(event: Long): Unit = {
        doStatistics(service)
      }
    })
  }

  protected def doStatistics(service: Measured): Unit

}
