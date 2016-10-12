package com.ecfront.ez.framework.core.dist

import java.util.concurrent.TimeUnit

import com.ecfront.common.Resp
import com.hazelcast.core.HazelcastInstance
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager

class HazelcastDistributedServiceProcessor extends DistributedServiceProcessor {

  private var dist: HazelcastInstance = _

  private[core] def init(mgr: HazelcastClusterManager): Resp[Void] = {
    dist = mgr.getHazelcastInstance
    Resp.success(null)
  }

  override def lock(key: String): ILock = Lock(key)

  case class Lock(key: String) extends ILock {

    private val lock = dist.getLock(key)

    override def lockWithFun(leaseTime: Long = -1, unit: TimeUnit = TimeUnit.MILLISECONDS)(fun: => Any): Any = {
      try {
        lock(leaseTime, unit)
        fun
      } catch {
        case e: Throwable =>
          logger.error("execute fun error with lock", e)
          throw e
      } finally {
        unLock()
      }
    }

    override def tryLockWithFun(waitTime: Long = -1, leaseTime: Long = -1, unit: TimeUnit = TimeUnit.MILLISECONDS)(fun: => Any): Any = {
      if (tryLock(waitTime, leaseTime, unit)) {
        try {
          fun
        } catch {
          case e: Throwable =>
            logger.error("execute fun error with tryLock", e)
            throw e
        } finally {
          unLock()
        }
      }
    }

    override def lock(leaseTime: Long = -1, unit: TimeUnit = TimeUnit.MILLISECONDS): this.type = {
      try {
        if(leaseTime== -1) {
          lock.lock()
        }else{
          lock.lock(leaseTime, unit)
        }
      } catch {
        case e: Exception => logger.warn(s"lock [$key] error.", e)
      }
      this
    }

    override def tryLock(waitTime: Long = -1, leaseTime: Long = -1, unit: TimeUnit = TimeUnit.MILLISECONDS): Boolean = {
      this.synchronized {
        try {
          if (waitTime == -1) {
            lock.tryLock()
          } else {
            if(leaseTime == -1){
              lock.tryLock(waitTime, unit)
            }else {
              lock.tryLock(waitTime, unit, leaseTime, unit)
            }
          }
        } catch {
          case e: Exception =>
            logger.warn(s"lock [$key] error.", e)
            false
        }
      }
    }

    override def unLock(): Boolean = {
      try {
        lock.unlock()
        true
      } catch {
        case e: Exception =>
          logger.error("Unlock error.", e)
          true
      }
    }

    override def isLock: Boolean = {
      lock.isLocked
    }

    override def delete(): this.type = {
      lock.destroy()
      this
    }

  }


}
