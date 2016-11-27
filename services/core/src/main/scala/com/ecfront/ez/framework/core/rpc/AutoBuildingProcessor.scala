package com.ecfront.ez.framework.core.rpc

import com.ecfront.common._
import com.ecfront.ez.framework.core.EZ
import com.ecfront.ez.framework.core.logger.Logging
import com.ecfront.ez.framework.core.rpc.Method.Method
import com.ecfront.ez.framework.core.rpc.apidoc.{APIDocItemVO, APIDocProcessor, APIDocSectionVO}

import scala.reflect.runtime._

object AutoBuildingProcessor extends Logging {

  private val runtimeMirror = universe.runtimeMirror(getClass.getClassLoader)

  /**
    * 使用基于注解的自动构建
    *
    * @param rootPackage 服务类所在的根包名
    * @return 当前实例
    */
  def autoBuilding(rootPackage: String): this.type = {
    logger.info(s"auto building rpc at $rootPackage")

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
    val rpc = BeanHelper.getClassAnnotation[RPC](clazz).get
    var baseUri = rpc.baseUri
    if (!baseUri.endsWith("/")) {
      baseUri += "/"
    }
    val apiDoc = APIDocSectionVO(clazz.getSimpleName, rpc.docName, rpc.docDesc)
    try {
      BeanHelper.findMethodAnnotations(clazz,
        Seq(classOf[GET], classOf[POST], classOf[PUT], classOf[DELETE], classOf[WS], classOf[SUB], classOf[RESP], classOf[REPLY])).foreach {
        methodInfo =>
          val methodMirror = BeanHelper.invoke(instance, methodInfo.method)
          val respType =
            methodMirror.symbol.returnType.toString match {
              case "com.ecfront.common.Resp[com.ecfront.ez.framework.core.rpc.DownloadFile]" => "DownloadFile"
              case "com.ecfront.common.Resp[com.ecfront.ez.framework.core.rpc.ReqFile]" => "ReqFile"
              case "com.ecfront.common.Resp[com.ecfront.ez.framework.core.rpc.Raw]" => "Raw"
              case "com.ecfront.common.Resp[com.ecfront.ez.framework.core.rpc.RespRedirect]" => "RespRedirect"
              case _ => ""
            }
          val annInfo = methodInfo.annotation match {
            case ann: GET =>
              (Method.GET, ann.uri, null, ann.docName, ann.docDesc, "", ann.docRespExt)
            case ann: POST =>
              (Method.POST, ann.uri, getBodyClassByMethodInfo(methodInfo), ann.docName, ann.docDesc, ann.docReqExt, ann.docRespExt)
            case ann: PUT =>
              (Method.PUT, ann.uri, getBodyClassByMethodInfo(methodInfo), ann.docName, ann.docDesc, ann.docReqExt, ann.docRespExt)
            case ann: DELETE =>
              (Method.DELETE, ann.uri, null, ann.docName, ann.docDesc, "", ann.docRespExt)
            case ann: WS =>
              (Method.WS, ann.uri, getBodyClassByMethodInfo(methodInfo), ann.docName, ann.docDesc, ann.docReqExt, ann.docRespExt)
            case ann: SUB =>
              (Method.PUB_SUB, ann.uri, getBodyClassByMethodInfo(methodInfo), ann.docName, ann.docDesc, ann.docReqExt, ann.docRespExt)
            case ann: RESP =>
              (Method.REQ_RESP, ann.uri, getBodyClassByMethodInfo(methodInfo), ann.docName, ann.docDesc, ann.docReqExt, ann.docRespExt)
            case ann: REPLY =>
              (Method.ACK, ann.uri, getBodyClassByMethodInfo(methodInfo), ann.docName, ann.docDesc, ann.docReqExt, ann.docRespExt)
          }
          val uri = if (annInfo._2.startsWith("/")) annInfo._2 else baseUri + annInfo._2
          apiDoc.items += APIDocItemVO(annInfo._4, annInfo._1.toString, uri, annInfo._3, methodInfo.method.returnType.toString, annInfo._5, annInfo._6, annInfo._7)
          RPCProcessor.add(annInfo._1, uri, annInfo._3, respType, fun(annInfo._1, methodMirror))
      }
      APIDocProcessor.build(apiDoc)
    } catch {
      case e: Throwable =>
        logger.error(s"${instance.getClass} Method reflect error")
        throw e
    }
  }

  private def fun(method: Method, methodMirror: universe.MethodMirror): (Map[String, String], Any) => Any = {
    (parameter, body) =>
      try {
        if (method == Method.GET || method == Method.DELETE) {
          methodMirror(parameter)
        } else {
          methodMirror(parameter, body)
        }
      } catch {
        case e: Exception =>
          val context = EZ.context
          logger.error(s"Occurred unchecked exception by ${context.id}:${context.sourceRPCPath} from ${context.sourceIP}", e)
          Resp.serverError(s"Occurred unchecked exception by ${context.id}:${context.sourceRPCPath} from ${context.sourceIP} : ${e.getMessage}")
      }
  }

  private def getBodyClassByMethodInfo(methodInfo: methodAnnotationInfo): Class[_] = {
    BeanHelper.getClassByStr(methodInfo.method.paramLists.head(1).info.toString)
  }

}
