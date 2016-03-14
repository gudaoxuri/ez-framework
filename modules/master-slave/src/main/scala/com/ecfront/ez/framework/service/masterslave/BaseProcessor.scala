package com.ecfront.ez.framework.service.masterslave

import java.lang.reflect.ParameterizedType

import com.ecfront.common.{JsonHelper, Resp}
import com.typesafe.scalalogging.slf4j.LazyLogging

trait BaseProcessor[E] extends Serializable with LazyLogging {

  val modelClazz = this.getClass.getGenericInterfaces()(0).asInstanceOf[ParameterizedType].getActualTypeArguments()(0).asInstanceOf[Class[E]]

  val category: String

  def execute(
               taskInfo: Map[String, Any], taskVar: Map[String, Any],
               instanceParameters: Map[String, Any], again: Boolean): Resp[(Map[String, Any], Map[String, Any])] = {
    val task = JsonHelper.toObject(taskInfo, modelClazz)
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

  protected def tryPreProcess(task: E, taskVar: Map[String, Any], instanceParameters: Map[String, Any]): Resp[Void] = Resp.success(null)

  protected def tryPostProcess(task: E, taskVar: Map[String, Any], instanceParameters: Map[String, Any]): Resp[Void] = Resp.success(null)

  protected def checkProcess(task: E, taskVar: Map[String, Any], instanceParameters: Map[String, Any]): Resp[Boolean] = Resp.success(true)

  protected def process(task: E, taskVar: Map[String, Any], instanceParameters: Map[String, Any]): Resp[(Map[String, Any], Map[String, Any])]

}
