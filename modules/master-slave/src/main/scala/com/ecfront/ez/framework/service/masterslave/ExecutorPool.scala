package com.ecfront.ez.framework.service.masterslave

import scala.collection.mutable.ArrayBuffer

object ExecutorPool {

  private val maxPool = collection.mutable.Map[String, Int]()
  private val useNewThread = collection.mutable.Map[String, Boolean]()
  private val currentPool = collection.mutable.Map[String, ArrayBuffer[Executor]]()

  val executeThreadPool = java.util.concurrent.Executors.newCachedThreadPool()

  def initPool(category: String, maxNumber: Int, newThread: Boolean): Unit = {
    maxPool += category -> maxNumber
    useNewThread += category -> newThread
    currentPool += category -> ArrayBuffer()
  }

  def addExecute(executor: Executor): Unit = {
    currentPool(executor.category) += executor
    tryStartExecute(executor.category)
  }

  def finishExecute(executor: Executor): Unit = {
    this.synchronized {
      currentPool(executor.category) -= executor
      tryStartExecute(executor.category)
    }
  }

  private def tryStartExecute(category: String): Unit = {
    val idle = maxPool(category) - currentPool(category).count(_.running)
    if (idle > 0) {
      currentPool(category).filter(!_.running).take(idle).foreach(_.run(useNewThread(category)))
    }
  }


}
