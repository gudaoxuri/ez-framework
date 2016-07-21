package com.ecfront.ez.framework.service.distributed.trace

import java.util.Date

import com.ecfront.ez.framework.service.redis.RedisProcessor
import com.typesafe.scalalogging.slf4j.LazyLogging

import scala.beans.BeanProperty
import scala.collection.mutable.ArrayBuffer

/**
  * 分布式追踪日志
  */
object DTrace extends LazyLogging {

  /**
    * 追踪日志定义
    * 日志流程定义code -> 日志节点code -> 日志节点定义
    */
  private val traceNodeDefs = collection.mutable.Map[String, collection.mutable.Map[String, TraceNodeDef]]()
  /**
    * 追踪日志状态持久化到Redis
    */
  private val rMap = RedisProcessor.redis.getMap[String, TraceFlowInst]("ez:trace")

  /**
    * 追踪日志流程定义
    *
    * @param flowCode 流程code,要求全局唯一
    * @param flow     流程定义，支持分支，如
    *                 List(
    *                 "m1#s1",
    *                 "m1#s2",
    *                 List(
    *                 List("m2#s3-1", "m2#s4-1"),
    *                 List("m3#s3-2")
    *                 ),
    *                 "m1#s5"
    *                 )
    */
  def define(flowCode: String, flow: List[Any]): Unit = {
    traceNodeDefs += flowCode -> collection.mutable.Map[String, TraceNodeDef]()
    addNodeDefs(flowCode, List(), flow)
  }

  private def addNodeDefs(flowCode: String, _parentNodes: List[TraceNodeDef], node: List[Any]): List[TraceNodeDef] = {
    var parentNodes = _parentNodes
    node.foreach {
      case nodeCode: String =>
        val module = nodeCode.split("#")(0)
        val stage = nodeCode.split("#")(1)
        val parentNodeCodes = parentNodes.map(_.code)
        val node = TraceNodeDef(nodeCode, module, stage, parentNodeCodes)
        traceNodeDefs(flowCode) += nodeCode -> node
        parentNodes.foreach(_.childrenNodeCodes += node.code)
        parentNodes = List(node)
      case node: List[List[Any]] =>
        parentNodes = node.flatMap(addNodeDefs(flowCode, parentNodes, _))
      case _ =>
        throw new Exception("Trace define error,flow only support [string][list] types")
    }
    parentNodes
  }

  /**
    * 写日志
    *
    * @param clueId   追踪线索，每个流程实例唯一，如贷款流程追踪可用身份证号做为clueId
    * @param flowCode 日志流程定义code
    * @param nodeCode 当前节点code，由模块#阶段 组成
    * @param message  消息
    */
  def simpleLog(flowCode: String)(clueId: String, nodeCode: String, message: String): Unit = {
    log(clueId, flowCode, nodeCode.split("#")(0), nodeCode.split("#")(1), message)
  }

  /**
    * 写日志
    *
    * @param clueId   追踪线索，每个流程实例唯一，如贷款流程追踪可用身份证号做为clueId
    * @param flowCode 日志流程定义code
    * @param module   当前模块
    * @param stage    当前阶段
    * @param message  消息
    */
  def flowLog(flowCode: String)(clueId: String, module: String, stage: String, message: String): Unit = {
    log(clueId, flowCode, module, stage, message)
  }

  /**
    * 写日志
    *
    * @param clueId   追踪线索，每个流程实例唯一，如贷款流程追踪可用身份证号做为clueId
    * @param flowCode 日志流程定义code
    * @param module   当前模块
    * @param stage    当前阶段
    * @param message  消息
    */
  def log(clueId: String, flowCode: String, module: String, stage: String, message: String): Unit = {
    try {
      // 根据传入参数获取实际的节点
      val realCurrNode = traceNodeDefs(flowCode)(module + "#" + stage)
      // 从Redis获取流程实例状态或新建一个流程实例状态
      val flowInst =
      if (realCurrNode.parentNodeCodes.isEmpty) {
        val inst = new TraceFlowInst
        inst.startTime = new Date()
        inst.parentNodeCode = ""
        inst.flow = ""
        inst.success = true
        inst
      } else {
        rMap.get(flowCode)
      }
      // 根据流程实例状态对照流程定义获取期望的节点codes
      val expectNodeCodes =
      if (realCurrNode.parentNodeCodes.isEmpty) {
        // 初始节点，期望节点肯定等于实际节点
        ArrayBuffer(realCurrNode.code)
      } else {
        traceNodeDefs(flowCode)(flowInst.parentNodeCode).childrenNodeCodes
      }
      // 更新流程实例状态
      flowInst.parentNodeCode = realCurrNode.code
      flowInst.flow = flowInst.flow + " > " + realCurrNode.code
      rMap.put(flowCode, flowInst)
      // 写日志
      val logStr = new StringBuffer()
      val instCode = (flowCode + "|" + clueId).hashCode
      if (realCurrNode.parentNodeCodes.isEmpty) {
        logStr.append(s"\r\n=|$instCode|======================= START [$flowCode] ========================")
      } else {
        logStr.append(s"\r\n=|$instCode|-------------------------------------------------------------------")
      }
      logStr.append(s"\r\n=|$instCode|= Trace [$flowCode] for [$clueId] at [$module]-[$stage] : $message")
      logStr.append(s"\r\n=|$instCode|= Flow [${flowInst.flow}]")
      if (!expectNodeCodes.contains(realCurrNode.code)) {
        logStr.append(s"\r\n=|$instCode|= Expect current in [${expectNodeCodes.mkString("/")}] But real current is ${realCurrNode.code}")
        flowInst.success = false
      }
      if (realCurrNode.childrenNodeCodes.isEmpty) {
        logStr.append(s"\r\n=|$instCode|= Result [${if (flowInst.success) "SUCCESS" else "FAIL"}] , Use Time [${new Date().getTime - flowInst.startTime.getTime}ms]")
        logStr.append(s"\r\n=|$instCode|======================= FINISH [$flowCode] ========================")
      }
      if (flowInst.success) {
        logger.info(logStr.toString)
      } else {
        logger.warn(logStr.toString)
      }
    } catch {
      case e: Throwable =>
        logger.error("Trace log write error.", e)
    }
  }
}

/**
  * 追踪日志流程实例状态，持久化到Redis中
  */
class TraceFlowInst {
  // 开始时间
  @BeanProperty var startTime: Date = _
  // 父节点code，上次执行的节点code
  @BeanProperty var parentNodeCode: String = _
  // 流程描述
  @BeanProperty var flow: String = _
  // 是否成功
  @BeanProperty var success: Boolean = _
}

/**
  * 追踪日志节点定义
  *
  * @param code              节点code
  * @param module            节点所属模块
  * @param stage             节点所属阶段
  * @param parentNodeCodes   父节点Codes
  * @param childrenNodeCodes 子节点Codes
  */
case class TraceNodeDef(code: String, module: String, stage: String,
                        parentNodeCodes: List[String] = List(), childrenNodeCodes: ArrayBuffer[String] = ArrayBuffer())
