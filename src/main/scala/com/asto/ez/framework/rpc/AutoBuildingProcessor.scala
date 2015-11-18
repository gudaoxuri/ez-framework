package com.asto.ez.framework.rpc

import com.asto.ez.framework.EZContext
import com.ecfront.common._
import com.typesafe.scalalogging.slf4j.LazyLogging

import scala.reflect.runtime._

object AutoBuildingProcessor extends LazyLogging {

  private val runtimeMirror = universe.runtimeMirror(getClass.getClassLoader)

  /**
    * 使用基于注解的自动构建，此方法必须在服务启动“startup”后才能调用
    * @param basePackage  服务类所在的根包名
    */
  def autoBuilding(basePackage: String) = {
    ClassScanHelper.scan[RPC](basePackage).foreach {
      clazz =>
        if (clazz.getSimpleName.endsWith("$")) {
          process(runtimeMirror.reflectModule(runtimeMirror.staticModule(clazz.getName)).instance.asInstanceOf[AnyRef])
        } else {
          process(clazz.newInstance().asInstanceOf[AnyRef])
        }
    }
    this
  }

  def process(instance: AnyRef) = {
    //根路径
    var baseUri = BeanHelper.getClassAnnotation[RPC](instance.getClass).get.baseUri
    if (!baseUri.endsWith("/")) {
      baseUri += "/"
    }
    //默认通道
    val isUseHttp = BeanHelper.getClassAnnotation[HTTP](instance.getClass)
    val isUseEB = BeanHelper.getClassAnnotation[EVENT_BUS](instance.getClass)
    val isUseWebSocket = BeanHelper.getClassAnnotation[WEB_SOCKETS](instance.getClass)

    //存在指定通道的方法，当存在指定通道时默认无效
    val specialChannels = BeanHelper.findMethodAnnotations(instance.getClass, Seq(classOf[HTTP], classOf[EVENT_BUS], classOf[WEB_SOCKETS]))

    val specialMethods = specialChannels.map(_.method.name)
    val specialHttpMethods = specialChannels.filter(_.annotation.isInstanceOf[HTTP]).map(_.method.name)
    val specialEBMethods = specialChannels.filter(_.annotation.isInstanceOf[EVENT_BUS]).map(_.method.name)
    val specialWebSocketMethods = specialChannels.filter(_.annotation.isInstanceOf[WEB_SOCKETS]).map(_.method.name)

    BeanHelper.findMethodAnnotations(instance.getClass, Seq(classOf[GET], classOf[POST], classOf[PUT], classOf[DELETE])).foreach {
      methodInfo =>
        val methodName = methodInfo.method.name
        val methodMirror = BeanHelper.invoke(instance, methodInfo.method)
        val annInfo = methodInfo.annotation match {
          case ann: GET =>
            (Method.GET, if (ann.uri.startsWith("/")) ann.uri else baseUri + ann.uri, null)
          case ann: POST =>
            (Method.POST, if (ann.uri.startsWith("/")) ann.uri else baseUri + ann.uri, getClassFromMethodInfo(methodInfo))
          case ann: PUT =>
            (Method.PUT, if (ann.uri.startsWith("/")) ann.uri else baseUri + ann.uri, getClassFromMethodInfo(methodInfo))
          case ann: DELETE =>
            (Method.DELETE, if (ann.uri.startsWith("/")) ann.uri else baseUri + ann.uri, null)
        }
        if (specialEBMethods.contains(methodName) || isUseEB.isDefined && !specialMethods.contains(methodName)) {
          Router.add(EChannel.EVENT_BUS, annInfo._1, annInfo._2, annInfo._3, fun(annInfo._1, methodMirror))
        }
        if (specialHttpMethods.contains(methodName) || isUseHttp.isDefined && !specialMethods.contains(methodName)) {
          Router.add(EChannel.HTTP, annInfo._1, annInfo._2, annInfo._3, fun(annInfo._1, methodMirror))
        }
        if (specialWebSocketMethods.contains(methodName) || isUseWebSocket.isDefined && !specialMethods.contains(methodName)) {
          Router.add(EChannel.WEB_SOCKETS, annInfo._1, annInfo._2, annInfo._3, fun(annInfo._1, methodMirror))
        }
    }
  }

  def fun(method: String, methodMirror: universe.MethodMirror): (Map[String, String], Any, AsyncResp[Any], EZContext) => Unit = {
    (parameter, body, p, context) =>
      try {
        if (method == Method.GET || method == Method.DELETE) {
          methodMirror(parameter, p, context)
        } else {
          methodMirror(parameter, body, p, context)
        }
      } catch {
        case e: Exception =>
          logger.error("Occurred unchecked exception.", e)
          p.success(Resp.serverError(s"Occurred unchecked exception : ${e.getMessage}"))
      }

  }

  private def getClassFromMethodInfo(methodInfo: methodAnnotationInfo): Class[_] = {
    BeanHelper.getClassByStr(methodInfo.method.paramLists.head(1).info.toString)
  }

}
