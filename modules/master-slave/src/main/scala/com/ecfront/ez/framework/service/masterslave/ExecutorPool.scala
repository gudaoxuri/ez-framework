package com.ecfront.ez.framework.service.masterslave

import scala.collection.mutable.ArrayBuffer

/**
  * 执行任务池
  *
  * 用于控制执行并发数量
  *
  * 每个执行任务都有对应的类型，执行并发数控制到类型级别
  */
object ExecutorPool {

  private val maxPool = collection.mutable.Map[String, Int]()
  private val useNewThread = collection.mutable.Map[String, Boolean]()
  private val currentPool = collection.mutable.Map[String, ArrayBuffer[Executor]]()

  val executeThreadPool = java.util.concurrent.Executors.newCachedThreadPool()

  /**
    * 初始化执行池
    *
    * @param category  执行器类别
    * @param maxNumber 最大并发数量
    * @param newThread 是否在新线程中执行
    */
  def initPool(category: String, maxNumber: Int, newThread: Boolean): Unit = {
    maxPool += category -> maxNumber
    useNewThread += category -> newThread
    currentPool += category -> ArrayBuffer()
  }

  /**
    * 添加执行任务
    *
    * @param executor 执行任务
    */
  def addExecute(executor: Executor): Unit = {
    currentPool(executor.category) += executor
    tryStartExecute(executor.category)
  }

  /**
    * 完成一次执行，并尝试启动池内其它执行任务
    *
    * @param executor 完成的执行任务
    */
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
