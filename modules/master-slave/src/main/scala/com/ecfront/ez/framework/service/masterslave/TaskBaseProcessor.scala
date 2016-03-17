package com.ecfront.ez.framework.service.masterslave

import java.lang.reflect.ParameterizedType

import com.ecfront.common.{JsonHelper, Resp}
import com.typesafe.scalalogging.slf4j.LazyLogging

/**
  * 任务处理基类
  *
  * @tparam E 任务类型
  */
trait TaskBaseProcessor[E] extends Serializable with LazyLogging {

  val modelClazz = this.getClass.getGenericInterfaces()(0).asInstanceOf[ParameterizedType].getActualTypeArguments()(0).asInstanceOf[Class[E]]

  /**
    * 处理类类别，用于接收对应的任务
    */
  val category: String

  def execute(
               taskInfo: Map[String, Any], taskVar: Map[String, Any],
               instanceParameters: Map[String, Any], again: Boolean): Resp[(Map[String, Any], Map[String, Any])] = {
    val task =
      if (modelClazz == classOf[Void]) {
        null.asInstanceOf[E]
      } else {
        JsonHelper.toObject(taskInfo, modelClazz)
      }
    if (again) {
      tryPreProcess(task, taskVar, instanceParameters)
    }
    val resp = process(task, taskVar, instanceParameters)
    if (again) {
      tryPostProcess(task, taskVar, instanceParameters)
    }
    resp
  }

  def hasChange(taskInfo: Map[String, Any], taskVar: Map[String, Any], instanceParameters: Map[String, Any]): Resp[Boolean] = {
    val task = JsonHelper.toObject(taskInfo, modelClazz)
    checkProcess(task, taskVar, instanceParameters)
  }

  /**
    * 前置处理，当处理发生错误时重试执行[[process]]前执行此方法
    *
    * @param task               任务信息
    * @param taskVar            任务变量
    * @param instanceParameters 实例参数
    * @return 是否成功
    */
  protected def tryPreProcess(task: E, taskVar: Map[String, Any], instanceParameters: Map[String, Any]): Resp[Void] = Resp.success(null)

  /**
    * 后置处理，当处理发生错误时重试执行[[process]]后执行此方法
    *
    * @param task               任务信息
    * @param taskVar            任务变量
    * @param instanceParameters 实例参数
    * @return 是否成功
    */
  protected def tryPostProcess(task: E, taskVar: Map[String, Any], instanceParameters: Map[String, Any]): Resp[Void] = Resp.success(null)

  /**
    * 是否需要执行[[process]]检查
    *
    * 在某些情况下（如数据没有变更）不需要执行[[process]]，则可通过此方法检查
    *
    * @param task               任务信息
    * @param taskVar            任务变量
    * @param instanceParameters 实例参数
    * @return 是否需要执行[[process]]
    */
  protected def checkProcess(task: E, taskVar: Map[String, Any], instanceParameters: Map[String, Any]): Resp[Boolean] = Resp.success(true)

  /**
    * 任务执行方法
    *
    * @param task               任务信息
    * @param taskVar            任务变量
    * @param instanceParameters 实例参数
    * @return 执行结果，格式为：Resp(任务变量,实例参数)
    */
  protected def process(task: E, taskVar: Map[String, Any], instanceParameters: Map[String, Any]): Resp[(Map[String, Any], Map[String, Any])]

}
