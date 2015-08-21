package com.ecfront.ez.framework.service.common

import java.util.concurrent.TimeUnit

import com.ecfront.ez.framework.rpc.cluster.ClusterManager
import com.typesafe.scalalogging.slf4j.LazyLogging

case class DLockService(key: String) extends LazyLogging {

  private val lock = ClusterManager.clusterManager.getHazelcastInstance.getLock(key)

  def lock(leaseTime: Long = -1, unit: TimeUnit = TimeUnit.SECONDS) {
    if(leaseTime== -1){
      lock.lock()
    }else {
      lock.lock(leaseTime, unit)
    }
  }

  def tryLock(leaseTime: Long = -1, unit: TimeUnit = TimeUnit.MILLISECONDS): Boolean = {
    if (leaseTime == -1) {
      lock.tryLock()
    } else {
      lock.tryLock(leaseTime, unit)
    }
  }

  def unLock() {
    lock.forceUnlock()
  }

  def isLock: Boolean = {
    lock.isLocked
  }

  def delete() {
    lock.destroy()
  }

}
