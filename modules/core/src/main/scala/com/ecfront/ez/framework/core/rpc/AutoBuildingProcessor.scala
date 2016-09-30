package com.ecfront.ez.framework.core.rpc

import com.ecfront.common._
import com.ecfront.ez.framework.core.EZ
import com.typesafe.scalalogging.slf4j.LazyLogging

import scala.reflect.runtime._

object AutoBuildingProcessor extends LazyLogging {

  private val runtimeMirror = universe.runtimeMirror(getClass.getClassLoader)

  /**
    * 使用基于注解的自动构建
    *
    * @param rootPackage 服务类所在的根包名
    * @return 当前实例
    */
  def autoBuilding(rootPackage: String): this.type = {
    ClassScanHelper.scan[RPC](rootPackage).foreach {
      clazz =>
        if (clazz.getSimpleName.endsWith("$")) {
          process(runtimeMirror.reflectModule(runtimeMirror.staticModule(clazz.getName)).instance.asInstanceOf[AnyRef])
        } else {
          process(clazz.newInstance().asInstanceOf[AnyRef])
        }
    }
    this
  }

  private def process(instance: AnyRef): Unit = {
    val clazz = instance.getClass
    // 根路径
    var baseUri = BeanHelper.getClassAnnotation[RPC](clazz).get.baseUri
    if (!baseUri.endsWith("/")) {
      baseUri += "/"
    }
    try {
      BeanHelper.findMethodAnnotations(clazz,
        Seq(classOf[GET], classOf[POST], classOf[PUT], classOf[DELETE], classOf[WS], classOf[SUB], classOf[RESP], classOf[REPLY])).foreach {
        methodInfo =>
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
            case ann: WS =>
              (Method.WS, if (ann.uri.startsWith("/")) ann.uri else baseUri + ann.uri, getClassFromMethodInfo(methodInfo))
            case ann: SUB =>
              (Method.PUB_SUB, if (ann.uri.startsWith("/")) ann.uri else baseUri + ann.uri, getClassFromMethodInfo(methodInfo))
            case ann: RESP =>
              (Method.REQ_RESP, if (ann.uri.startsWith("/")) ann.uri else baseUri + ann.uri, getClassFromMethodInfo(methodInfo))
            case ann: REPLY =>
              (Method.ACK, if (ann.uri.startsWith("/")) ann.uri else baseUri + ann.uri, getClassFromMethodInfo(methodInfo))
          }
          Router.add(annInfo._1, annInfo._2, annInfo._3, fun(annInfo._1, methodMirror))
      }
    } catch {
      case e: Throwable =>
        logger.error(s"${instance.getClass} Method reflect error")
        throw e
    }
  }

  private def fun(method: String, methodMirror: universe.MethodMirror): (Map[String, String], Any) => Resp[Any] = {
    (parameter, body) =>
      try {
        if (method == Method.GET || method == Method.DELETE) {
          methodMirror(parameter).asInstanceOf[Resp[Any]]
        } else {
          methodMirror(parameter, body).asInstanceOf[Resp[Any]]
        }
      } catch {
        case e: Exception =>
          val context = EZ.context
          logger.error(s"Occurred unchecked exception by ${context.id}:${context.sourceRPCPath} from ${context.sourceIP}", e)
          Resp.serverError(s"Occurred unchecked exception by ${context.id}:${context.sourceRPCPath} from ${context.sourceIP} : ${e.getMessage}")
      }
  }

  private def getClassFromMethodInfo(methodInfo: methodAnnotationInfo): Class[_] = {
    BeanHelper.getClassByStr(methodInfo.method.paramLists.head(1).info.toString)
  }

}
