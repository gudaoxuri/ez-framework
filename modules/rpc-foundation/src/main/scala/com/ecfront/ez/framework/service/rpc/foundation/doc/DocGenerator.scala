package com.ecfront.ez.framework.service.rpc.foundation.doc

import com.ecfront.common.{BeanHelper, ClassScanHelper, JsonHelper}
import com.ecfront.ez.framework.core.EZManager
import com.ecfront.ez.framework.service.rpc.foundation.AutoBuildingProcessor._
import com.ecfront.ez.framework.service.rpc.foundation._
import com.fasterxml.jackson.databind.JsonNode
import com.typesafe.scalalogging.slf4j.LazyLogging
import io.vertx.core.json.JsonObject

import scala.annotation.StaticAnnotation
import scala.reflect.runtime._
import scala.reflect.runtime.universe._

object DocGenerator extends App with LazyLogging {
  /*
  private val runtimeMirror = universe.runtimeMirror(getClass.getClassLoader)

 /* logger.info("starting generate doc")
  private val services=EZManager.startInParseConfig().body.ez.services
  if(services.contains("rpc.http")){
    val servicePath=services("rpc.http").asInstanceOf[JsonNode].get("servicePath").asText()
    autoBuilding[Http](servicePath,classOf[Http])
  }
  if(services.contains("rpc.websocket")){
    val servicePath=services("rpc.websocket").asInstanceOf[JsonNode].get("servicePath").asText()
    autoBuilding[WebS](servicePath,classOf[Http])
  }
  logger.info("finish generate doc at ./docs/")*/


 /* private def autoBuilding[A: TypeTag](servicePath: String, annClazz: Class[_ <: StaticAnnotation]): this.type = {
    ClassScanHelper.scan[RPC](servicePath).foreach {
      clazz =>
        if (clazz.getSimpleName.endsWith("$")) {
          process(runtimeMirror.reflectModule(runtimeMirror.staticModule(clazz.getName)).instance.asInstanceOf[AnyRef])
        } else {
          process(clazz.newInstance().asInstanceOf[AnyRef])
        }
    }
  }*/

  private def process(instance: AnyRef): Unit = {
    // 根路径
    var baseUri = BeanHelper.getClassAnnotation[RPC](instance.getClass).get.baseUri
    if (!baseUri.endsWith("/")) {
      baseUri += "/"
    }
    // 默认通道
    val defaultChannel = BeanHelper.getClassAnnotation[A](instance.getClass)
    // 存在指定通道的方法，当存在指定通道时默认无效
    val specialChannels = BeanHelper.findMethodAnnotations(instance.getClass, Seq(annClazz))

    try {
      BeanHelper.findMethodAnnotations(instance.getClass, Seq(classOf[GET], classOf[POST], classOf[PUT], classOf[DELETE], classOf[REQUEST])).foreach {
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
            case ann: REQUEST =>
              (Method.REQUEST, if (ann.uri.startsWith("/")) ann.uri else baseUri + ann.uri, getClassFromMethodInfo(methodInfo))

          }
          if (specialChannels.contains(methodName) || defaultChannel.isDefined) {
            Router.add(annClazz.getSimpleName, annInfo._1, annInfo._2, annInfo._3, fun(annInfo._1, methodMirror))
          }
      }
    }catch {
      case e:Throwable =>
        logger.error(s"${instance.getClass} Method reflect error")
        throw e
    }
  }*/

}
