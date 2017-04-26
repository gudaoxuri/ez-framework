package com.ecfront.ez.framework.core.rpc

import com.ecfront.common.{BeanHelper, FieldAnnotationInfo, Resp}
import com.ecfront.ez.framework.core.EZ
import com.ecfront.ez.framework.core.i18n.I18NProcessor.Impl
import com.ecfront.ez.framework.core.logger.Logging
import com.ecfront.ez.framework.core.rpc.Method.Method
import com.ecfront.ez.framework.core.rpc.apidoc.APIDocProcessor

object RPCProcessor extends Logging {

  private val RPC_API_URL_FLAG = "/ez/gateway/address/add/"
  val RESP_TYPE_FLAG = "__respType__"
  // Token信息 key : ez:auth:token:info:<token Id> value : <token info>
  val TOKEN_INFO_FLAG = "ez:auth:token:info:"
  // 前端传入的token标识
  val VIEW_TOKEN_FLAG = "__ez_token__"

  private val address = collection.mutable.Set[String]()

  private val allAnnotations = collection.mutable.Map[String, List[FieldAnnotationInfo]]()
  private val fieldLabels = collection.mutable.Map[String, Map[String, String]]()
  private val requireFieldNames = collection.mutable.Map[String, List[String]]()

  private[core] def init(config: Map[String, Any]): Resp[Void] = {
    APIDocProcessor.init(config.getOrElse("docPath", null).asInstanceOf[String])
    logger.info("[RPC] Init successful")
    Resp.success(null)
  }

  private[core] def autoBuilding(config: Map[String, Any]): Resp[Void] = {
    val basePackage = config("package").asInstanceOf[String]
    AutoBuildingProcessor.autoBuilding(basePackage)
    address.foreach {
      addr =>
        logger.info(s"[RPC] Register address : $addr")
    }
    Resp.success(null)
  }

  private[core] def add[E: Manifest](method: Method, path: String,
                                     bodyClass: Class[E], respType: String, fun: => (Map[String, String], E) => Any): Unit = {
    // 格式化path
    val formatPath = EZ.eb.packageAddress(method.toString, path)
    address += formatPath
    addClazzInfo(bodyClass)
    method match {
      case m if m == Method.GET || m == Method.POST || m == Method.PUT || m == Method.DELETE =>
        EZ.eb.publish(RPC_API_URL_FLAG, APIDTO(method.toString, path))
        EZ.eb.reply[E](formatPath, bodyClass) {
          (message, args) =>
            (execute[E](bodyClass, fun, message, args), Map(RESP_TYPE_FLAG -> respType))
        }
      case Method.WS =>
        EZ.eb.publish(RPC_API_URL_FLAG, APIDTO(method.toString, path))
        EZ.eb.reply[E](formatPath, bodyClass) {
          (message, args) =>
            (execute[E](bodyClass, fun, message, args), Map(RESP_TYPE_FLAG -> respType))
        }
      case Method.PUB_SUB =>
        EZ.eb.subscribe[E](formatPath, bodyClass) {
          (message, args) =>
            execute[E](bodyClass, fun, message, args)
        }
      case Method.REQ_RESP =>
        EZ.eb.response[E](formatPath, bodyClass) {
          (message, args) =>
            execute[E](bodyClass, fun, message, args)
        }
      case Method.ACK =>
        EZ.eb.reply[E](formatPath, bodyClass) {
          (message, args) =>
            (execute[E](bodyClass, fun, message, args), Map(RESP_TYPE_FLAG -> respType))
        }
    }
  }

  private def execute[E: Manifest](bodyClass: Class[E], fun: => (Map[String, String], E) => Any, message: E, args: Map[String, String]): Any = {
    message match {
      case bean: AnyRef =>
        val errorFields = requireFieldNames(bodyClass.getName).filter(BeanHelper.getValue(bean, _).get == null).map {
          requireField =>
            if (fieldLabels(bodyClass.getName).contains(requireField)) {
              fieldLabels(bodyClass.getName)(requireField).x
            } else {
              requireField.x
            }
        }
        if (errorFields.nonEmpty) {
          Resp.badRequest(errorFields.mkString("[", ",", "]") + " not null")
        } else {
          fun(args, message)
        }
      case _ =>
        fun(args, message)
    }
  }

  private def addClazzInfo[E](bodyClass: Class[E]): Any = {
    if (bodyClass != null) {
      val className = bodyClass.getName
      if (!allAnnotations.contains(className)) {
        allAnnotations += className -> BeanHelper.findFieldAnnotations(bodyClass).toList
        fieldLabels += className -> allAnnotations(className).filter(_.annotation.isInstanceOf[Label]).map {
          field =>
            field.fieldName -> field.annotation.asInstanceOf[Label].label
        }.toMap
        requireFieldNames += className -> allAnnotations(className).filter(_.annotation.isInstanceOf[Require]).map {
          field =>
            field.fieldName
        }
      }
    }
  }

}
