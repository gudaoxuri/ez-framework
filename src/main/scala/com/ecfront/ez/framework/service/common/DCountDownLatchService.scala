package com.ecfront.ez.framework.service.common

import java.util.concurrent.TimeUnit

import com.ecfront.ez.framework.rpc.cluster.ClusterManager
import com.typesafe.scalalogging.slf4j.LazyLogging

case class DCountDownLatchService(key: String) extends LazyLogging {

  private val countDownLatch = ClusterManager.clusterManager.getHazelcastInstance.getCountDownLatch(key)

  def set(value: Int): Boolean = {
    countDownLatch.destroy()
    countDownLatch.trySetCount(value)
  }

  def await(time: Long = -1, unit: TimeUnit = null) {
    if (time == -1) {
      countDownLatch.await(Int.MaxValue, TimeUnit.SECONDS)
    } else {
      countDownLatch.await(time, unit)
    }
  }

  def get: Long = {
    countDownLatch.getCount
  }

  def countDown() {
    countDownLatch.countDown()
  }

  def delete() {
    countDownLatch.destroy()
  }

}
