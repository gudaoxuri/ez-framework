package com.ecfront.ez.framework.service.masterslave

import com.ecfront.common.{JsonHelper, Resp}
import com.ecfront.ez.framework.core.EZContext
import com.ecfront.ez.framework.service.kafka.KafkaProcessor
import com.ecfront.ez.framework.service.kafka.KafkaProcessor.Producer
import com.typesafe.scalalogging.slf4j.LazyLogging

/**
  * 分配类，用于指定Master还是Slave
  */
object Assigner extends LazyLogging {

  private var module: String = _
  private var clusterId: String = _

  /**
    * 初始化
    *
    * @param _module    模块名
    * @param _clusterId 集群名，同一集群可以互通消息
    */
  private[masterslave] def init(_module: String, _clusterId: String): Unit = {
    module = _module
    clusterId = _clusterId
    logger.info("Kafka initialized.")
  }

  /**
    * Master类
    */
  object Master {

    private[masterslave] var masterTaskProducer: Producer = _

    /**
      * 注册Master
      *
      * @param finishCallback 任务完成时回调方法
      * @param startCallback  任务开始时回调方法
      */
    def register(finishCallback: => TaskFinishDTO => Unit, startCallback: => TaskStartDTO => Unit): Unit = {
      masterTaskProducer = KafkaProcessor.Producer(clusterId + "_prepare", module)
      KafkaProcessor.Consumer(module, clusterId + "_start", autoCommit = true).receive({
        message =>
          startCallback(JsonHelper.toObject(message, classOf[TaskStartDTO]))
          Resp.success(null)
      })
      KafkaProcessor.Consumer(module, clusterId + "_finish", autoCommit = true).receive({
        message =>
          finishCallback(JsonHelper.toObject(message, classOf[TaskFinishDTO]))
          Resp.success(null)
      })
    }

    /**
      * 发送要执行的任务
      *
      * @param dto 执行任务信息
      */
    def send(dto: TaskPrepareDTO): Unit = {
      if (masterTaskProducer != null) {
        masterTaskProducer.send(JsonHelper.toJsonString(dto))
      } else {
        logger.warn("Kafka producer not found.")
      }
    }

  }

  /**
    * Worker，这里的Worker指的就是Slave
    */
  object Worker {

    private[masterslave] var startTaskProducer: Producer = _
    private[masterslave] var finishTaskProducer: Producer = _

    /**
      * 注册Worker
      *
      * @param processors worker的处理器列表
      * @param worker     worker名，默认为当前的模块名
      */
    def register(processors: List[TaskBaseProcessor[_]], worker: String = EZContext.module): Unit = {
      Executor.registerProcessors(processors)
      startTaskProducer = KafkaProcessor.Producer(clusterId + "_start", module)
      finishTaskProducer = KafkaProcessor.Producer(clusterId + "_finish", module)
      KafkaProcessor.Consumer(module, clusterId + "_prepare").receive({
        message =>
          val dto = JsonHelper.toObject(message, classOf[TaskPrepareDTO])
          if (worker == dto.worker) {
            logger.trace(s"Received a message : $message")
            ExecutorPool.addExecute(Executor(dto))
          }
          Resp.success(null)
      })
    }

    /**
      * 发送开始执行任务
      *
      * @param dto 开始任务信息
      */
    private[masterslave] def startTask(dto: TaskStartDTO): Unit = {
      if (startTaskProducer != null) {
        startTaskProducer.send(JsonHelper.toJsonString(dto))
      } else {
        logger.warn("Kafka producer not found.")
      }
    }

    /**
      * 发送完成任务
      *
      * @param dto 完成任务信息
      */
    private[masterslave] def finishTask(dto: TaskFinishDTO): Unit = {
      if (finishTaskProducer != null) {
        finishTaskProducer.send(JsonHelper.toJsonString(dto))
      } else {
        logger.warn("Kafka producer not found.")
      }
    }

  }

  private[masterslave] def close(): Unit = {
    KafkaProcessor.close()
  }

}




