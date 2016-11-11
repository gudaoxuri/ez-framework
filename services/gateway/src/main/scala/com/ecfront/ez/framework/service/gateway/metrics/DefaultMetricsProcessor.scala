package com.ecfront.ez.framework.service.gateway.metrics

import io.vertx.core.metrics.Measured

import scala.collection.JavaConversions._

object DefaultMetricsProcessor extends MetricsProcessor {

  override protected def doStatistics(service: Measured): Unit = {
    val items = getMetrics(service).map {
      item =>
        s"== ${item.getKey} : ${item.getValue}"
    }.mkString("\r\n")
    val log =
      s"""
         |==================Metrics  Statistics==================
         |$items
         |===============================================
       """.stripMargin
    logger.info(log)
  }

}
