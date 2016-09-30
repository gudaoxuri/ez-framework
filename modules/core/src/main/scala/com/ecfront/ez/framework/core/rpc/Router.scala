package com.ecfront.ez.framework.core.rpc

import com.ecfront.common.{BeanHelper, FieldAnnotationInfo, Resp}
import com.ecfront.ez.framework.core.EZ
import com.ecfront.ez.framework.core.i18n.I18NProcessor.Impl
import com.typesafe.scalalogging.slf4j.LazyLogging

object Router extends LazyLogging {

  private val FLAG_ADDRESS = "ez:rpc:address:"

  private val allAnnotations = collection.mutable.Map[String, List[FieldAnnotationInfo]]()
  private val fieldLabels = collection.mutable.Map[String, Map[String, String]]()
  private val requireFieldNames = collection.mutable.Map[String, List[String]]()

  /**
    * 注册路由规则
    *
    * @param path      请求路径
    * @param bodyClass 请求对象的类型
    * @param fun       业务方法
    */
  private[rpc] def add[E](method: String, path: String, bodyClass: Class[E], fun: => (Map[String, String], E) => Resp[Any]): Unit = {
    // 格式化path
    val formatPath = if (path.endsWith("/")) path else path + "/"
    val rpcFun = method match {
      case Method.POST =>
        ("http>post>" + formatPath, EZ.eb.reply[E](formatPath))
      case Method.GET =>
        ("http>get>" + formatPath, EZ.eb.reply[E](formatPath))
      case Method.DELETE =>
        ("http>delete>" + formatPath, EZ.eb.reply[E](formatPath))
      case Method.PUT =>
        ("http>put>" + formatPath, EZ.eb.reply[E](formatPath))
      case Method.WS =>
        ("ws>" + formatPath, EZ.eb.reply[E](formatPath))
      case Method.PUB_SUB =>
        (formatPath, EZ.eb.subscribe[E](formatPath))
      case Method.REQ_RESP =>
        (formatPath, EZ.eb.response[E](formatPath))
      case Method.ACK => (
        formatPath, EZ.eb.reply[E](formatPath))
    }
    addClazzInfo(bodyClass)
    rpcFun._2 {
      (message, args) =>
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
    EZ.cache.lpush(FLAG_ADDRESS, rpcFun._1)
    logger.info(s"Register path : ${rpcFun._1}")
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