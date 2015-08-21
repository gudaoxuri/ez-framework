package com.ecfront.ez.framework.rpc.autobuilding

import com.ecfront.common.{BeanHelper, Resp, methodAnnotationInfo}
import com.ecfront.ez.framework.rpc.EVENT_BUS
import com.ecfront.ez.framework.rpc.HTTP
import com.ecfront.ez.framework.rpc.WEB_SOCKETS
import com.ecfront.ez.framework.rpc.RPC.EChannel
import com.ecfront.ez.framework.rpc._
import com.typesafe.scalalogging.slf4j.LazyLogging

object AutoBuildingProcessor extends LazyLogging {

  def process(server: Server, instance: AnyRef) = {
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
        if (server.getChannel == EChannel.EVENT_BUS && (specialEBMethods.contains(methodName) || isUseEB.isDefined && !specialMethods.contains(methodName))
          || server.getChannel == EChannel.HTTP && (specialHttpMethods.contains(methodName) || isUseHttp.isDefined && !specialMethods.contains(methodName))
          || server.getChannel == EChannel.WEB_SOCKETS && (specialWebSocketMethods.contains(methodName) || isUseWebSocket.isDefined && !specialMethods.contains(methodName))) {
          val methodMirror = BeanHelper.invoke(instance, methodInfo.method)
          methodInfo.annotation match {
            case ann: GET =>
              server.reflect.get(if (ann.uri.startsWith("/")) ann.uri else baseUri + ann.uri, {
                (param, _, inject) =>
                  try {
                    methodMirror(param, Some(inject))
                  } catch {
                    case e: Exception =>
                      logger.error("Occurred unchecked exception.", e)
                      Resp.serverError(s"Occurred unchecked exception : ${e.getMessage}")
                  }
              })
            case ann: POST =>
              server.reflect.post(if (ann.uri.startsWith("/")) ann.uri else baseUri + ann.uri, getClassFromMethodInfo(methodInfo), {
                (param, body, inject) =>
                  try {
                    methodMirror(param, body, Some(inject))
                  } catch {
                    case e: Exception =>
                      logger.error("Occurred unchecked exception.", e)
                      Resp.serverError("Occurred unchecked exception.")
                  }
              })
            case ann: PUT =>
              server.reflect.put(if (ann.uri.startsWith("/")) ann.uri else baseUri + ann.uri, getClassFromMethodInfo(methodInfo), {
                (param, body, inject) =>
                  try {
                    methodMirror(param, body, Some(inject))
                  } catch {
                    case e: Exception =>
                      logger.error("Occurred unchecked exception.", e)
                      Resp.serverError("Occurred unchecked exception.")
                  }
              })
            case ann: DELETE =>
              server.reflect.delete(if (ann.uri.startsWith("/")) ann.uri else baseUri + ann.uri, {
                (param, _, inject) =>
                  try {
                    methodMirror(param, Some(inject))
                  } catch {
                    case e: Exception =>
                      logger.error("Occurred unchecked exception.", e)
                      Resp.serverError("Occurred unchecked exception.")
                  }
              })
          }
        }
    }
  }

  private def getClassFromMethodInfo(methodInfo: methodAnnotationInfo): Class[_] = {
    val clazzStr = methodInfo.method.paramLists.head(1).info.toString
    clazzStr match {
      case "Int" => classOf[Int]
      case "String" => classOf[String]
      case "Long" => classOf[Long]
      case "Float" => classOf[Float]
      case "Double" => classOf[Double]
      case "Boolean" => classOf[Boolean]
      case "Short" => classOf[Short]
      case "Byte" => classOf[Byte]
      case s if s.startsWith("Map") => Class.forName("scala.collection.immutable.Map")
      case s if s.startsWith("List") || s.startsWith("scala.List") => Class.forName("scala.collection.immutable.List")
      case s if s.startsWith("Set") => Class.forName("scala.collection.immutable.Set")
      case s if s.startsWith("Seq") || s.startsWith("scala.Seq") => Class.forName("scala.collection.immutable.Seq")
      case s if s.startsWith("Vector") || s.startsWith("scala.Vector") => Class.forName("scala.collection.immutable.Vector")
      case s if s.startsWith("Array") => Class.forName("scala.Array")
      //去泛型
      case s if s.endsWith("]") => Class.forName(s.substring(0, s.indexOf("[")))
      case s => Class.forName(s)
    }
  }

}
