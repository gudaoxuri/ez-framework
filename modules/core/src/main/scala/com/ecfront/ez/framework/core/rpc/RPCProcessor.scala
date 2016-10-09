package com.ecfront.ez.framework.core.rpc

import com.ecfront.common.{BeanHelper, FieldAnnotationInfo, Resp}
import com.ecfront.ez.framework.core.EZ
import com.ecfront.ez.framework.core.i18n.I18NProcessor.Impl
import com.ecfront.ez.framework.core.rpc.Channel.Channel
import com.ecfront.ez.framework.core.rpc.Method.Method
import com.typesafe.scalalogging.slf4j.LazyLogging
import io.vertx.core.Vertx

object RPCProcessor extends LazyLogging {

  private val FLAG_RPC_API_URL = "/ez/gateway/address/add/"
  val FLAG_RESP_TYPE = "__respType__"
  // Token信息 key : ez:auth:token:info:<token Id> value : <token info>
  val TOKEN_INFO_FLAG = "ez:auth:token:info:"

  private val address = collection.mutable.Set[String]()

  private val allAnnotations = collection.mutable.Map[String, List[FieldAnnotationInfo]]()
  private val fieldLabels = collection.mutable.Map[String, Map[String, String]]()
  private val requireFieldNames = collection.mutable.Map[String, List[String]]()

  private[core] def init(vertx: Vertx, basePackage: String): Resp[Void] = {
    AutoBuildingProcessor.autoBuilding(basePackage)
    address.foreach {
      addr =>
        logger.info(s"[RPC] Register address : $addr")
    }
    HttpClientProcessor.init(vertx)
    logger.info("[RPC] Init successful")
    Resp.success(null)
  }

  private[core] def add[E: Manifest](channel: Channel, method: Method, path: String,
                                     bodyClass: Class[E], respType:String, fun: => (Map[String, String], E) => Any): Unit = {
    // 格式化path
    val formatPath = packageAddress(channel.toString, method.toString, path)
    address += formatPath
    addClazzInfo(bodyClass)
    channel match {
      case Channel.HTTP =>
        EZ.eb.publish(FLAG_RPC_API_URL, APIDTO(channel.toString, method.toString, path))
        (formatPath, EZ.eb.reply[E](formatPath, bodyClass) {
          (message, args) =>
            (execute[E](bodyClass, fun, message, args),Map(FLAG_RESP_TYPE -> respType))
        })
      case Channel.WS =>
        EZ.eb.publish(FLAG_RPC_API_URL, APIDTO(channel.toString, method.toString, path))
        (formatPath, EZ.eb.reply[E](formatPath, bodyClass) {
          (message, args) =>
            (execute[E](bodyClass, fun, message, args),Map(FLAG_RESP_TYPE -> respType))
        })
      case Channel.EB =>
        method match {
          case Method.PUB_SUB =>
            (formatPath, EZ.eb.subscribe[E](formatPath, bodyClass) {
              (message, args) =>
                execute[E](bodyClass, fun, message, args)
            })
          case Method.REQ_RESP =>
            (formatPath, EZ.eb.response[E](formatPath, bodyClass) {
              (message, args) =>
                execute[E](bodyClass, fun, message, args)
            })
          case Method.REPLY =>
            (formatPath, EZ.eb.reply[E](formatPath, bodyClass) {
              (message, args) =>
                (execute[E](bodyClass, fun, message, args),Map(FLAG_RESP_TYPE -> respType))
            })
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

  def packageAddress(channel: String, method: String, path: String): String = {
    val formatPath = if (path.endsWith("/")) path else path + "/"
    if (channel != Channel.EB.toString) {
      channel.toString + "@" + method + "@" + formatPath
    } else {
      formatPath
    }
  }
}
