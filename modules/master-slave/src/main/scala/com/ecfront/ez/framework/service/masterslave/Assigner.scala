package com.ecfront.ez.framework.service.masterslave

import com.ecfront.common.{JsonHelper, Resp}
import com.ecfront.ez.framework.core.EZContext
import com.ecfront.ez.framework.service.kafka.KafkaProcessor
import com.ecfront.ez.framework.service.kafka.KafkaProcessor.Producer
import com.typesafe.scalalogging.slf4j.LazyLogging

object Assigner extends LazyLogging {

  private var module: String = _
  private var clusterId: String = _

  private[masterslave] def init(_module: String, _clusterId: String): Unit = {
    module = _module
    clusterId = _clusterId
    logger.info("Kafka initialized.")
  }

  object Master {

    private[masterslave] var masterTaskProducer: Producer = _

    def register(finishCallback: => ExecFinishRespDTO => Unit, startCallback: => ExecStartRespDTO => Unit): Unit = {
      masterTaskProducer = KafkaProcessor.Producer(clusterId + "_prepare", module)
      KafkaProcessor.Consumer(module, clusterId + "_start", autoCommit = true).receive({
        message =>
          startCallback(JsonHelper.toObject(message, classOf[ExecStartRespDTO]))
          Resp.success(null)
      })
      KafkaProcessor.Consumer(module, clusterId + "_finish", autoCommit = true).receive({
        message =>
          finishCallback(JsonHelper.toObject(message, classOf[ExecFinishRespDTO]))
          Resp.success(null)
      })
    }

    def send(dto: ExecReqDTO): Unit = {
      if (masterTaskProducer != null) {
        masterTaskProducer.send(JsonHelper.toJsonString(dto))
      } else {
        logger.warn("Kafka producer not found.")
      }
    }

  }

  object Worker {

    private[masterslave] var startTaskProducer: Producer = _
    private[masterslave] var finishTaskProducer: Producer = _

    def register(processors: List[BaseProcessor[_]], worker: String = EZContext.module): Unit = {
      Executor.registerProcessors(processors)
      startTaskProducer = KafkaProcessor.Producer(clusterId + "_start", module)
      finishTaskProducer = KafkaProcessor.Producer(clusterId + "_finish", module)
      KafkaProcessor.Consumer(module, clusterId + "_prepare").receive({
        message =>
          val dto = JsonHelper.toObject(message, classOf[ExecReqDTO])
          if (worker == dto.worker) {
            logger.trace(s"Received a message : $message")
            ExecutorPool.addExecute(Executor(dto))
          }
          Resp.success(null)
      })
    }

    private[masterslave] def startTask(dto: ExecStartRespDTO): Unit = {
      if (startTaskProducer != null) {
        startTaskProducer.send(JsonHelper.toJsonString(dto))
      } else {
        logger.warn("Kafka producer not found.")
      }
    }

    private[masterslave] def finishTask(dto: ExecFinishRespDTO): Unit = {
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




