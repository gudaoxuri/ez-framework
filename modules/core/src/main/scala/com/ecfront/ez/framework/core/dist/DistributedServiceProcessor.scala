package com.ecfront.ez.framework.core.dist

import java.util.concurrent.TimeUnit

import com.typesafe.scalalogging.slf4j.LazyLogging

trait DistributedServiceProcessor extends LazyLogging {

  def lock(key: String): ILock

}

trait ILock {

  def lockWithFun(leaseTime: Long = -1, unit: TimeUnit = TimeUnit.MILLISECONDS)(fun: => Any): Any

  def tryLockWithFun(waitTime: Long = -1, leaseTime: Long = -1, unit: TimeUnit = TimeUnit.MILLISECONDS)(fun: => Any): Any

  def lock(leaseTime: Long = -1, unit: TimeUnit = TimeUnit.MILLISECONDS): this.type

  def tryLock(waitTime: Long = -1, leaseTime: Long = -1, unit: TimeUnit = TimeUnit.MILLISECONDS): Boolean

  def unLock(): Boolean

  def isLock: Boolean

  def delete(): this.type

}
