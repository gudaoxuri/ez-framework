package com.ecfront.ez.framework.core

import java.lang.reflect.ParameterizedType

import com.ecfront.common.{JsonHelper, Resp}
import com.typesafe.scalalogging.slf4j.LazyLogging
import io.vertx.core.json.JsonObject

import scala.reflect.runtime._

/**
  * EZ服务适配器，用于将服务整合到EZ框架中
  * 此对象路径要求为：`com.ecfront.ez.framework.service.服务标识.ServiceAdapter`
  *
  * @tparam E 服务配置项类型
  */
trait EZServiceAdapter[E] extends Serializable with LazyLogging {

  // 服务名称，框架会使用`服务标识`做为默认服务名称
  var serviceName: String = ""

  // 服务依赖，用于定义此服务的依赖服务的`服务标识`
  lazy val dependents: collection.mutable.Set[String] = collection.mutable.Set()

  // 服务初始化方法
  def init(parameter: E): Resp[String]

  // 服务销毁方法
  def destroy(parameter: E): Resp[String]

  // 服务动态依赖处理方法，如果服务需要根据配置使用不同依赖请重写此方法
  def getDynamicDependents(parameter: E): Set[String] = {
    null
  }

  private[core] def innerInit(parameter: Any): Resp[String] = {
    if (modelClazz == classOf[JsonObject]) {
      init(new JsonObject(JsonHelper.toJsonString(parameter)).asInstanceOf[E])
    } else {
      init(JsonHelper.toObject(parameter, modelClazz))
    }
  }

  private[core] def innerDestroy(parameter: Any): Resp[String] = {
    if (modelClazz == classOf[JsonObject]) {
      destroy(new JsonObject(JsonHelper.toJsonString(parameter)).asInstanceOf[E])
    } else {
      destroy(JsonHelper.toObject(parameter, modelClazz))
    }
  }

  private[core] def innerSetDynamicDependents(parameter: Any): Unit = {
    val dynamicDependents = if (modelClazz == classOf[JsonObject]) {
      getDynamicDependents(new JsonObject(JsonHelper.toJsonString(parameter)).asInstanceOf[E])
    } else {
      getDynamicDependents(JsonHelper.toObject(parameter, modelClazz))
    }
    if (dynamicDependents != null) {
      dependents ++= dynamicDependents
    }
  }

  protected val runtimeMirror = universe.runtimeMirror(getClass.getClassLoader)
  private val modelClazz = this.getClass.getGenericInterfaces()(0).asInstanceOf[ParameterizedType].getActualTypeArguments()(0).asInstanceOf[Class[E]]

}
