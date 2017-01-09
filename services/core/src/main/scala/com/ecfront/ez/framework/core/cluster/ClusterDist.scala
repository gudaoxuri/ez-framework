package com.ecfront.ez.framework.core.cluster

import java.util.concurrent.TimeUnit

import com.ecfront.ez.framework.core.logger.Logging

trait ClusterDist extends Logging {

  def lock(key: String): ILock

  def map[M:Manifest](key: String): IMap[M]

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

trait IMap[M] {

  def put(key: String, value: M): this.type

  def putAsync(key: String, value: M): this.type

  def putIfAbsent(key: String, value: M): this.type

  def contains(key: String): Boolean

  def foreach(fun: (String, M) => Unit): this.type

  def get(key: String): M

  def remove(key: String): this.type

  def removeAsync(key: String): this.type

  def clear(): this.type

}

