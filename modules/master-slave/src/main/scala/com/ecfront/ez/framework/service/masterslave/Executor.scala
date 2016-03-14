package com.ecfront.ez.framework.service.masterslave

import com.typesafe.scalalogging.slf4j.LazyLogging

import scala.annotation.tailrec

case class Executor(dto: ExecReqDTO) extends LazyLogging {

  val category: String = dto.category
  var running: Boolean = false

  def run(useNewThread: Boolean): Unit = {
    logger.debug(s"Executor Running [${dto.instanceId}]")
    running = true
    if (useNewThread) {
      val self = this
      ExecutorPool.executeThreadPool.execute(new Runnable {
        override def run(): Unit = {
          Executor.execute(dto)
          ExecutorPool.finishExecute(self)
        }
      })
    } else {
      Executor.execute(dto)
      ExecutorPool.finishExecute(this)
    }
  }

}

object Executor extends Serializable with LazyLogging {

  private val DEFAULT_MAX_TRY_TIME: Int = 5

  private[masterslave] def maxTryTime: Int = DEFAULT_MAX_TRY_TIME

  private var processorContainer = Map[String, BaseProcessor[_]]()

  private[masterslave] def registerProcessors(processors: List[BaseProcessor[_]]): Unit = {
    processorContainer = processors.map {
      processor =>
        processor.category -> processor
    }.toMap
  }

  private[masterslave] def execute(dto: ExecReqDTO): Unit = {
    Assigner.Worker.startTask(ExecStartRespDTO(dto.instanceId))
    val processorOpt = processorContainer.get(dto.category)
    if (processorOpt.isDefined) {
      doExecute(dto, processorOpt.get, 0)
    } else {
      logger.error("Processor NOT exist " + dto.category)
      Assigner.Worker.finishTask(ExecFinishRespDTO(
        dto.instanceId,
        isSuccess = false,
        hasChange = false,
        s"Not found process by category:${dto.category}",
        Map(),
        Map()
      ))
    }
  }

  @tailrec
  private def doExecute(dto: ExecReqDTO, processor: BaseProcessor[_], tryTimes: Int): Unit = {
    try {
      if (tryTimes != 0) {
        logger.warn(s"Worker process error try [$tryTimes] times ")
      }
      val changeCheckResp = processor.hasChange(dto.taskInfo, dto.taskVar, dto.instanceParameters)
      if (changeCheckResp) {
        if (changeCheckResp.body) {
          logger.debug(s"Worker process execute [${dto.instanceId}]")
          val resp = processor.execute(dto.taskInfo, dto.taskVar, dto.instanceParameters, tryTimes != 0)
          if (resp) {
            Assigner.Worker.finishTask(ExecFinishRespDTO(
              dto.instanceId,
              isSuccess = true,
              hasChange = true,
              "",
              resp.body._1,
              resp.body._2
            ))
          } else {
            Assigner.Worker.finishTask(ExecFinishRespDTO(
              dto.instanceId,
              isSuccess = false,
              hasChange = false,
              resp.message,
              Map(),
              Map()
            ))
          }
        } else {
          Assigner.Worker.finishTask(ExecFinishRespDTO(
            dto.instanceId,
            isSuccess = true,
            hasChange = false,
            "",
            Map(),
            Map()
          ))
        }
      } else {
        Assigner.Worker.finishTask(ExecFinishRespDTO(
          dto.instanceId,
          isSuccess = false,
          hasChange = false,
          changeCheckResp.message,
          Map(),
          Map()
        ))
      }
    } catch {
      case e: Throwable =>
        if (tryTimes < maxTryTime) {
          doExecute(dto, processor, tryTimes + 1)
        } else {
          Assigner.Worker.finishTask(ExecFinishRespDTO(
            dto.instanceId,
            isSuccess = false,
            hasChange = false,
            e.getMessage,
            Map(),
            Map()
          ))
        }
    }
  }

}